package com.carwash.mobile.ingreso

import android.content.Context
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlateTextReader : AutoCloseable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun read(image: InputImage): Task<List<String>> = recognizer.process(image).continueWith { result ->
        plateCandidates(result.result.text)
    }

    override fun close() = recognizer.close()
}

internal class PlateCamera(
    context: Context,
    private val owner: LifecycleOwner,
    private val view: PreviewView,
    private val onReady: (Boolean) -> Unit,
    private val onRead: (List<String>) -> Unit,
    private val onError: () -> Unit,
) : AutoCloseable {
    private val executor = ContextCompat.getMainExecutor(context)
    private val future = ProcessCameraProvider.getInstance(context)
    private val preview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        ).build())
        .build()
    private val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        ).build())
        .build()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val reader = PlateTextReader()
    private val liveReader = PlateTextReader()
    @Volatile private var analyzing = false
    @Volatile private var found = false
    private var lastFrameAt = 0L
    private var lastPlate: String? = null
    private var hits = 0
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var active = true
    private var capturing = false
    private var recognizing = false
    private val cameraObserver = Observer<CameraState> { state ->
        if (active && state.error != null) onError()
    }

    fun start() {
        future.addListener({
            if (active) {
                try {
                    val source = future.get()
                    provider = source
                    val bound = source.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis)
                    camera = bound
                    analysis.setAnalyzer(analysisExecutor) { image -> analyze(image) }
                    onReady(bound.cameraInfo.hasFlashUnit())
                    bound.cameraInfo.cameraState.observe(owner, cameraObserver)
                } catch (_: Exception) {
                    onError()
                }
            }
        }, executor)
    }

    // Lectura automatica: una placa con formato valido debe verse en dos lecturas seguidas antes de aceptarla.
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val mediaImage = image.image
        if (!active || found || capturing || recognizing || mediaImage == null || now - lastFrameAt < FRAME_INTERVAL_MS) {
            image.close()
            return
        }
        lastFrameAt = now
        analyzing = true
        try {
            liveReader.read(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
                .addOnSuccessListener { result -> if (active && !found && !capturing) onLiveRead(result) }
                .addOnCompleteListener {
                    image.close()
                    analyzing = false
                    if (!active) liveReader.close()
                }
        } catch (_: Exception) {
            image.close()
            analyzing = false
        }
    }

    private fun onLiveRead(candidates: List<String>) {
        val plate = candidates.firstOrNull { VALID_PLATE.matches(it) }
        if (plate == null) {
            lastPlate = null
            hits = 0
            return
        }
        hits = if (plate == lastPlate) hits + 1 else 1
        lastPlate = plate
        if (hits >= STABLE_HITS) {
            found = true
            onRead(listOf(plate) + candidates.filter { it != plate })
        }
    }

    fun focus(horizontal: Float, vertical: Float) {
        if (!active) return
        val point = view.meteringPointFactory.createPoint(horizontal, vertical)
        camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build(),
        )
    }

    fun torch(enabled: Boolean) {
        if (active) camera?.cameraControl?.enableTorch(enabled)
    }

    fun zoom(scaleFactor: Float) {
        if (!active || !scaleFactor.isFinite() || scaleFactor <= 0f) return
        val bound = camera ?: return
        val state = bound.cameraInfo.zoomState.value ?: return
        bound.cameraControl.setZoomRatio(
            (state.zoomRatio * scaleFactor).coerceIn(state.minZoomRatio, state.maxZoomRatio),
        )
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun takePhoto() {
        if (!active || capturing || camera == null) return
        capturing = true
        capture.targetRotation = view.display?.rotation ?: android.view.Surface.ROTATION_0
        try {
            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    if (!active) {
                        image.close()
                        return
                    }
                    try {
                        val mediaImage = checkNotNull(image.image)
                        recognizing = true
                        reader.read(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
                            .addOnSuccessListener { result -> if (active) onRead(result) }
                            .addOnFailureListener { if (active) onError() }
                            .addOnCompleteListener {
                                image.close()
                                recognizing = false
                                capturing = false
                                if (!active) reader.close()
                            }
                    } catch (_: Exception) {
                        image.close()
                        recognizing = false
                        capturing = false
                        if (active) onError() else reader.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    if (active) onError()
                }
            })
        } catch (_: Exception) {
            capturing = false
            if (active) onError()
        }
    }

    override fun close() {
        active = false
        camera?.cameraInfo?.cameraState?.removeObserver(cameraObserver)
        analysis.clearAnalyzer()
        provider?.unbind(preview, capture, analysis)
        analysisExecutor.shutdown()
        if (!recognizing) reader.close()
        if (!analyzing) liveReader.close()
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 500L
        const val STABLE_HITS = 2
        val VALID_PLATE = Regex("[A-Z]{3}[0-9]{2}[0-9A-Z]")
    }
}

internal fun plateCameraTouchListener(
    context: Context,
    onZoom: (Float) -> Unit,
    onFocus: (Float, Float) -> Unit,
): View.OnTouchListener {
    var pinching = false
    val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            pinching = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onZoom(detector.scaleFactor)
            return true
        }
    }).apply { isQuickScaleEnabled = false }
    return View.OnTouchListener { target, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) pinching = false
        if (event.pointerCount > 1) pinching = true
        detector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP && !pinching) {
            onFocus(event.x, event.y)
            target.performClick()
        }
        true
    }
}
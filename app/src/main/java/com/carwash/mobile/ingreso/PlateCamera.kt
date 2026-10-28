package com.carwash.mobile.ingreso

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
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
    private val reader = PlateTextReader()
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
                    val bound = source.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    camera = bound
                    onReady(bound.cameraInfo.hasFlashUnit())
                    bound.cameraInfo.cameraState.observe(owner, cameraObserver)
                } catch (_: Exception) {
                    onError()
                }
            }
        }, executor)
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
        provider?.unbind(preview, capture)
        if (!recognizing) reader.close()
    }
}
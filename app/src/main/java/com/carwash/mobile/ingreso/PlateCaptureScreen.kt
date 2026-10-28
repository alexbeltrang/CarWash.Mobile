package com.carwash.mobile.ingreso

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.view.PreviewView
import com.carwash.mobile.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCaptureScreen(onConfirm: (String) -> Unit, onManual: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    var suggestions by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var plate by rememberSaveable { mutableStateOf("") }
    var failed by rememberSaveable { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<PlateCamera?>(null) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        if (hasCamera && !granted && !requested) {
            requested = true
            request.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (reviewing) R.string.review_plate else R.string.capture_plate)) },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("closeCamera")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.keep_editing))
                    }
                },
                actions = {
                    if (flash && ready && !reviewing && !failed && granted) {
                        IconToggleButton(checked = torch, enabled = !processing, onCheckedChange = {
                            torch = it
                            session?.torch(it)
                        }) {
                            Icon(if (torch) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                stringResource(if (torch) R.string.torch_off else R.string.torch_on))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (granted && hasCamera && !reviewing && !failed) {
                        Button(onClick = { processing = true; session?.takePhoto() }, enabled = ready && !processing,
                            modifier = Modifier.fillMaxWidth().testTag("takePhoto")) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(if (processing) R.string.reading_plate else R.string.take_photo))
                        }
                    }
                    TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth().testTag("manualPlate")) {
                        Icon(Icons.Default.Keyboard, null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.manual_plate))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                reviewing -> PlateReviewContent(suggestions, plate, { plate = it }, onConfirm, {
                    reviewing = false; failed = false; ready = false; torch = false
                })
                !hasCamera || !granted || failed -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(when {
                        !hasCamera -> R.string.camera_unavailable
                        !granted -> R.string.camera_permission_denied
                        else -> R.string.camera_failed
                    }), style = MaterialTheme.typography.bodyLarge)
                    if (hasCamera && !granted) {
                        Button(onClick = { requested = true; request.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.allow_camera))
                        }
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)))
                        }) { Text(stringResource(R.string.open_settings)) }
                    } else if (failed) {
                        OutlinedButton(onClick = { failed = false; ready = false; processing = false; torch = false }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.retry_capture))
                        }
                    }
                }
                else -> {
                    LivePlateCamera(
                        onSession = { session = it },
                        onReady = { ready = true; flash = it },
                        onRead = { result ->
                            suggestions = result; plate = result.firstOrNull().orEmpty()
                            processing = false; reviewing = true; torch = false
                        },
                        onError = { failed = true; ready = false; processing = false; torch = false },
                    )
                    if (!processing) Box(Modifier.align(Alignment.Center).fillMaxWidth(.85f).aspectRatio(3.2f)
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp)))
                    if (processing || !ready) {
                        Surface(Modifier.align(Alignment.Center), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                                Text(stringResource(if (processing) R.string.reading_plate else R.string.opening_camera))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePlateCamera(
    onSession: (PlateCamera?) -> Unit,
    onReady: (Boolean) -> Unit,
    onRead: (List<String>) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    val currentReady by rememberUpdatedState(onReady)
    val currentRead by rememberUpdatedState(onRead)
    val currentError by rememberUpdatedState(onError)
    val camera = remember(context, owner, view) {
        PlateCamera(context, owner, view, { currentReady(it) }, { currentRead(it) }, { currentError() })
    }
    DisposableEffect(camera) {
        onSession(camera)
        camera.start()
        onDispose { camera.close(); onSession(null) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize().testTag("cameraPreview"), update = { preview ->
            preview.setOnTouchListener { target, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    camera.focus(event.x, event.y)
                    target.performClick()
                }
                true
            }
        })
    }
}

@Composable
internal fun PlateReviewContent(
    candidates: List<String>,
    value: String,
    onChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(if (candidates.isEmpty()) R.string.plate_unreadable else R.string.plate_detected),
            style = MaterialTheme.typography.titleMedium)
        if (candidates.size > 1) candidates.forEach { candidate ->
            Row(Modifier.fillMaxWidth().selectable(selected = value == candidate, role = Role.RadioButton,
                onClick = { onChange(candidate) }).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(selected = value == candidate, onClick = null)
                Text(candidate, style = MaterialTheme.typography.bodyLarge)
            }
        }
        OutlinedTextField(
            value = value, onValueChange = { onChange(it.uppercase(Locale.ROOT)) },
            label = { Text(stringResource(R.string.plate_label)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().testTag("reviewPlate"),
        )
        Button(onClick = { reviewedPlate(value)?.let(onConfirm) }, enabled = reviewedPlate(value) != null,
            modifier = Modifier.fillMaxWidth().testTag("confirmPlate")) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.use_plate))
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().testTag("retryCapture")) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.retry_capture))
        }
    }
}
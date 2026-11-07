package com.apexfission.android.carddetector.demo

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme
import com.apexfission.android.carddetector.domain.ModelCatalog
import com.apexfission.android.carddetector.tfmodel.cardClasses
import com.apexfission.android.carddetector.tfmodel.classes
import com.apexfission.android.carddetector.tfmodel.modelPath
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Runs a confirmed synthetic card pair through the isolated backend bench. */
class BackendOcrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            CardDetectorDemoTheme {
                var code by remember { mutableStateOf("") }
                var capturing by remember { mutableStateOf(false) }
                var busy by remember { mutableStateOf(false) }
                var result by remember { mutableStateOf<BackendOcrResult?>(null) }
                var error by remember { mutableStateOf(false) }
                Scaffold { insets ->
                    Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                        if (capturing) {
                            HandlePermissions(
                                permissions = listOf(PermissionDescription(permission = Manifest.permission.CAMERA) {
                                    DefaultPermissionPage(
                                        label = stringResource(R.string.ocr_demo_camera),
                                        heroImage = Icons.Default.PhotoCamera,
                                        title = stringResource(R.string.ocr_demo_title),
                                        body = stringResource(R.string.ocr_demo_instructions),
                                    )
                                }),
                                onBack = { capturing = false },
                                onNotNow = { capturing = false },
                            ) {
                                TwoSidedCardCapture(
                                    modifier = Modifier.fillMaxSize(),
                                    instanceKey = "isolated-ocr-demo",
                                    modelPath = ModelCatalog.TfLite.modelPath,
                                    classLabels = ModelCatalog.TfLite.classes,
                                    cardClasses = ModelCatalog.TfLite.cardClasses,
                                    onBack = { capturing = false },
                                    onComplete = { photos ->
                                        val pairingCode = code.trim()
                                        capturing = false
                                        busy = true
                                        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            try {
                                                result = withContext(Dispatchers.IO) {
                                                    runBackendOcr(photos.front.bitmap, photos.back.bitmap, pairingCode)
                                                }
                                            } catch (cancel: CancellationException) {
                                                throw cancel
                                            } catch (_: Exception) {
                                                error = true
                                            } finally {
                                                photos.front.bitmap.recycle()
                                                photos.back.bitmap.recycle()
                                                busy = false
                                            }
                                        }
                                    },
                                )
                            }
                        } else {
                            BackendOcrScreen(code, { code = it }, busy, result, error) {
                                result = null
                                error = false
                                capturing = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendOcrScreen(
    code: String,
    onCode: (String) -> Unit,
    busy: Boolean,
    result: BackendOcrResult?,
    error: Boolean,
    onCapture: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.ocr_demo_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.ocr_demo_instructions))
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            enabled = !busy,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text(stringResource(R.string.ocr_demo_pairing)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onCapture,
            enabled = !busy && code.trim().length == 48,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.ocr_demo_open))
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.ocr_demo_running))
        }
        if (error) Text(stringResource(R.string.ocr_demo_error), color = MaterialTheme.colorScheme.error)
        result?.let {
            when {
                it.error != null -> Text(stringResource(R.string.ocr_demo_failed, it.error), color = MaterialTheme.colorScheme.error)
                else -> {
                    Text(stringResource(R.string.ocr_demo_timings, it.totalMs, it.inferenceMs))
                    Text(it.extraction ?: stringResource(R.string.ocr_demo_no_fields), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BackendOcrSetupPreview() {
    CardDetectorDemoTheme { BackendOcrScreen("", {}, false, null, false, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrRunningPreview() {
    CardDetectorDemoTheme { BackendOcrScreen("demo", {}, true, null, false, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrResultPreview() {
    CardDetectorDemoTheme { BackendOcrScreen("demo", {}, false, BackendOcrResult(1234, 1040, "Sample result", null), false, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrErrorPreview() {
    CardDetectorDemoTheme { BackendOcrScreen("demo", {}, false, null, true, {}) }
}

package com.apexfission.android.carddetector.demo

import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.ResponseTypeValues
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.TimeoutCancellationException
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
    private lateinit var authorization: AuthorizationService
    private var session by mutableStateOf<AuthState?>(null)
    private var signingIn by mutableStateOf(false)
    private var loginError by mutableStateOf<String?>(null)
    private val login = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        val response = intent?.let { AuthorizationResponse.fromIntent(it) }
        val exception = intent?.let { AuthorizationException.fromIntent(it) }
        if (response == null || exception != null) {
            signingIn = false
            loginError = "Sign-in was cancelled or failed. Please try again."
        } else {
            val state = AuthState(response, exception)
            authorization.performTokenRequest(response.createTokenExchangeRequest()) { token, error ->
                state.update(token, error)
                session = state.takeIf { it.isAuthorized }
                signingIn = false
                loginError = if (session == null) "Sign-in failed. Please try again." else null
            }
        }
    }

    private fun signIn() {
        session = null
        signingIn = true
        loginError = null
        lifecycleScope.launch {
            try {
                val config = withContext(Dispatchers.IO) { demoJson("/android-config.json") }
                val issuer = Uri.parse(config.getString("login"))
                require(issuer.scheme == "https" && issuer.host?.endsWith(".amazoncognito.com") == true)
                val service = AuthorizationServiceConfiguration(
                    Uri.parse("$issuer/oauth2/authorize"), Uri.parse("$issuer/oauth2/token"))
                val request = AuthorizationRequest.Builder(service, config.getString("clientId"),
                    ResponseTypeValues.CODE,
                    Uri.parse("com.apexfission.carddetector.demo:/oauth2redirect"))
                    .setScope("openid demo-ocr/run").setPrompt("login").build()
                login.launch(authorization.getAuthorizationRequestIntent(request))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                signingIn = false
                loginError = "Could not open sign-in. Check your connection and try again."
            }
        }
    }

    private suspend fun accessToken(): String = suspendCancellableCoroutine { continuation ->
        val current = session
        if (current == null) {
            continuation.resumeWith(Result.failure(IllegalStateException("Sign in to continue.")))
        } else {
            current.performActionWithFreshTokens(authorization) { access, _, error ->
                if (continuation.isActive) {
                    if (access != null && error == null) continuation.resumeWith(Result.success(access))
                    else continuation.resumeWith(Result.failure(IllegalStateException("Session expired. Sign in again.")))
                }
            }
        }
    }

    override fun onDestroy() {
        authorization.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        authorization = AuthorizationService(this)
        enableEdgeToEdge()
        setContent {
            CardDetectorDemoTheme {
                var capturing by remember { mutableStateOf(false) }
                var busy by remember { mutableStateOf(false) }
                var result by remember { mutableStateOf<BackendOcrResult?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
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
                                        capturing = false
                                        busy = true
                                        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            try {
                                                result = withContext(Dispatchers.IO) {
                                                    runBackendOcr(photos.front.bitmap, photos.back.bitmap, ::accessToken)
                                                }
                                            } catch (_: TimeoutCancellationException) {
                                                error = "OCR timed out. Please try again."
                                            } catch (cancel: CancellationException) {
                                                throw cancel
                                            } catch (failure: Exception) {
                                                error = failure.message ?: "Could not complete OCR."
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
                            BackendOcrScreen(session?.isAuthorized == true, {
                                result = null
                                error = null
                                signIn()
                            }, busy || signingIn, result, error ?: loginError) {
                                result = null
                                error = null
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
    signedIn: Boolean,
    onSignIn: () -> Unit,
    busy: Boolean,
    result: BackendOcrResult?,
    error: String?,
    onCapture: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.ocr_demo_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.ocr_demo_instructions))
        Text(stringResource(if (signedIn) R.string.ocr_demo_signed_in else R.string.ocr_demo_sign_in_help))
        TextButton(onClick = onSignIn, enabled = !busy) {
            Text(stringResource(R.string.ocr_demo_sign_in))
        }
        Button(
            onClick = onCapture,
            enabled = !busy && signedIn,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.ocr_demo_open))
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.ocr_demo_running))
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
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
    CardDetectorDemoTheme { BackendOcrScreen(false, {}, false, null, null, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrRunningPreview() {
    CardDetectorDemoTheme { BackendOcrScreen(true, {}, true, null, null, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrResultPreview() {
    CardDetectorDemoTheme { BackendOcrScreen(true, {}, false, BackendOcrResult(1234, 1040, "Sample result", null), null, {}) }
}

@PreviewLightDark
@Composable
private fun BackendOcrErrorPreview() {
    CardDetectorDemoTheme { BackendOcrScreen(true, {}, false, null, "Could not complete OCR.", {}) }
}

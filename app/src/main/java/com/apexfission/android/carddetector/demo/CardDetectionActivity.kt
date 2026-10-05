package com.apexfission.android.carddetector.demo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.carddetector.domain.ModelCatalog
import com.apexfission.android.carddetector.tfmodel.cardClasses
import com.apexfission.android.carddetector.tfmodel.classes
import com.apexfission.android.carddetector.tfmodel.modelPath
import com.apexfission.android.carddetector.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetector.ui.detector.CardDetectorLite
import com.apexfission.android.carddetector.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetector.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetector.demo.ui.theme.CardDetectorDemoTheme
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import com.apexfission.android.permission.ui.ReadingPace
import com.apexfission.android.permission.ui.estimateReadingDelayMillis

private const val CAMERA_TITLE = "Scan documents"
private const val CAMERA_BODY = "Allow camera access to capture a document when you start a scan."

class CardDetectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            CardDetectorDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    HandlePermissions(
                        permissions = listOf(
                            PermissionDescription(
                                permission = Manifest.permission.CAMERA,
                                autoAdvanceDelayMillis = estimateReadingDelayMillis(
                                    "$CAMERA_TITLE $CAMERA_BODY", ReadingPace.Slow
                                )
                            ){
                                DefaultPermissionPage(
                                    label = "Camera",
                                    heroImage = Icons.Default.PhotoCamera,
                                    title = CAMERA_TITLE,
                                    body = CAMERA_BODY,
                                )
                            }
                        ),
                        onBack = { finish() },
                        onNotNow = { finish() },
                    ) {
                        val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()
                        val navigateBack by mainViewModel.navigateBack.collectAsStateWithLifecycle()

                        LaunchedEffect(navigateBack) {
                            if (navigateBack) {
                                finish()
                                mainViewModel.onBackHandled()
                            }
                        }

                        CardDetectorLite(
                            modifier = Modifier.padding(innerPadding),
                            instanceKey = "card-detection-camera",
                            modelPath = ModelCatalog.TfLite.modelPath,
                            classLabels = ModelCatalog.TfLite.classes,
                            cardClasses = ModelCatalog.TfLite.cardClasses,
                            detectorPreset = CardDetectorPreset.HighPerformance.copy(scoreThreshold = 0.5f),
                            cameraPreset = CameraPreset.Default,
                            isDetectionEnabled = isDetectionEnabled,
                            onCardDetection = mainViewModel::onCardDetection,
                            onBack = mainViewModel::onBackRequested,
                            onCapture = mainViewModel::onCapture,
                            controlOverlay = {
                                IdCaptureOverlay()
                            }
                        )
                    }
                }
            }
        }
    }
}

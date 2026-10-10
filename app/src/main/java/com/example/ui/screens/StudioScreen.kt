package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FacialPose
import com.example.ui.components.AvatarCanvasView
import com.example.ui.components.CameraFramingSetupGuide
import com.example.ui.components.CharacterPickerSheet
import com.example.ui.components.CollapsibleSelfViewPip
import com.example.ui.components.ExportProgressDialog
import com.example.ui.components.LandmarkEditorDialog
import com.example.ui.components.SettingsModalSheet
import com.example.ui.theme.LiveBadgeGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraStats by viewModel.cameraTracker.stats.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCharacterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLandmarkEditor by remember { mutableStateOf(false) }

    val previewView = remember(context) { PreviewView(context) }

    // Dynamic Permission Checks
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startLiveMic()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.setCameraActive(true)
        }
    }

    // Camera Lifecycle Management: Runs safely in background
    DisposableEffect(lifecycleOwner, hasCameraPermission, uiState.isCameraActive) {
        if (hasCameraPermission && uiState.isCameraActive) {
            viewModel.cameraTracker.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                onError = { /* Graceful fallback */ }
            )
        }
        onDispose {
            viewModel.cameraTracker.stopCamera()
        }
    }

    // Auto-prompt camera permission on first launch if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.setCameraActive(true)
        }
    }

    // Toast error / notification
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // 1. TOP STUDIO APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NeonViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AvatarSync Studio",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Real-Time Camera & Gestural Studio",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Framing Guide Toggle
                    IconButton(
                        onClick = { viewModel.toggleFramingGuide() },
                        modifier = Modifier.testTag("btn_framing_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Framing Guide",
                            tint = if (uiState.showFramingGuide) NeonCyan else Color(0xFF94A3B8)
                        )
                    }

                    // Optional Collapsible Mini Self-View PIP
                    IconButton(
                        onClick = { viewModel.toggleSelfView() },
                        modifier = Modifier.testTag("btn_toggle_self_view")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSelfViewVisible) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Mini Camera View",
                            tint = if (uiState.isSelfViewVisible) NeonEmerald else Color(0xFFCBD5E1)
                        )
                    }

                    // Calibrate Avatar Rig / Landmarks
                    IconButton(
                        onClick = { showLandmarkEditor = true },
                        modifier = Modifier.testTag("btn_calibrate_rig")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Calibrate Facial & Body Rig",
                            tint = NeonCyan
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Studio Settings",
                            tint = Color(0xFFCBD5E1)
                        )
                    }

                    // Gallery Navigation
                    IconButton(
                        onClick = onNavigateToGallery,
                        modifier = Modifier.testTag("btn_open_gallery")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Export Gallery",
                            tint = NeonViolet
                        )
                    }
                }
            }

            // 2. CHARACTER & TRACKING TELEMETRY HEADER CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Character Chip
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                        .clickable { showCharacterSheet = true }
                        .testTag("chip_character_selector"),
                    color = StudioSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "AVATAR", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = uiState.customCharacterName ?: uiState.selectedPreset.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tracking Telemetry Status Chip
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
                    color = StudioSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isCameraActive && cameraStats.isFaceDetected) LiveBadgeGreen else RecordRed
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "MOTION CAPTURE", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (cameraStats.isFaceDetected) {
                                    "${cameraStats.detectedEmotion} • ${cameraStats.detectedGesture}"
                                } else if (uiState.isCameraActive) "Searching Face" else "Camera Standby",
                                color = if (cameraStats.isFaceDetected) NeonEmerald else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. SUBTLE TRACKING TELEMETRY PILL (Cleanly outside the canvas)
            TrackingTelemetryPill(
                poseFlow = viewModel.poseFlow,
                cameraStats = cameraStats,
                isCameraActive = uiState.isCameraActive,
                isLiveMicActive = uiState.isLiveMicActive,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 4. MAIN AVATAR CANVAS STAGE (UNOBSTRUCTED!)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // The main preview displays ONLY the clean animated avatar
                AvatarCanvasView(
                    poseFlow = viewModel.poseFlow,
                    preset = uiState.selectedPreset,
                    customBitmap = uiState.customCharacterBitmap,
                    landmarks = uiState.landmarks,
                    settings = uiState.animationSettings,
                    showLandmarksOverlay = uiState.showLandmarksOverlay,
                    modifier = Modifier.fillMaxSize()
                )

                // Optional collapsible mini self-view (only if user explicitly opens it)
                if (uiState.isSelfViewVisible && uiState.isCameraActive) {
                    CollapsibleSelfViewPip(
                        cameraTracker = viewModel.cameraTracker,
                        previewView = previewView,
                        onClose = { viewModel.toggleSelfView() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }

                // Camera Framing Setup Guide Overlay (temporary during calibration)
                if (uiState.showFramingGuide) {
                    CameraFramingSetupGuide(
                        cameraTracker = viewModel.cameraTracker,
                        onStartTracking = {
                            viewModel.toggleFramingGuide()
                            viewModel.setCameraActive(true)
                        },
                        onDismiss = { viewModel.toggleFramingGuide() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Permission Warning Overlay if camera permission missing
                if (!hasCameraPermission) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, NeonViolet, RoundedCornerShape(16.dp)),
                        color = Color(0xF2111322)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Camera Access Required", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AvatarSync uses the camera exclusively for motion capture to track your facial expressions, lips, head pose, and hand gestures in real time.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Enable Camera Tracking", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Wireframe Overlay Toggle Button
                IconButton(
                    onClick = { viewModel.toggleLandmarksOverlay() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(StudioSurface.copy(alpha = 0.85f))
                        .size(36.dp)
                        .testTag("btn_toggle_landmarks_overlay")
                ) {
                    Icon(
                        imageVector = if (uiState.showLandmarksOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Landmarks Wireframe",
                        tint = if (uiState.showLandmarksOverlay) NeonCyan else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Live Recording Active Banner
                if (uiState.isRecordingVideo) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(RecordRed.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC ${String.format("%02d:%02d", uiState.recordingSeconds / 60, uiState.recordingSeconds % 60)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. STUDIO CONTROLS & CAMERA / MIC STATUS
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp))
                    .padding(bottom = 8.dp),
                color = StudioSurfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isCameraActive) "Live Performance Tracking" else "Camera Tracking Standby",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isCameraActive) {
                                "Face, lips, eyes, and gestures drive the avatar live"
                            } else "Tap Start Tracking to begin performance capture",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Mic Audio capture toggle
                        IconButton(
                            onClick = {
                                if (hasMicPermission) {
                                    viewModel.toggleMicAudio()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (uiState.isLiveMicActive) NeonViolet else Color(0xFF262D42))
                                .size(38.dp)
                                .testTag("btn_toggle_mic")
                        ) {
                            Icon(
                                imageVector = if (uiState.isLiveMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle Microphone Audio Capture",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Camera Tracking Toggle Button
                        Button(
                            onClick = {
                                if (uiState.isCameraActive) {
                                    viewModel.stopCameraTrack()
                                } else {
                                    if (hasCameraPermission) {
                                        viewModel.setCameraActive(true)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isCameraActive) RecordRed else NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_toggle_camera_tracking")
                        ) {
                            Icon(
                                imageVector = if (uiState.isCameraActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isCameraActive) "Stop" else "Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6. PRIMARY PERFORMANCE ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleLiveRecording() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRecordingVideo) RecordRed else NeonViolet
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_record_live_performance")
                ) {
                    Icon(
                        imageVector = if (uiState.isRecordingVideo) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isRecordingVideo) "Stop & Save Video" else "Record Performance Video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.startExport(overrideDurationMs = 5000L) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_quick_render_clip")
                ) {
                    Text("Export Clip", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // SHEETS & DIALOGS
    if (showCharacterSheet) {
        CharacterPickerSheet(
            selectedPreset = uiState.selectedPreset,
            customCharacterName = uiState.customCharacterName,
            onSelectPreset = { viewModel.selectPresetCharacter(it) },
            onUploadCustom = { uri -> viewModel.setCustomCharacter(uri, context) },
            onDismiss = { showCharacterSheet = false }
        )
    }

    // Prompt Rig Calibration Dialog when custom character is prepared
    if (uiState.showRigPreviewDialog || showLandmarkEditor) {
        LandmarkEditorDialog(
            preset = uiState.selectedPreset,
            customBitmap = uiState.customCharacterBitmap,
            landmarks = uiState.landmarks,
            onUpdatePoint = { key, pt -> viewModel.updateLandmarkPoint(key, pt) },
            onReset = { viewModel.resetLandmarks() },
            onDismiss = {
                showLandmarkEditor = false
                viewModel.closeRigPreviewDialog()
            }
        )
    }

    if (showSettingsSheet) {
        SettingsModalSheet(
            settings = uiState.animationSettings,
            exportSettings = uiState.exportSettings,
            onSettingsChanged = { viewModel.updateAnimationSettings(it) },
            onExportSettingsChanged = { viewModel.updateExportSettings(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (uiState.showExportDialog) {
        ExportProgressDialog(
            isExporting = uiState.isExporting,
            progress = uiState.exportProgress,
            statusMessage = uiState.exportStatusMessage,
            successVideo = uiState.exportSuccessVideo,
            onCancel = { viewModel.dismissExportDialog() },
            onNavigateGallery = onNavigateToGallery,
            onDismiss = { viewModel.dismissExportDialog() }
        )
    }
}

@Composable
private fun TrackingTelemetryPill(
    poseFlow: StateFlow<FacialPose>,
    cameraStats: com.example.engine.camera.CameraTrackingStats,
    isCameraActive: Boolean,
    isLiveMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    val currentPose by poseFlow.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141824))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (cameraStats.isFaceDetected && isCameraActive) NeonEmerald else NeonPink)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isCameraActive) {
                    if (cameraStats.isFaceDetected) {
                        "Face Locked • ${cameraStats.detectedEmotion} • Gestures: ${cameraStats.detectedGesture}"
                    } else "Waiting for camera face lock..."
                } else if (isLiveMicActive) {
                    "Voice: ${(currentPose.audioAmplitude * 100).toInt()}% • Shape: ${currentPose.visemeType.displayName}"
                } else "Standby • Camera Inactive",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "${cameraStats.fps} FPS",
            color = NeonCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

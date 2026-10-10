package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ExportedVideoEntity
import com.example.data.model.AnimationSettings
import com.example.data.model.CharacterPreset
import com.example.data.model.ExportSettings
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPoint
import com.example.data.model.FacialPose
import com.example.data.model.SampleAudioClip
import com.example.data.model.StudioMode
import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType
import com.example.data.repository.CharacterRepository
import com.example.data.repository.ExportRepository
import com.example.engine.audio.AudioRecordManager
import com.example.engine.camera.CameraTracker
import com.example.engine.camera.FaceLandmarkDetector
import com.example.engine.exporter.VideoExportManager
import com.example.engine.rig.FacialRig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudioUiState(
    val currentMode: StudioMode = StudioMode.MODE_CAMERA_TRACK,
    val selectedPreset: CharacterPreset = CharacterRepository.PRESET_CHARACTERS[0],
    val customCharacterBitmap: Bitmap? = null,
    val customCharacterName: String? = null,
    val landmarks: FacialLandmarks = CharacterRepository.PRESET_CHARACTERS[0].defaultLandmarks,
    val isCameraActive: Boolean = false,
    val isLiveMicActive: Boolean = true,
    val isSelfViewVisible: Boolean = false, // Never obscuring character by default
    val showFramingGuide: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingSeconds: Int = 0,
    val currentPose: FacialPose = FacialPose(),
    val animationSettings: AnimationSettings = AnimationSettings(),
    val exportSettings: ExportSettings = ExportSettings(),
    val showLandmarksOverlay: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportStatusMessage: String? = null,
    val exportSuccessVideo: ExportedVideoEntity? = null,
    val showExportDialog: Boolean = false,
    val showLandmarkEditor: Boolean = false,
    val showRigPreviewDialog: Boolean = false,
    val showSettingsDrawer: Boolean = false,
    val errorMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val exportRepository = ExportRepository(db.exportedVideoDao())
    val audioRecordManager = AudioRecordManager()
    val cameraTracker = CameraTracker(application)
    private val exportManager = VideoExportManager(application)
    private val facialRig = FacialRig()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val _poseFlow = MutableStateFlow(FacialPose())
    val poseFlow: StateFlow<FacialPose> = _poseFlow.asStateFlow()

    private var recordTimerJob: Job? = null
    private var animationTickJob: Job? = null

    init {
        // Start render loop ticker for 30-40 fps smooth life & idle breathing animations
        startAnimationTickLoop()

        // Observe fused camera & mic performance tracking
        viewModelScope.launch {
            cameraTracker.cameraPose.collect { camPose ->
                if (_uiState.value.isCameraActive) {
                    val combinedPose = if (_uiState.value.isLiveMicActive) {
                        val micPose = audioRecordManager.livePose.value
                        camPose.copy(
                            jawOpen = kotlin.math.max(camPose.jawOpen, micPose.jawOpen * 1.15f),
                            visemeType = if (micPose.audioAmplitude > 0.12f) micPose.visemeType else camPose.visemeType,
                            audioAmplitude = kotlin.math.max(camPose.audioAmplitude, micPose.audioAmplitude)
                        )
                    } else camPose

                    val blended = facialRig.updatePose(combinedPose, _uiState.value.animationSettings)
                    _poseFlow.value = blended
                }
            }
        }

        // Observe mic audio
        viewModelScope.launch {
            audioRecordManager.livePose.collect { micPose ->
                if (!_uiState.value.isCameraActive && _uiState.value.isLiveMicActive) {
                    val blended = facialRig.updatePose(micPose, _uiState.value.animationSettings)
                    _poseFlow.value = blended
                }
            }
        }
    }

    private fun startAnimationTickLoop() {
        animationTickJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(33) // ~30 fps
                val state = _uiState.value
                val now = System.currentTimeMillis()

                if (!state.isCameraActive && !state.isLiveMicActive) {
                    // Life idle breathing and autonomic blinking
                    val idlePose = facialRig.updatePose(FacialPose(), state.animationSettings, now)
                    _poseFlow.value = idlePose
                }
            }
        }
    }

    fun selectPresetCharacter(preset: CharacterPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                customCharacterBitmap = null,
                customCharacterName = null,
                landmarks = preset.defaultLandmarks
            )
        }
    }

    fun setCustomCharacter(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val detectedLandmarks = FaceLandmarkDetector.autoDetectLandmarks(bitmap)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                customCharacterBitmap = bitmap,
                                customCharacterName = "Custom Avatar",
                                landmarks = detectedLandmarks,
                                showRigPreviewDialog = true, // Open rig preview before tracking!
                                errorMessage = "Face & facial rig detected! Review rig points or start tracking."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load character: ${e.localizedMessage}") }
            }
        }
    }

    fun closeRigPreviewDialog() {
        _uiState.update { it.copy(showRigPreviewDialog = false) }
    }

    // Camera Tracking Controls
    fun setCameraActive(active: Boolean) {
        _uiState.update { it.copy(isCameraActive = active) }
    }

    fun stopCameraTrack() {
        cameraTracker.stopCamera()
        _uiState.update { it.copy(isCameraActive = false) }
    }

    fun toggleSelfView() {
        _uiState.update { it.copy(isSelfViewVisible = !it.isSelfViewVisible) }
    }

    fun toggleFramingGuide() {
        _uiState.update { it.copy(showFramingGuide = !it.showFramingGuide) }
    }

    fun toggleMicAudio() {
        val newState = !_uiState.value.isLiveMicActive
        if (newState) {
            audioRecordManager.startListening()
        } else {
            audioRecordManager.stopListening()
        }
        _uiState.update { it.copy(isLiveMicActive = newState) }
    }

    fun startLiveMic() {
        audioRecordManager.startListening()
        _uiState.update { it.copy(isLiveMicActive = true) }
    }

    fun stopLiveMic() {
        audioRecordManager.stopListening()
        _uiState.update { it.copy(isLiveMicActive = false) }
    }

    // Live Recording Controls
    fun toggleLiveRecording() {
        if (_uiState.value.isRecordingVideo) {
            stopLiveRecording()
        } else {
            startLiveRecording()
        }
    }

    private fun startLiveRecording() {
        // Automatically ensure mic is active during recording
        audioRecordManager.startListening()
        _uiState.update { it.copy(isRecordingVideo = true, recordingSeconds = 0, isLiveMicActive = true) }

        recordTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRecordingVideo) {
                delay(1000)
                _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
            }
        }
    }

    private fun stopLiveRecording() {
        recordTimerJob?.cancel()
        val recordedSecs = _uiState.value.recordingSeconds.coerceAtLeast(3)
        _uiState.update { it.copy(isRecordingVideo = false) }

        // Automatically trigger export with synchronous audio & duration
        startExport(overrideDurationMs = (recordedSecs * 1000).toLong())
    }

    // Settings Updates
    fun updateAnimationSettings(settings: AnimationSettings) {
        _uiState.update { it.copy(animationSettings = settings) }
    }

    fun updateExportSettings(settings: ExportSettings) {
        _uiState.update { it.copy(exportSettings = settings) }
    }

    fun toggleLandmarksOverlay() {
        _uiState.update { it.copy(showLandmarksOverlay = !it.showLandmarksOverlay) }
    }

    fun updateLandmarkPoint(pointName: String, newPoint: FacialPoint) {
        val current = _uiState.value.landmarks
        val updated = when (pointName) {
            "mouthCenter" -> current.copy(mouthCenter = newPoint)
            "mouthLeftCorner" -> current.copy(mouthLeftCorner = newPoint)
            "mouthRightCorner" -> current.copy(mouthRightCorner = newPoint)
            "upperLipCenter" -> current.copy(upperLipCenter = newPoint)
            "lowerLipCenter" -> current.copy(lowerLipCenter = newPoint)
            "leftEyeCenter" -> current.copy(leftEyeCenter = newPoint)
            "rightEyeCenter" -> current.copy(rightEyeCenter = newPoint)
            "leftEyebrow", "leftEyebrowPeak" -> current.copy(leftEyebrowPeak = newPoint, leftEyebrow = newPoint)
            "rightEyebrow", "rightEyebrowPeak" -> current.copy(rightEyebrowPeak = newPoint, rightEyebrow = newPoint)
            "leftEyebrowInner" -> current.copy(leftEyebrowInner = newPoint)
            "rightEyebrowInner" -> current.copy(rightEyebrowInner = newPoint)
            "chin" -> current.copy(chin = newPoint)
            "leftJaw" -> current.copy(leftJaw = newPoint)
            "rightJaw" -> current.copy(rightJaw = newPoint)
            "leftShoulder" -> current.copy(leftShoulder = newPoint)
            "rightShoulder" -> current.copy(rightShoulder = newPoint)
            else -> current
        }
        _uiState.update { it.copy(landmarks = updated) }
    }

    fun resetLandmarks() {
        val defaultLms = _uiState.value.selectedPreset.defaultLandmarks
        _uiState.update { it.copy(landmarks = defaultLms) }
    }

    // Video Export
    fun startExport(overrideDurationMs: Long? = null) {
        val state = _uiState.value
        val title = "${state.customCharacterName ?: state.selectedPreset.name} Live Capture"
        val durationMs = overrideDurationMs ?: 6000L

        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = 0f,
                exportStatusMessage = "Encoding camera performance & speech frames...",
                showExportDialog = true,
                exportSuccessVideo = null
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val exportedVideo = exportManager.exportVideo(
                    title = title,
                    preset = state.selectedPreset,
                    customBitmap = state.customCharacterBitmap,
                    landmarks = state.landmarks,
                    visemes = emptyList(),
                    totalDurationMs = durationMs,
                    animationSettings = state.animationSettings,
                    exportSettings = state.exportSettings,
                    modeUsed = "CAMERA_TRACK",
                    onProgress = { p ->
                        _uiState.update { it.copy(exportProgress = p) }
                    }
                )

                // Save to Room DB
                exportRepository.insertVideo(exportedVideo)

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 1.0f,
                        exportStatusMessage = "Performance video exported successfully!",
                        exportSuccessVideo = exportedVideo
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.localizedMessage ?: "Codec initialization error"}",
                        showExportDialog = false
                    )
                }
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false, exportSuccessVideo = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

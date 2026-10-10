package com.example.data.model

enum class VisemeType(val displayName: String, val phoneticExample: String) {
    SILENCE("Silence", "Rest / P, B, M"),
    AH("Ah", "Car, Father, Open"),
    EE("Ee", "See, Read, Smile"),
    OH("Oh / Oo", "Go, Moon, Round"),
    FV("F / V", "Fine, Voice"),
    TH_SZ("Th / S / Z", "Think, Sun, Zoo")
}

data class VisemeCue(
    val timestampMs: Long,
    val durationMs: Long,
    val type: VisemeType,
    val intensity: Float = 1.0f
)

data class FacialPoint(
    val x: Float, // Normalized 0f..1f
    val y: Float  // Normalized 0f..1f
)

enum class HandGesture(val displayName: String) {
    IDLE("Resting"),
    OPEN_PALM("Open Palm"),
    WAVING("Waving"),
    POINTING("Pointing"),
    THUMBS_UP("Thumbs Up"),
    RAISED("Hand Raised")
}

data class FacialLandmarks(
    val leftEyeCenter: FacialPoint = FacialPoint(0.38f, 0.36f),
    val rightEyeCenter: FacialPoint = FacialPoint(0.62f, 0.36f),
    val leftEyebrow: FacialPoint = FacialPoint(0.38f, 0.29f),
    val rightEyebrow: FacialPoint = FacialPoint(0.62f, 0.29f),
    val leftEyebrowInner: FacialPoint = FacialPoint(0.44f, 0.31f),
    val leftEyebrowPeak: FacialPoint = leftEyebrow,
    val leftEyebrowOuter: FacialPoint = FacialPoint(0.31f, 0.32f),
    val rightEyebrowInner: FacialPoint = FacialPoint(0.56f, 0.31f),
    val rightEyebrowPeak: FacialPoint = rightEyebrow,
    val rightEyebrowOuter: FacialPoint = FacialPoint(0.69f, 0.32f),
    val noseTip: FacialPoint = FacialPoint(0.50f, 0.46f),
    val mouthCenter: FacialPoint = FacialPoint(0.50f, 0.54f),
    val mouthLeftCorner: FacialPoint = FacialPoint(0.42f, 0.54f),
    val mouthRightCorner: FacialPoint = FacialPoint(0.58f, 0.54f),
    val upperLipCenter: FacialPoint = FacialPoint(0.50f, 0.52f),
    val lowerLipCenter: FacialPoint = FacialPoint(0.50f, 0.56f),
    val chin: FacialPoint = FacialPoint(0.50f, 0.66f),
    val leftJaw: FacialPoint = FacialPoint(0.28f, 0.58f),
    val rightJaw: FacialPoint = FacialPoint(0.72f, 0.58f),
    val leftShoulder: FacialPoint = FacialPoint(0.20f, 0.85f),
    val rightShoulder: FacialPoint = FacialPoint(0.80f, 0.85f)
)

data class FacialPose(
    val jawOpen: Float = 0f,              // 0f (closed) to 1f (wide open)
    val mouthWidth: Float = 1f,           // 0.6f (narrow/pucker) to 1.4f (wide smile)
    val lipPucker: Float = 0f,            // 0f (flat) to 1f (puckered 'Oh/Oo')
    val mouthSmile: Float = 0f,           // -1f (frown) to +1f (smile)
    val mouthSmileLeft: Float = 0f,       // 0f..1f asymmetrical left corner lift
    val mouthSmileRight: Float = 0f,      // 0f..1f asymmetrical right corner lift
    val mouthSkew: Float = 0f,            // -1f (left shift) to +1f (right shift)
    val leftEyeBlink: Float = 0f,         // 0f (open) to 1f (closed)
    val rightEyeBlink: Float = 0f,        // 0f (open) to 1f (closed)
    val eyebrowRaise: Float = 0f,         // General brow raise (-1f to +1f)
    val leftEyebrowRaise: Float = 0f,     // Independent left brow (-1f to +1f)
    val rightEyebrowRaise: Float = 0f,    // Independent right brow (-1f to +1f)
    val browFurrow: Float = 0f,           // 0f..1f furrow / concentration
    val headPitch: Float = 0f,            // Up/Down degrees (-15f to +15f)
    val headYaw: Float = 0f,              // Left/Right degrees (-20f to +20f)
    val headRoll: Float = 0f,             // Tilt side degrees (-15f to +15f)
    val shoulderTilt: Float = 0f,         // Degrees tilt between shoulders
    val bodyLeanX: Float = 0f,            // -1f..+1f torso lateral sway
    val leftElbow: FacialPoint? = null,
    val rightElbow: FacialPoint? = null,
    val leftWrist: FacialPoint? = null,
    val rightWrist: FacialPoint? = null,
    val leftHandGesture: HandGesture = HandGesture.IDLE,
    val rightHandGesture: HandGesture = HandGesture.IDLE,
    val isFaceDetected: Boolean = false,
    val isBodyDetected: Boolean = false,
    val isLeftHandDetected: Boolean = false,
    val isRightHandDetected: Boolean = false,
    val detectedEmotion: String = "Neutral",
    val visemeType: VisemeType = VisemeType.SILENCE,
    val audioAmplitude: Float = 0f
)

enum class StudioMode {
    MODE_CAMERA_TRACK   // Unified Camera-Driven Character Studio
}

enum class BackgroundType(val displayName: String) {
    STUDIO_DARK("Studio Dark"),
    CYBER_GRID("Cyber Grid"),
    GRADIENT_PURPLE("Neon Violet"),
    CHROMA_GREEN("Chroma Green"),
    CLEAN_SLATE("Clean Slate")
}

enum class AspectRatioType(val displayName: String, val ratioWidth: Float, val ratioHeight: Float) {
    RATIO_9_16("9:16 Shorts", 9f, 16f),
    RATIO_16_9("16:9 YouTube", 16f, 9f),
    RATIO_1_1("1:1 Social", 1f, 1f)
}

data class AnimationSettings(
    val expressionIntensity: Float = 1.0f,
    val blinkFrequency: Float = 1.0f,
    val headMovementIntensity: Float = 0.85f,
    val faceSmoothing: Float = 0.35f,
    val idleAnimationEnabled: Boolean = true,
    val mouthTrackingEnabled: Boolean = true,
    val eyeTrackingEnabled: Boolean = true,
    val headTrackingEnabled: Boolean = true,
    val handTrackingEnabled: Boolean = true,
    val bodyTrackingEnabled: Boolean = true,
    val asymmetricBrowsEnabled: Boolean = true,
    val backgroundType: BackgroundType = BackgroundType.STUDIO_DARK,
    val aspectRatio: AspectRatioType = AspectRatioType.RATIO_9_16,
    val mirrorCamera: Boolean = true
)

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    HD_720P("720p HD", 720, 1280),
    FHD_1080P("1080p FHD", 1080, 1920)
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.HD_720P,
    val fps: Int = 30,
    val format: String = "MP4"
)

data class SampleAudioClip(
    val id: String,
    val title: String,
    val speaker: String,
    val durationSeconds: Float,
    val textTranscript: String,
    val tone: String
)

data class CharacterPreset(
    val id: String,
    val name: String,
    val role: String,
    val styleBadge: String,
    val baseColorHex: Long,
    val accentColorHex: Long,
    val eyeColorHex: Long,
    val hairStyle: String,
    val expressionDefault: String,
    val defaultLandmarks: FacialLandmarks = FacialLandmarks()
)

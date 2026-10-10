package com.example.engine.camera

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.model.FacialPoint
import com.example.data.model.FacialPose
import com.example.data.model.HandGesture
import com.example.data.model.VisemeType
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

data class CameraTrackingStats(
    val isFaceDetected: Boolean = false,
    val isBodyDetected: Boolean = false,
    val isLeftHandDetected: Boolean = false,
    val isRightHandDetected: Boolean = false,
    val mouthAperturePercent: Int = 0,
    val eyeOpennessPercent: Int = 100,
    val smilePercent: Int = 0,
    val browRaisePercent: Int = 0,
    val headTiltDegrees: Float = 0f,
    val headYawDegrees: Float = 0f,
    val headPitchDegrees: Float = 0f,
    val detectedGesture: String = "Resting",
    val detectedEmotion: String = "Neutral",
    val framingGuidance: String = "Align your face inside the frame",
    val fps: Int = 30
)

class CameraTracker(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // High-performance real-time ML Kit Face Detector with contours & landmarks
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.12f)
            .build()
        FaceDetection.getClient(options)
    }

    // Real-time ML Kit Pose Detector for upper-body and hand tracking
    private val poseDetector: PoseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    private val _cameraPose = MutableStateFlow(FacialPose())
    val cameraPose: StateFlow<FacialPose> = _cameraPose.asStateFlow()

    private val _stats = MutableStateFlow(CameraTrackingStats())
    val stats: StateFlow<CameraTrackingStats> = _stats.asStateFlow()

    private val _isCameraRunning = MutableStateFlow(false)
    val isCameraRunning: StateFlow<Boolean> = _isCameraRunning.asStateFlow()

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    // Temporal Smoothing states (Exponential Moving Average)
    private var smoothedJaw = 0f
    private var smoothedWidth = 1f
    private var smoothedPucker = 0f
    private var smoothedSmile = 0f
    private var smoothedSmileLeft = 0f
    private var smoothedSmileRight = 0f
    private var smoothedSkew = 0f
    private var smoothedLeftBlink = 0f
    private var smoothedRightBlink = 0f
    private var smoothedBrow = 0f
    private var smoothedLeftBrow = 0f
    private var smoothedRightBrow = 0f
    private var smoothedBrowFurrow = 0f
    private var smoothedPitch = 0f
    private var smoothedYaw = 0f
    private var smoothedRoll = 0f
    private var smoothedShoulderTilt = 0f
    private var smoothedBodyLeanX = 0f

    // Pose tracking points
    private var smoothedLeftElbow: FacialPoint? = null
    private var smoothedRightElbow: FacialPoint? = null
    private var smoothedLeftWrist: FacialPoint? = null
    private var smoothedRightWrist: FacialPoint? = null

    // Waving history for gesture classification
    private var lastLeftWristX = 0f
    private var leftWristOscillations = 0
    private var lastRightWristX = 0f
    private var rightWristOscillations = 0
    private var lastGestureCheckTime = 0L

    // FPS calculation
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var currentFps = 30

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                val bound = bindCameraSafe(lifecycleOwner, previewView)
                if (bound) {
                    _isCameraRunning.value = true
                    onSuccess()
                } else {
                    _isCameraRunning.value = false
                    onError(IllegalStateException("No suitable camera could be bound."))
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                _isCameraRunning.value = false
                onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraSafe(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?
    ): Boolean {
        val provider = cameraProvider ?: return false
        provider.unbindAll()

        // Choose lens facing safely with fallback
        val targetFacing = when {
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
            else -> null
        } ?: return false

        lensFacing = targetFacing
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(targetFacing)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            try {
                processFrameWithMlKit(imageProxy)
            } catch (t: Throwable) {
                t.printStackTrace()
                try { imageProxy.close() } catch (_: Exception) {}
            }
        }

        return try {
            if (previewView != null) {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } else {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback attempt: bind only ImageAnalysis without Preview surface
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
                true
            } catch (e2: Exception) {
                e2.printStackTrace()
                false
            }
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraSafe(lifecycleOwner, previewView)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processFrameWithMlKit(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTimestamp >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTimestamp = now
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        val frameWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val frameHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        val faceTask = faceDetector.process(image)
        val poseTask = poseDetector.process(image)

        Tasks.whenAllComplete(faceTask, poseTask)
            .addOnCompleteListener {
                try {
                    val faces = if (faceTask.isSuccessful) faceTask.result else null
                    val pose = if (poseTask.isSuccessful) poseTask.result else null

                    val face = if (!faces.isNullOrEmpty()) faces[0] else null
                    handleFusedTracking(face, pose, frameWidth.toFloat(), frameHeight.toFloat())
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }
    }

    private fun handleFusedTracking(
        face: Face?,
        pose: Pose?,
        frameW: Float,
        frameH: Float
    ) {
        val fw = frameW.coerceAtLeast(1f)
        val fh = frameH.coerceAtLeast(1f)

        // -------------------------------------------------------------
        // 1. FACE TRACKING
        // -------------------------------------------------------------
        var faceDetected = false
        var rawJaw = 0f
        var rawMouthWidth = 1f
        var rawPucker = 0f
        var rawSmile = 0f
        var rawSmileLeft = 0f
        var rawSmileRight = 0f
        var rawSkew = 0f
        var rawLeftBlink = 0f
        var rawRightBlink = 0f
        var rawBrowRaise = 0f
        var rawLeftBrow = 0f
        var rawRightBrow = 0f
        var rawFurrow = 0f
        var rawPitch = 0f
        var rawYaw = 0f
        var rawRoll = 0f

        if (face != null) {
            faceDetected = true
            val bounds = face.boundingBox
            val faceHeight = bounds.height().toFloat().coerceAtLeast(80f)
            val faceWidth = bounds.width().toFloat().coerceAtLeast(80f)

            // Landmarks & Contours
            val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
            val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
            val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
            val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
            val upperLipContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
            val lowerLipContour = face.getContour(FaceContour.LOWER_LIP_TOP)?.points
            val leftBrowContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points
            val rightBrowContour = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points
            val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
            val rightEyeContour = face.getContour(FaceContour.RIGHT_EYE)?.points

            // Mouth Opening & Lip Aperture
            if (!upperLipContour.isNullOrEmpty() && !lowerLipContour.isNullOrEmpty()) {
                val upperMid = upperLipContour[upperLipContour.size / 2]
                val lowerMid = lowerLipContour[lowerLipContour.size / 2]
                val separation = (lowerMid.y - upperMid.y).coerceAtLeast(0f)
                // Normalize by face height
                rawJaw = (separation / (faceHeight * 0.12f)).coerceIn(0f, 1f)
            } else if (mouthBottom != null && noseBase != null) {
                val dist = abs(mouthBottom.y - noseBase.y)
                val base = faceHeight * 0.22f
                rawJaw = ((dist - base) / (faceHeight * 0.16f)).coerceIn(0f, 1f)
            }

            // Mouth Width & Skew
            if (mouthLeft != null && mouthRight != null) {
                val span = abs(mouthRight.x - mouthLeft.x)
                val baseSpan = faceWidth * 0.35f
                rawMouthWidth = (span / baseSpan).coerceIn(0.65f, 1.45f)

                // Asymmetrical mouth skew (lateral offset)
                val mouthMidX = (mouthLeft.x + mouthRight.x) / 2f
                rawSkew = ((mouthMidX - bounds.exactCenterX()) / (faceWidth * 0.15f)).coerceIn(-1f, 1f)

                // Asymmetric corner elevation for smiling
                val leftCornerY = mouthLeft.y
                val rightCornerY = mouthRight.y
                val cornerDiff = (rightCornerY - leftCornerY) / (faceHeight * 0.05f)
                val globalSmile = face.smilingProbability ?: 0f
                rawSmile = globalSmile
                rawSmileLeft = (globalSmile - cornerDiff * 0.4f).coerceIn(0f, 1f)
                rawSmileRight = (globalSmile + cornerDiff * 0.4f).coerceIn(0f, 1f)
            } else {
                rawSmile = face.smilingProbability ?: 0f
                rawSmileLeft = rawSmile
                rawSmileRight = rawSmile
            }

            // Lip Pucker (narrow mouth + moderate jaw separation)
            if (rawMouthWidth < 0.88f && rawJaw > 0.15f) {
                rawPucker = ((0.9f - rawMouthWidth) / 0.25f).coerceIn(0f, 1f)
            }

            // Eye Blinking (0 = Open, 1 = Closed)
            val leftOpenProb = face.leftEyeOpenProbability ?: 0.9f
            val rightOpenProb = face.rightEyeOpenProbability ?: 0.9f
            rawLeftBlink = (1.0f - leftOpenProb).coerceIn(0f, 1f)
            rawRightBlink = (1.0f - rightOpenProb).coerceIn(0f, 1f)

            // Independent Eyebrow Tracking
            if (!leftBrowContour.isNullOrEmpty() && !leftEyeContour.isNullOrEmpty()) {
                val browY = leftBrowContour[leftBrowContour.size / 2].y
                val eyeY = leftEyeContour[leftEyeContour.size / 2].y
                val dist = (eyeY - browY).coerceAtLeast(0f)
                val baseline = faceHeight * 0.14f
                rawLeftBrow = ((dist - baseline) / (faceHeight * 0.08f)).coerceIn(-1f, 1.2f)
            }
            if (!rightBrowContour.isNullOrEmpty() && !rightEyeContour.isNullOrEmpty()) {
                val browY = rightBrowContour[rightBrowContour.size / 2].y
                val eyeY = rightEyeContour[rightEyeContour.size / 2].y
                val dist = (eyeY - browY).coerceAtLeast(0f)
                val baseline = faceHeight * 0.14f
                rawRightBrow = ((dist - baseline) / (faceHeight * 0.08f)).coerceIn(-1f, 1.2f)
            }
            rawBrowRaise = (rawLeftBrow + rawRightBrow) / 2f

            // Brow Furrow (inner brows pulled down / together)
            if (rawLeftBrow < -0.1f && rawRightBrow < -0.1f) {
                rawFurrow = ((-rawLeftBrow - rawRightBrow) / 2f).coerceIn(0f, 1f)
            }

            // Head Euler Angles
            rawPitch = face.headEulerAngleX.coerceIn(-25f, 25f)
            rawYaw = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                -face.headEulerAngleY.coerceIn(-35f, 35f)
            } else {
                face.headEulerAngleY.coerceIn(-35f, 35f)
            }
            rawRoll = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                -face.headEulerAngleZ.coerceIn(-25f, 25f)
            } else {
                face.headEulerAngleZ.coerceIn(-25f, 25f)
            }
        }

        // -------------------------------------------------------------
        // 2. UPPER-BODY & HAND GESTURE TRACKING (from ML Kit Pose)
        // -------------------------------------------------------------
        var bodyDetected = false
        var leftHandDetected = false
        var rightHandDetected = false
        var rawShoulderTilt = 0f
        var rawBodyLean = 0f
        var rawLeftElbow: FacialPoint? = null
        var rawRightElbow: FacialPoint? = null
        var rawLeftWrist: FacialPoint? = null
        var rawRightWrist: FacialPoint? = null
        var detectedLeftGesture = HandGesture.IDLE
        var detectedRightGesture = HandGesture.IDLE

        if (pose != null) {
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
            val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
            val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
            val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
            val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
            val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
            val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
            val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
            val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)

            fun normPoint(x: Float, y: Float): FacialPoint {
                val nx = if (lensFacing == CameraSelector.LENS_FACING_FRONT) 1f - (x / fw) else (x / fw)
                return FacialPoint(nx.coerceIn(0f, 1f), (y / fh).coerceIn(0f, 1f))
            }

            if (leftShoulder != null && rightShoulder != null &&
                leftShoulder.inFrameLikelihood > 0.45f && rightShoulder.inFrameLikelihood > 0.45f) {
                bodyDetected = true
                val dy = rightShoulder.position.y - leftShoulder.position.y
                val dx = rightShoulder.position.x - leftShoulder.position.x
                rawShoulderTilt = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()).coerceIn(-25f, 25f)
                val midX = (leftShoulder.position.x + rightShoulder.position.x) / 2f
                rawBodyLean = ((midX - (fw / 2f)) / (fw * 0.3f)).coerceIn(-1f, 1f)
            }

            // Left Arm & Hand
            if (leftElbow != null && leftElbow.inFrameLikelihood > 0.45f) {
                rawLeftElbow = normPoint(leftElbow.position.x, leftElbow.position.y)
            }
            if (leftWrist != null && leftWrist.inFrameLikelihood > 0.45f) {
                leftHandDetected = true
                val wristPt = normPoint(leftWrist.position.x, leftWrist.position.y)
                rawLeftWrist = wristPt

                // Hand Gesture classification
                val isRaised = leftShoulder != null && leftWrist.position.y < leftShoulder.position.y
                val isPointing = leftIndex != null && leftPinky != null &&
                        (leftIndex.position.y < leftPinky.position.y - (fh * 0.04f))
                val isSpread = leftThumb != null && leftPinky != null &&
                        (abs(leftThumb.position.x - leftPinky.position.x) > fw * 0.07f)

                // Oscillation check for waving
                val nowTime = System.currentTimeMillis()
                if (nowTime - lastGestureCheckTime > 300) {
                    if (abs(wristPt.x - lastLeftWristX) > 0.04f && isRaised) {
                        leftWristOscillations++
                    } else {
                        leftWristOscillations = max(0, leftWristOscillations - 1)
                    }
                    lastLeftWristX = wristPt.x
                }

                detectedLeftGesture = when {
                    leftWristOscillations >= 2 -> HandGesture.WAVING
                    isPointing -> HandGesture.POINTING
                    isSpread -> HandGesture.OPEN_PALM
                    isRaised -> HandGesture.RAISED
                    else -> HandGesture.IDLE
                }
            }

            // Right Arm & Hand
            if (rightElbow != null && rightElbow.inFrameLikelihood > 0.45f) {
                rawRightElbow = normPoint(rightElbow.position.x, rightElbow.position.y)
            }
            if (rightWrist != null && rightWrist.inFrameLikelihood > 0.45f) {
                rightHandDetected = true
                val wristPt = normPoint(rightWrist.position.x, rightWrist.position.y)
                rawRightWrist = wristPt

                val isRaised = rightShoulder != null && rightWrist.position.y < rightShoulder.position.y
                val isPointing = rightIndex != null && rightPinky != null &&
                        (rightIndex.position.y < rightPinky.position.y - (fh * 0.04f))
                val isSpread = rightThumb != null && rightPinky != null &&
                        (abs(rightThumb.position.x - rightPinky.position.x) > fw * 0.07f)

                val nowTime = System.currentTimeMillis()
                if (nowTime - lastGestureCheckTime > 300) {
                    if (abs(wristPt.x - lastRightWristX) > 0.04f && isRaised) {
                        rightWristOscillations++
                    } else {
                        rightWristOscillations = max(0, rightWristOscillations - 1)
                    }
                    lastRightWristX = wristPt.x
                    lastGestureCheckTime = nowTime
                }

                detectedRightGesture = when {
                    rightWristOscillations >= 2 -> HandGesture.WAVING
                    isPointing -> HandGesture.POINTING
                    isSpread -> HandGesture.OPEN_PALM
                    isRaised -> HandGesture.RAISED
                    else -> HandGesture.IDLE
                }
            }
        }

        // -------------------------------------------------------------
        // 3. TEMPORAL ADAPTIVE EMA SMOOTHING
        // -------------------------------------------------------------
        val alphaFace = 0.45f
        val alphaFast = 0.65f
        val alphaPose = 0.35f

        smoothedJaw = smoothedJaw * (1f - alphaFast) + rawJaw * alphaFast
        smoothedWidth = smoothedWidth * (1f - alphaFace) + rawMouthWidth * alphaFace
        smoothedPucker = smoothedPucker * (1f - alphaFace) + rawPucker * alphaFace
        smoothedSmile = smoothedSmile * (1f - alphaFace) + rawSmile * alphaFace
        smoothedSmileLeft = smoothedSmileLeft * (1f - alphaFace) + rawSmileLeft * alphaFace
        smoothedSmileRight = smoothedSmileRight * (1f - alphaFace) + rawSmileRight * alphaFace
        smoothedSkew = smoothedSkew * (1f - alphaFace) + rawSkew * alphaFace
        smoothedLeftBlink = smoothedLeftBlink * 0.35f + rawLeftBlink * 0.65f
        smoothedRightBlink = smoothedRightBlink * 0.35f + rawRightBlink * 0.65f
        smoothedBrow = smoothedBrow * (1f - alphaFace) + rawBrowRaise * alphaFace
        smoothedLeftBrow = smoothedLeftBrow * (1f - alphaFace) + rawLeftBrow * alphaFace
        smoothedRightBrow = smoothedRightBrow * (1f - alphaFace) + rawRightBrow * alphaFace
        smoothedBrowFurrow = smoothedBrowFurrow * (1f - alphaFace) + rawFurrow * alphaFace
        smoothedPitch = smoothedPitch * 0.55f + rawPitch * 0.45f
        smoothedYaw = smoothedYaw * 0.55f + rawYaw * 0.45f
        smoothedRoll = smoothedRoll * 0.55f + rawRoll * 0.45f
        smoothedShoulderTilt = smoothedShoulderTilt * 0.6f + rawShoulderTilt * 0.4f
        smoothedBodyLeanX = smoothedBodyLeanX * 0.6f + rawBodyLean * 0.4f

        smoothedLeftElbow = rawLeftElbow
        smoothedRightElbow = rawRightElbow
        smoothedLeftWrist = rawLeftWrist
        smoothedRightWrist = rawRightWrist

        // Phonetic Viseme inference based on jaw aperture and mouth width/pucker
        val viseme = when {
            smoothedJaw > 0.45f -> VisemeType.AH
            smoothedPucker > 0.4f -> VisemeType.OH
            smoothedJaw > 0.20f && smoothedSmile > 0.45f -> VisemeType.EE
            smoothedJaw > 0.12f -> VisemeType.TH_SZ
            else -> VisemeType.SILENCE
        }

        // Facial emotion classification
        val emotion = when {
            smoothedSmile > 0.55f -> "Smiling"
            smoothedBrow > 0.45f && smoothedJaw > 0.35f -> "Surprised"
            smoothedBrowFurrow > 0.35f -> "Concentrating"
            abs(smoothedLeftBrow - smoothedRightBrow) > 0.35f -> "Curious"
            smoothedJaw > 0.25f -> "Speaking"
            else -> "Neutral"
        }

        // Camera Framing Guidance
        val guidance = when {
            !faceDetected -> "Center your face in the front camera"
            !bodyDetected -> "Step back slightly to frame shoulders & gestures"
            !leftHandDetected && !rightHandDetected -> "Raise hands into view for gesture tracking"
            else -> "Optimal Framing: Full Body & Expression Active"
        }

        val primaryGesture = when {
            detectedLeftGesture != HandGesture.IDLE -> detectedLeftGesture.displayName
            detectedRightGesture != HandGesture.IDLE -> detectedRightGesture.displayName
            else -> "Resting"
        }

        val updatedPose = FacialPose(
            jawOpen = smoothedJaw,
            mouthWidth = smoothedWidth,
            lipPucker = smoothedPucker,
            mouthSmile = (smoothedSmile * 2f - 1f).coerceIn(-0.4f, 1f),
            mouthSmileLeft = smoothedSmileLeft,
            mouthSmileRight = smoothedSmileRight,
            mouthSkew = smoothedSkew,
            leftEyeBlink = smoothedLeftBlink,
            rightEyeBlink = smoothedRightBlink,
            eyebrowRaise = smoothedBrow,
            leftEyebrowRaise = smoothedLeftBrow,
            rightEyebrowRaise = smoothedRightBrow,
            browFurrow = smoothedBrowFurrow,
            headPitch = smoothedPitch,
            headYaw = smoothedYaw,
            headRoll = smoothedRoll,
            shoulderTilt = smoothedShoulderTilt,
            bodyLeanX = smoothedBodyLeanX,
            leftElbow = smoothedLeftElbow,
            rightElbow = smoothedRightElbow,
            leftWrist = smoothedLeftWrist,
            rightWrist = smoothedRightWrist,
            leftHandGesture = detectedLeftGesture,
            rightHandGesture = detectedRightGesture,
            isFaceDetected = faceDetected,
            isBodyDetected = bodyDetected,
            isLeftHandDetected = leftHandDetected,
            isRightHandDetected = rightHandDetected,
            detectedEmotion = emotion,
            visemeType = viseme,
            audioAmplitude = smoothedJaw
        )

        _cameraPose.value = updatedPose
        _stats.value = CameraTrackingStats(
            isFaceDetected = faceDetected,
            isBodyDetected = bodyDetected,
            isLeftHandDetected = leftHandDetected,
            isRightHandDetected = rightHandDetected,
            mouthAperturePercent = (smoothedJaw * 100).toInt().coerceIn(0, 100),
            eyeOpennessPercent = (((1f - (smoothedLeftBlink + smoothedRightBlink) / 2f)) * 100).toInt().coerceIn(0, 100),
            smilePercent = (smoothedSmile * 100).toInt().coerceIn(0, 100),
            browRaisePercent = ((smoothedBrow + 1f) * 50).toInt().coerceIn(0, 100),
            headTiltDegrees = smoothedRoll,
            headYawDegrees = smoothedYaw,
            headPitchDegrees = smoothedPitch,
            detectedGesture = primaryGesture,
            detectedEmotion = emotion,
            framingGuidance = guidance,
            fps = currentFps
        )
    }

    fun stopCamera() {
        _isCameraRunning.value = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        _cameraPose.value = FacialPose()
        _stats.value = CameraTrackingStats()
    }
}

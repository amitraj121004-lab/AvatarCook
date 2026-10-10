package com.example.engine.rig

import com.example.data.model.AnimationSettings
import com.example.data.model.FacialPoint
import com.example.data.model.FacialPose
import com.example.data.model.HandGesture
import kotlin.math.sin
import kotlin.random.Random

class FacialRig {

    private var lastBlinkTimeMs = System.currentTimeMillis()
    private var nextBlinkIntervalMs = Random.nextLong(2800, 4500)
    private var isBlinking = false
    private var blinkStartTimeMs = 0L
    private val blinkDurationMs = 170L

    // Filtered pose state
    private var currentPose = FacialPose()

    /**
     * Updates the facial rig for the current frame time, combining input signals,
     * smoothing filter, idle breathing / head sway, and natural autonomic blinking.
     */
    fun updatePose(
        inputPose: FacialPose,
        settings: AnimationSettings,
        currentTimeMs: Long = System.currentTimeMillis()
    ): FacialPose {
        // 1. Natural Autonomic Blinking Engine
        var autonomicBlink = 0f
        if (settings.idleAnimationEnabled) {
            val adjustedInterval = (nextBlinkIntervalMs / settings.blinkFrequency.coerceAtLeast(0.2f)).toLong()
            if (!isBlinking && (currentTimeMs - lastBlinkTimeMs > adjustedInterval)) {
                isBlinking = true
                blinkStartTimeMs = currentTimeMs
            }

            if (isBlinking) {
                val elapsed = currentTimeMs - blinkStartTimeMs
                if (elapsed >= blinkDurationMs) {
                    isBlinking = false
                    lastBlinkTimeMs = currentTimeMs
                    nextBlinkIntervalMs = Random.nextLong(2600, 4200)
                    autonomicBlink = 0f
                } else {
                    // Parabolic curve: 0 -> 1 -> 0
                    val progress = elapsed.toFloat() / blinkDurationMs
                    autonomicBlink = sin(progress * Math.PI).toFloat().coerceIn(0f, 1f)
                }
            }
        }

        // 2. Idle Life Motion (breathing cycle, micro-sway, and subtle posture shift)
        var idleHeadPitch = 0f
        var idleHeadYaw = 0f
        var idleHeadRoll = 0f
        var idleBodyLean = 0f
        if (settings.idleAnimationEnabled) {
            val breathTime = currentTimeMs / 1000.0
            idleHeadPitch = (sin(breathTime * 1.4) * 1.0 * settings.headMovementIntensity).toFloat()
            idleHeadYaw = (sin(breathTime * 0.7) * 1.5 * settings.headMovementIntensity).toFloat()
            idleHeadRoll = (sin(breathTime * 1.0) * 0.7 * settings.headMovementIntensity).toFloat()
            idleBodyLean = (sin(breathTime * 0.5) * 0.04f).toFloat()
        }

        // 3. Apply settings multipliers and toggles
        val intensity = settings.expressionIntensity

        // Jaw aperture: if input is very small (< 0.04f), snap to 0f for a genuinely closed resting mouth
        val rawJawClamped = if (inputPose.jawOpen < 0.04f) 0f else inputPose.jawOpen
        val targetJaw = if (settings.mouthTrackingEnabled) (rawJawClamped * intensity).coerceIn(0f, 1f) else 0f

        val targetWidth = if (settings.mouthTrackingEnabled) {
            1f + (inputPose.mouthWidth - 1f) * intensity
        } else 1f

        val targetPucker = if (settings.mouthTrackingEnabled) (inputPose.lipPucker * intensity).coerceIn(0f, 1f) else 0f
        val targetSmile = (inputPose.mouthSmile * intensity).coerceIn(-1f, 1f)
        val targetSmileLeft = (inputPose.mouthSmileLeft * intensity).coerceIn(0f, 1f)
        val targetSmileRight = (inputPose.mouthSmileRight * intensity).coerceIn(0f, 1f)
        val targetSkew = (inputPose.mouthSkew * intensity).coerceIn(-1f, 1f)

        val targetLeftBlink = if (settings.eyeTrackingEnabled) {
            (inputPose.leftEyeBlink + autonomicBlink).coerceIn(0f, 1f)
        } else autonomicBlink

        val targetRightBlink = if (settings.eyeTrackingEnabled) {
            (inputPose.rightEyeBlink + autonomicBlink).coerceIn(0f, 1f)
        } else autonomicBlink

        val targetBrow = (inputPose.eyebrowRaise * intensity).coerceIn(-1f, 1.2f)
        val targetLeftBrow = if (settings.asymmetricBrowsEnabled) {
            (inputPose.leftEyebrowRaise * intensity).coerceIn(-1f, 1.2f)
        } else targetBrow
        val targetRightBrow = if (settings.asymmetricBrowsEnabled) {
            (inputPose.rightEyebrowRaise * intensity).coerceIn(-1f, 1.2f)
        } else targetBrow
        val targetFurrow = (inputPose.browFurrow * intensity).coerceIn(0f, 1f)

        val targetPitch = if (settings.headTrackingEnabled) {
            (inputPose.headPitch * settings.headMovementIntensity) + idleHeadPitch
        } else idleHeadPitch

        val targetYaw = if (settings.headTrackingEnabled) {
            (inputPose.headYaw * settings.headMovementIntensity) + idleHeadYaw
        } else idleHeadYaw

        val targetRoll = if (settings.headTrackingEnabled) {
            (inputPose.headRoll * settings.headMovementIntensity) + idleHeadRoll
        } else idleHeadRoll

        val targetShoulderTilt = if (settings.bodyTrackingEnabled) {
            inputPose.shoulderTilt.coerceIn(-20f, 20f)
        } else 0f

        val targetBodyLean = if (settings.bodyTrackingEnabled) {
            inputPose.bodyLeanX + idleBodyLean
        } else idleBodyLean

        // 4. Smooth interpolation (EMA filter based on settings.faceSmoothing)
        val alpha = (1.0f - settings.faceSmoothing).coerceIn(0.20f, 0.85f)
        val alphaFast = (alpha * 1.25f).coerceAtMost(0.95f)

        currentPose = FacialPose(
            jawOpen = lerp(currentPose.jawOpen, targetJaw, alphaFast),
            mouthWidth = lerp(currentPose.mouthWidth, targetWidth, alpha),
            lipPucker = lerp(currentPose.lipPucker, targetPucker, alpha),
            mouthSmile = lerp(currentPose.mouthSmile, targetSmile, alpha),
            mouthSmileLeft = lerp(currentPose.mouthSmileLeft, targetSmileLeft, alpha),
            mouthSmileRight = lerp(currentPose.mouthSmileRight, targetSmileRight, alpha),
            mouthSkew = lerp(currentPose.mouthSkew, targetSkew, alpha),
            leftEyeBlink = lerp(currentPose.leftEyeBlink, targetLeftBlink, 0.55f),
            rightEyeBlink = lerp(currentPose.rightEyeBlink, targetRightBlink, 0.55f),
            eyebrowRaise = lerp(currentPose.eyebrowRaise, targetBrow, alpha),
            leftEyebrowRaise = lerp(currentPose.leftEyebrowRaise, targetLeftBrow, alpha),
            rightEyebrowRaise = lerp(currentPose.rightEyebrowRaise, targetRightBrow, alpha),
            browFurrow = lerp(currentPose.browFurrow, targetFurrow, alpha),
            headPitch = lerp(currentPose.headPitch, targetPitch, alpha * 0.75f),
            headYaw = lerp(currentPose.headYaw, targetYaw, alpha * 0.75f),
            headRoll = lerp(currentPose.headRoll, targetRoll, alpha * 0.75f),
            shoulderTilt = lerp(currentPose.shoulderTilt, targetShoulderTilt, alpha * 0.6f),
            bodyLeanX = lerp(currentPose.bodyLeanX, targetBodyLean, alpha * 0.6f),
            leftElbow = if (settings.handTrackingEnabled) inputPose.leftElbow else null,
            rightElbow = if (settings.handTrackingEnabled) inputPose.rightElbow else null,
            leftWrist = if (settings.handTrackingEnabled) inputPose.leftWrist else null,
            rightWrist = if (settings.handTrackingEnabled) inputPose.rightWrist else null,
            leftHandGesture = if (settings.handTrackingEnabled) inputPose.leftHandGesture else HandGesture.IDLE,
            rightHandGesture = if (settings.handTrackingEnabled) inputPose.rightHandGesture else HandGesture.IDLE,
            isFaceDetected = inputPose.isFaceDetected,
            isBodyDetected = inputPose.isBodyDetected,
            isLeftHandDetected = inputPose.isLeftHandDetected,
            isRightHandDetected = inputPose.isRightHandDetected,
            detectedEmotion = inputPose.detectedEmotion,
            visemeType = inputPose.visemeType,
            audioAmplitude = inputPose.audioAmplitude
        )

        return currentPose
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }
}

package com.example.engine.rig

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.model.AnimationSettings
import com.example.data.model.AspectRatioType
import com.example.data.model.BackgroundType
import com.example.data.model.CharacterPreset
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPoint
import com.example.data.model.FacialPose
import com.example.data.model.HandGesture
import com.example.data.model.VisemeType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class AvatarRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val skinShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mouthCavityPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val teethPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val upperLipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lowerLipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lipContourPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeScleraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val catchlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyelidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lashPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val browPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hairHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clothPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val armPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val landmarkDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val landmarkLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        mouthCavityPaint.color = Color.rgb(38, 12, 18)
        mouthCavityPaint.style = Paint.Style.FILL

        teethPaint.color = Color.rgb(248, 248, 252)
        teethPaint.style = Paint.Style.FILL

        tonguePaint.color = Color.rgb(225, 95, 115)
        tonguePaint.style = Paint.Style.FILL

        upperLipPaint.style = Paint.Style.FILL
        lowerLipPaint.style = Paint.Style.FILL

        lipContourPaint.style = Paint.Style.STROKE
        lipContourPaint.strokeWidth = 3f

        eyeScleraPaint.color = Color.rgb(250, 250, 255)
        eyeScleraPaint.style = Paint.Style.FILL

        pupilPaint.color = Color.rgb(15, 15, 20)
        pupilPaint.style = Paint.Style.FILL

        catchlightPaint.color = Color.WHITE
        catchlightPaint.style = Paint.Style.FILL

        lashPaint.color = Color.rgb(25, 22, 32)
        lashPaint.style = Paint.Style.STROKE
        lashPaint.strokeCap = Paint.Cap.ROUND

        browPaint.style = Paint.Style.STROKE
        browPaint.strokeCap = Paint.Cap.ROUND

        armPaint.style = Paint.Style.STROKE
        armPaint.strokeCap = Paint.Cap.ROUND
        armPaint.strokeJoin = Paint.Join.ROUND

        handPaint.style = Paint.Style.FILL

        landmarkDotPaint.color = Color.rgb(6, 182, 212)
        landmarkDotPaint.style = Paint.Style.FILL

        landmarkLinePaint.color = Color.argb(160, 168, 85, 247)
        landmarkLinePaint.strokeWidth = 2f
        landmarkLinePaint.style = Paint.Style.STROKE
    }

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        pose: FacialPose,
        preset: CharacterPreset,
        customBitmap: Bitmap? = null,
        landmarks: FacialLandmarks = preset.defaultLandmarks,
        settings: AnimationSettings = AnimationSettings(),
        showLandmarksOverlay: Boolean = false
    ) {
        // 1. Studio Background
        drawBackground(canvas, width, height, settings.backgroundType, preset)

        // 2. Aspect Ratio Framing
        val frameRect = calculateFrameRect(width, height, settings.aspectRatio)
        canvas.save()
        canvas.clipRect(frameRect)

        // 3. 2.5D Head & Body Transformations (Pitch, Yaw, Roll, Body Lean, Shoulder Tilt)
        val centerX = frameRect.centerX()
        val centerY = frameRect.centerY()
        val baseScale = (frameRect.width() / 480f).coerceAtLeast(0.5f)

        canvas.save()
        val anchorY = frameRect.bottom - (frameRect.height() * 0.10f)
        canvas.translate(centerX, anchorY)

        // Shoulder tilt & lateral torso lean
        canvas.rotate(pose.shoulderTilt * 0.45f)
        canvas.translate(pose.bodyLeanX * 24f * baseScale, 0f)

        // Head roll & subtle parallax
        canvas.rotate(pose.headRoll)
        canvas.translate(pose.headYaw * 1.3f * baseScale, pose.headPitch * 0.95f * baseScale)
        canvas.translate(-centerX, -anchorY)

        // 4. Render Avatar
        if (customBitmap != null && !customBitmap.isRecycled) {
            renderCustomCharacter(canvas, frameRect, customBitmap, pose, landmarks, baseScale)
        } else {
            renderProPresetAvatar(canvas, frameRect, preset, pose, baseScale)
        }

        canvas.restore() // restore transform

        // 5. Landmark Wireframe Overlay
        if (showLandmarksOverlay) {
            drawLandmarksOverlay(canvas, frameRect, landmarks)
        }

        canvas.restore() // restore clip
    }

    private fun calculateFrameRect(canvasWidth: Int, canvasHeight: Int, ratio: AspectRatioType): RectF {
        val targetRatio = ratio.ratioWidth / ratio.ratioHeight
        val currentRatio = canvasWidth.toFloat() / canvasHeight.toFloat()

        return if (currentRatio > targetRatio) {
            val targetWidth = canvasHeight * targetRatio
            val left = (canvasWidth - targetWidth) / 2f
            RectF(left, 0f, left + targetWidth, canvasHeight.toFloat())
        } else {
            val targetHeight = canvasWidth / targetRatio
            val top = (canvasHeight - targetHeight) / 2f
            RectF(0f, top, canvasWidth.toFloat(), top + targetHeight)
        }
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, type: BackgroundType, preset: CharacterPreset) {
        when (type) {
            BackgroundType.STUDIO_DARK -> {
                val shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.rgb(10, 11, 18), Color.rgb(20, 24, 38),
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                bgPaint.shader = null

                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val colorHex = preset.accentColorHex
                    val r = (colorHex shr 16 and 0xFF).toInt()
                    val g = (colorHex shr 8 and 0xFF).toInt()
                    val b = (colorHex and 0xFF).toInt()
                    setShader(
                        RadialGradient(
                            width / 2f, height * 0.40f, width * 0.60f,
                            Color.argb(55, r, g, b), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), haloPaint)
            }
            BackgroundType.CYBER_GRID -> {
                canvas.drawColor(Color.rgb(12, 10, 24))
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(40, 139, 92, 246)
                    strokeWidth = 2f
                }
                val step = width / 12f
                for (x in 0..12) canvas.drawLine(x * step, 0f, x * step, height.toFloat(), gridPaint)
                for (y in 0..22) canvas.drawLine(0f, y * step, width.toFloat(), y * step, gridPaint)
            }
            BackgroundType.GRADIENT_PURPLE -> {
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.rgb(40, 12, 90), Color.rgb(124, 58, 237),
                    Shader.TileMode.CLAMP
                )
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                bgPaint.shader = null
            }
            BackgroundType.CHROMA_GREEN -> canvas.drawColor(Color.rgb(0, 255, 0))
            BackgroundType.CLEAN_SLATE -> canvas.drawColor(Color.rgb(28, 33, 46))
        }
    }

    private fun samplePixelSafe(bitmap: Bitmap, normX: Float, normY: Float, defaultColor: Int): Int {
        val px = (normX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val py = (normY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        return try {
            bitmap.getPixel(px, py)
        } catch (_: Exception) {
            defaultColor
        }
    }

    /**
     * Renders custom character photo with organic multi-tangent point rig deformation.
     * Deforms eyebrows independently, animates eyelids with skin-tone folds,
     * pulls chin down with jaw opening, renders fine tangent lips and oral cavity,
     * and articulates upper body arms & hands when gestures are detected.
     */
    private fun renderCustomCharacter(
        canvas: Canvas,
        frameRect: RectF,
        bitmap: Bitmap,
        pose: FacialPose,
        landmarks: FacialLandmarks,
        baseScale: Float
    ) {
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        val scale = (frameRect.width() / bmpW).coerceAtLeast(frameRect.height() / bmpH)
        val dstW = bmpW * scale
        val dstH = bmpH * scale
        val dstLeft = frameRect.centerX() - (dstW / 2f)
        val dstTop = frameRect.centerY() - (dstH / 2f)
        val dstRect = RectF(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH)

        // 1. Draw original base portrait photo
        canvas.drawBitmap(bitmap, null, dstRect, avatarPaint)

        // 2. Sample local facial tones for organic blending
        val rawLipColor = samplePixelSafe(bitmap, landmarks.upperLipCenter.x, landmarks.upperLipCenter.y, Color.rgb(180, 80, 95))
        val rawSkinColor = samplePixelSafe(
            bitmap,
            landmarks.noseTip.x,
            (landmarks.upperLipCenter.y + landmarks.noseTip.y) / 2f,
            Color.rgb(215, 160, 140)
        )
        val rawBrowColor = samplePixelSafe(
            bitmap,
            landmarks.leftEyebrowPeak.x,
            landmarks.leftEyebrowPeak.y,
            Color.rgb(55, 45, 40)
        )
        val rawEyeSkinColor = samplePixelSafe(
            bitmap,
            landmarks.leftEyeCenter.x,
            (landmarks.leftEyeCenter.y - 0.035f).coerceAtLeast(0.01f),
            rawSkinColor
        )

        val lipR = Color.red(rawLipColor)
        val lipG = Color.green(rawLipColor)
        val lipB = Color.blue(rawLipColor)
        val skinR = Color.red(rawSkinColor)
        val skinG = Color.green(rawSkinColor)
        val skinB = Color.blue(rawSkinColor)

        // 3. Multi-point Tangent Rig Projection for Mouth
        val mouthX = dstRect.left + (landmarks.mouthCenter.x * dstRect.width()) + (pose.mouthSkew * 10f * baseScale)
        val mouthY = dstRect.top + (landmarks.mouthCenter.y * dstRect.height())
        val leftCornerX = dstRect.left + (landmarks.mouthLeftCorner.x * dstRect.width())
        val leftCornerY = dstRect.top + (landmarks.mouthLeftCorner.y * dstRect.height())
        val rightCornerX = dstRect.left + (landmarks.mouthRightCorner.x * dstRect.width())
        val rightCornerY = dstRect.top + (landmarks.mouthRightCorner.y * dstRect.height())

        val baseWidth = (rightCornerX - leftCornerX).coerceIn(dstRect.width() * 0.12f, dstRect.width() * 0.40f)

        // Asymmetrical smile corner elevations
        val leftSmileLift = pose.mouthSmileLeft * (dstRect.height() * 0.014f)
        val rightSmileLift = pose.mouthSmileRight * (dstRect.height() * 0.014f)
        val adjLeftCornerY = leftCornerY - leftSmileLift
        val adjRightCornerY = rightCornerY - rightSmileLift

        // Dynamic mouth aperture
        val mouthOpen = pose.jawOpen * (dstRect.height() * 0.058f)
        val animatedHalfW = (baseWidth * pose.mouthWidth * 0.5f).coerceAtLeast(baseWidth * 0.35f)

        // Pucker pulls corners inward
        val puckerPull = pose.lipPucker * animatedHalfW * 0.32f
        val riggedLeftX = mouthX - animatedHalfW + puckerPull
        val riggedRightX = mouthX + animatedHalfW - puckerPull

        val topLipApertureY = mouthY - (mouthOpen * 0.38f)
        val botLipApertureY = mouthY + (mouthOpen * 0.62f)

        // 4. Render seamless oral cavity only when lips genuinely part
        if (pose.jawOpen > 0.04f) {
            val quarterW = animatedHalfW * 0.5f

            val mouthPath = Path().apply {
                moveTo(riggedLeftX, adjLeftCornerY)
                // Upper lip tangent across Cupid's bow
                cubicTo(
                    mouthX - quarterW, topLipApertureY + (mouthOpen * 0.08f),
                    mouthX - (quarterW * 0.4f), topLipApertureY,
                    mouthX, topLipApertureY
                )
                cubicTo(
                    mouthX + (quarterW * 0.4f), topLipApertureY,
                    mouthX + quarterW, topLipApertureY + (mouthOpen * 0.08f),
                    riggedRightX, adjRightCornerY
                )
                // Lower lip tangent
                cubicTo(
                    mouthX + quarterW, botLipApertureY - (mouthOpen * 0.05f),
                    mouthX + (quarterW * 0.5f), botLipApertureY,
                    mouthX, botLipApertureY
                )
                cubicTo(
                    mouthX - (quarterW * 0.5f), botLipApertureY,
                    mouthX - quarterW, botLipApertureY - (mouthOpen * 0.05f),
                    riggedLeftX, adjLeftCornerY
                )
                close()
            }

            // Dark oral cavity with vertical lighting depth
            val cavityShader = LinearGradient(
                mouthX, topLipApertureY, mouthX, botLipApertureY,
                Color.rgb(32, 8, 14), Color.rgb(15, 3, 6),
                Shader.TileMode.CLAMP
            )
            mouthCavityPaint.shader = cavityShader
            canvas.drawPath(mouthPath, mouthCavityPaint)
            mouthCavityPaint.shader = null

            // Upper dental arch (teeth)
            if (mouthOpen > 5f) {
                val teethH = (mouthOpen * 0.36f).coerceAtMost(18f * baseScale)
                val teethPath = Path().apply {
                    moveTo(mouthX - (animatedHalfW * 0.65f), topLipApertureY + 1f)
                    cubicTo(
                        mouthX - (animatedHalfW * 0.32f), topLipApertureY + teethH,
                        mouthX + (animatedHalfW * 0.32f), topLipApertureY + teethH,
                        mouthX + (animatedHalfW * 0.65f), topLipApertureY + 1f
                    )
                    close()
                }
                teethPaint.color = Color.rgb(248, 248, 252)
                canvas.drawPath(teethPath, teethPaint)

                // Incisor division line
                val teethShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(45, 0, 0, 0)
                    strokeWidth = 1.2f
                }
                canvas.drawLine(mouthX, topLipApertureY + 1f, mouthX, topLipApertureY + teethH * 0.82f, teethShadowPaint)
            }

            // Dynamic tongue posture mapped by speech Viseme
            if (pose.jawOpen > 0.18f) {
                val tongueH = when (pose.visemeType) {
                    VisemeType.EE -> mouthOpen * 0.42f
                    VisemeType.OH -> mouthOpen * 0.26f
                    VisemeType.AH -> mouthOpen * 0.35f
                    else -> mouthOpen * 0.30f
                }
                val tongueW = animatedHalfW * 0.85f
                val tongueRect = RectF(
                    mouthX - (tongueW / 2f),
                    botLipApertureY - tongueH,
                    mouthX + (tongueW / 2f),
                    botLipApertureY
                )
                canvas.drawOval(tongueRect, tonguePaint)
            }

            // High-fidelity vermilion border blending cushions
            val strokeThick = (mouthOpen * 0.18f).coerceIn(2.5f, 6.0f)
            val innerLipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(175, lipR, lipG, lipB)
                style = Paint.Style.STROKE
                strokeWidth = strokeThick
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(mouthPath, innerLipPaint)

            val outerBlendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(105, (lipR + skinR) / 2, (lipG + skinG) / 2, (lipB + skinB) / 2)
                style = Paint.Style.STROKE
                strokeWidth = strokeThick * 1.5f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(mouthPath, outerBlendPaint)
        } else {
            // Closed Resting Mouth: Clean lip seal contour preserving original artwork without holes
            val closedLipPath = Path().apply {
                moveTo(riggedLeftX, adjLeftCornerY)
                quadTo(mouthX, mouthY + (2f * baseScale) - (leftSmileLift + rightSmileLift) * 0.5f, riggedRightX, adjRightCornerY)
            }
            val closedLipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(185, lipR, lipG, lipB)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * baseScale
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(closedLipPath, closedLipPaint)
        }

        // 5. Independent Eyebrows Deformed Organically
        val browColor = rawBrowColor
        val browR = Color.red(browColor)
        val browG = Color.green(browColor)
        val browB = Color.blue(browColor)

        fun drawDeformedBrow(inner: FacialPoint, peak: FacialPoint, outer: FacialPoint, raise: Float, furrow: Float, isLeft: Boolean) {
            val inX = dstRect.left + (inner.x * dstRect.width()) + (if (isLeft) furrow * 4f * baseScale else -furrow * 4f * baseScale)
            val inY = dstRect.top + (inner.y * dstRect.height()) - (raise * 16f * baseScale) + (furrow * 6f * baseScale)

            val pkX = dstRect.left + (peak.x * dstRect.width())
            val pkY = dstRect.top + (peak.y * dstRect.height()) - (raise * 20f * baseScale)

            val outX = dstRect.left + (outer.x * dstRect.width())
            val outY = dstRect.top + (outer.y * dstRect.height()) - (raise * 12f * baseScale)

            val bPath = Path().apply {
                moveTo(inX, inY)
                quadTo(pkX, pkY, outX, outY)
            }

            val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, browR, browG, browB)
                style = Paint.Style.STROKE
                strokeWidth = 3.5f * baseScale
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(bPath, bPaint)
        }

        if (abs(pose.leftEyebrowRaise) > 0.05f || pose.browFurrow > 0.05f) {
            drawDeformedBrow(landmarks.leftEyebrowInner, landmarks.leftEyebrowPeak, landmarks.leftEyebrowOuter, pose.leftEyebrowRaise, pose.browFurrow, true)
        }
        if (abs(pose.rightEyebrowRaise) > 0.05f || pose.browFurrow > 0.05f) {
            drawDeformedBrow(landmarks.rightEyebrowInner, landmarks.rightEyebrowPeak, landmarks.rightEyebrowOuter, pose.rightEyebrowRaise, pose.browFurrow, false)
        }

        // 6. Realistic Eyelid Blink Rigging
        val leftEyeX = dstRect.left + (landmarks.leftEyeCenter.x * dstRect.width())
        val leftEyeY = dstRect.top + (landmarks.leftEyeCenter.y * dstRect.height())
        val rightEyeX = dstRect.left + (landmarks.rightEyeCenter.x * dstRect.width())
        val rightEyeY = dstRect.top + (landmarks.rightEyeCenter.y * dstRect.height())
        val eyeRx = dstRect.width() * 0.042f
        val eyeRy = dstRect.height() * 0.025f

        val eyeSkinR = Color.red(rawEyeSkinColor)
        val eyeSkinG = Color.green(rawEyeSkinColor)
        val eyeSkinB = Color.blue(rawEyeSkinColor)

        fun drawNaturalEyelid(eyeCenterX: Float, eyeCenterY: Float, blinkRatio: Float) {
            if (blinkRatio <= 0.05f) return

            val clampedBlink = blinkRatio.coerceIn(0f, 1f)
            val topCreaseY = eyeCenterY - eyeRy
            val currentLidFoldY = topCreaseY + (eyeRy * 2f * clampedBlink)

            val lidPath = Path().apply {
                moveTo(eyeCenterX - eyeRx, eyeCenterY)
                cubicTo(
                    eyeCenterX - (eyeRx * 0.5f), topCreaseY,
                    eyeCenterX + (eyeRx * 0.5f), topCreaseY,
                    eyeCenterX + eyeRx, eyeCenterY
                )
                cubicTo(
                    eyeCenterX + (eyeRx * 0.5f), currentLidFoldY,
                    eyeCenterX - (eyeRx * 0.5f), currentLidFoldY,
                    eyeCenterX - eyeRx, eyeCenterY
                )
                close()
            }

            val lidShader = LinearGradient(
                eyeCenterX, topCreaseY, eyeCenterX, currentLidFoldY,
                Color.rgb((eyeSkinR * 0.94f).toInt(), (eyeSkinG * 0.94f).toInt(), (eyeSkinB * 0.94f).toInt()),
                Color.rgb(eyeSkinR, eyeSkinG, eyeSkinB),
                Shader.TileMode.CLAMP
            )
            eyelidPaint.shader = lidShader
            eyelidPaint.alpha = (clampedBlink * 240).toInt().coerceIn(120, 245)
            canvas.drawPath(lidPath, eyelidPaint)
            eyelidPaint.shader = null

            val lashStroke = Path().apply {
                moveTo(eyeCenterX - eyeRx, eyeCenterY)
                quadTo(eyeCenterX, currentLidFoldY, eyeCenterX + eyeRx, eyeCenterY)
            }
            val naturalLashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((clampedBlink * 200).toInt(), 32, 28, 38)
                strokeWidth = (2.2f * clampedBlink).coerceAtLeast(1.2f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(lashStroke, naturalLashPaint)
        }

        drawNaturalEyelid(leftEyeX, leftEyeY, pose.leftEyeBlink)
        drawNaturalEyelid(rightEyeX, rightEyeY, pose.rightEyeBlink)

        // 7. Upper Body, Arms and Hand Gestures
        if (pose.isBodyDetected || pose.leftWrist != null || pose.rightWrist != null ||
            pose.leftHandGesture != HandGesture.IDLE || pose.rightHandGesture != HandGesture.IDLE) {
            drawUpperBodyAndHands(
                canvas = canvas,
                frameRect = frameRect,
                pose = pose,
                baseScale = baseScale,
                skinColor = rawSkinColor,
                sleeveColor = Color.rgb(24, 28, 40)
            )
        }
    }

    /**
     * Renders high-fidelity artist-grade character models with full upper body and gestural rigs.
     */
    private fun renderProPresetAvatar(
        canvas: Canvas,
        frameRect: RectF,
        preset: CharacterPreset,
        pose: FacialPose,
        scale: Float
    ) {
        val cx = frameRect.centerX()
        val cy = frameRect.centerY()

        val yawShift = (pose.headYaw * 1.5f * scale).coerceIn(-28f * scale, 28f * scale)
        val pitchShift = (pose.headPitch * 1.2f * scale).coerceIn(-20f * scale, 20f * scale)

        val accentColor = Color.rgb(
            (preset.accentColorHex shr 16 and 0xFF).toInt(),
            (preset.accentColorHex shr 8 and 0xFF).toInt(),
            (preset.accentColorHex and 0xFF).toInt()
        )
        val baseColor = Color.rgb(
            (preset.baseColorHex shr 16 and 0xFF).toInt(),
            (preset.baseColorHex shr 8 and 0xFF).toInt(),
            (preset.baseColorHex and 0xFF).toInt()
        )

        // 1. Shoulders & Studio Clothing with tilt
        clothPaint.color = baseColor
        val shoulderY = cy + (135f * scale)
        val clothPath = Path().apply {
            moveTo(cx - (200f * scale), frameRect.bottom + 80f)
            cubicTo(
                cx - (160f * scale), shoulderY + (10f * scale),
                cx - (70f * scale), shoulderY - (20f * scale),
                cx - (40f * scale), shoulderY - (10f * scale)
            )
            lineTo(cx + (40f * scale), shoulderY - (10f * scale))
            cubicTo(
                cx + (70f * scale), shoulderY - (20f * scale),
                cx + (160f * scale), shoulderY + (10f * scale),
                cx + (200f * scale), frameRect.bottom + 80f
            )
            close()
        }
        canvas.drawPath(clothPath, clothPaint)

        // Collar trim
        accentGlowPaint.color = accentColor
        accentGlowPaint.style = Paint.Style.STROKE
        accentGlowPaint.strokeWidth = 4f * scale
        canvas.drawLine(cx - (40f * scale), shoulderY - (10f * scale), cx, shoulderY + (35f * scale), accentGlowPaint)
        canvas.drawLine(cx + (40f * scale), shoulderY - (10f * scale), cx, shoulderY + (35f * scale), accentGlowPaint)

        // 2. Neck
        val neckW = 34f * scale
        val neckTopY = cy + (50f * scale)
        skinPaint.color = Color.rgb(245, 212, 192)
        val neckPath = Path().apply {
            moveTo(cx - neckW, neckTopY)
            lineTo(cx + neckW, neckTopY)
            lineTo(cx + neckW + (10f * scale), shoulderY)
            lineTo(cx - neckW - (10f * scale), shoulderY)
            close()
        }
        canvas.drawPath(neckPath, skinPaint)

        // Neck shadow
        skinShadowPaint.color = Color.argb(45, 120, 60, 50)
        canvas.drawOval(RectF(cx - neckW, neckTopY - (5f * scale), cx + neckW, neckTopY + (25f * scale)), skinShadowPaint)

        // 3. Hair Back
        hairPaint.color = when (preset.id) {
            "preset_nova" -> Color.rgb(38, 22, 68)
            "preset_aria" -> Color.rgb(190, 45, 110)
            "preset_ethan" -> Color.rgb(55, 42, 36)
            "preset_marcus" -> Color.rgb(28, 32, 40)
            else -> Color.rgb(90, 48, 22)
        }

        val headCenterY = cy - (20f * scale) + pitchShift
        val headCenterX = cx + yawShift

        val hairBackPath = Path().apply {
            moveTo(headCenterX - (115f * scale), headCenterY + (30f * scale))
            cubicTo(
                headCenterX - (130f * scale), headCenterY - (120f * scale),
                headCenterX + (130f * scale), headCenterY - (120f * scale),
                headCenterX + (115f * scale), headCenterY + (30f * scale)
            )
            cubicTo(
                headCenterX + (125f * scale), headCenterY + (100f * scale),
                headCenterX - (125f * scale), headCenterY + (100f * scale),
                headCenterX - (115f * scale), headCenterY + (30f * scale)
            )
            close()
        }
        canvas.drawPath(hairBackPath, hairPaint)

        // 4. Head & Face Contour
        val faceRx = 88f * scale
        val faceRy = 115f * scale
        val chinY = headCenterY + faceRy + (pose.jawOpen * 14f * scale)

        val facePath = Path().apply {
            moveTo(headCenterX - faceRx, headCenterY - (20f * scale))
            cubicTo(
                headCenterX - faceRx, headCenterY - (95f * scale),
                headCenterX + faceRx, headCenterY - (95f * scale),
                headCenterX + faceRx, headCenterY - (20f * scale)
            )
            cubicTo(
                headCenterX + faceRx, headCenterY + (60f * scale),
                headCenterX + (35f * scale), chinY,
                headCenterX, chinY
            )
            cubicTo(
                headCenterX - (35f * scale), chinY,
                headCenterX - faceRx, headCenterY + (60f * scale),
                headCenterX - faceRx, headCenterY - (20f * scale)
            )
            close()
        }

        val skinShader = LinearGradient(
            headCenterX, headCenterY - faceRy,
            headCenterX, chinY,
            Color.rgb(255, 230, 218), Color.rgb(246, 210, 195),
            Shader.TileMode.CLAMP
        )
        skinPaint.shader = skinShader
        canvas.drawPath(facePath, skinPaint)
        skinPaint.shader = null

        // Soft blush
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, 240, 90, 110)
        }
        canvas.drawCircle(headCenterX - (52f * scale), headCenterY + (22f * scale), 20f * scale, blushPaint)
        canvas.drawCircle(headCenterX + (52f * scale), headCenterY + (22f * scale), 20f * scale, blushPaint)

        // 5. Independent Eyebrows
        browPaint.color = Color.rgb(45, 35, 48)
        browPaint.strokeWidth = 4f * scale
        val browSpan = 32f * scale

        // Left Brow with independent raise and furrow
        val leftBrowY = headCenterY - (36f * scale) - (pose.leftEyebrowRaise * 14f * scale) + (pose.browFurrow * 5f * scale)
        val browLeftPath = Path().apply {
            moveTo(headCenterX - (65f * scale), leftBrowY + (2f * scale))
            quadTo(headCenterX - (48f * scale), leftBrowY - (6f * scale), headCenterX - (65f * scale) + browSpan, leftBrowY)
        }
        canvas.drawPath(browLeftPath, browPaint)

        // Right Brow with independent raise and furrow
        val rightBrowY = headCenterY - (36f * scale) - (pose.rightEyebrowRaise * 14f * scale) + (pose.browFurrow * 5f * scale)
        val browRightPath = Path().apply {
            moveTo(headCenterX + (65f * scale) - browSpan, rightBrowY)
            quadTo(headCenterX + (48f * scale), rightBrowY - (6f * scale), headCenterX + (65f * scale), rightBrowY + (2f * scale))
        }
        canvas.drawPath(browRightPath, browPaint)

        // 6. Expressive Eyes
        val eyeBaseY = headCenterY - (14f * scale)
        val eyeDist = 44f * scale
        val eyeRx = 19f * scale
        val eyeRy = 13f * scale

        drawExpressiveEye(
            canvas = canvas,
            eyeCenterX = headCenterX - eyeDist,
            eyeCenterY = eyeBaseY,
            rx = eyeRx,
            ry = eyeRy,
            blink = pose.leftEyeBlink,
            irisColor = Color.rgb(
                (preset.eyeColorHex shr 16 and 0xFF).toInt(),
                (preset.eyeColorHex shr 8 and 0xFF).toInt(),
                (preset.eyeColorHex and 0xFF).toInt()
            ),
            gazeX = (pose.headYaw * 0.35f * scale).coerceIn(-5f * scale, 5f * scale),
            gazeY = (pose.headPitch * 0.25f * scale).coerceIn(-4f * scale, 4f * scale),
            scale = scale
        )

        drawExpressiveEye(
            canvas = canvas,
            eyeCenterX = headCenterX + eyeDist,
            eyeCenterY = eyeBaseY,
            rx = eyeRx,
            ry = eyeRy,
            blink = pose.rightEyeBlink,
            irisColor = Color.rgb(
                (preset.eyeColorHex shr 16 and 0xFF).toInt(),
                (preset.eyeColorHex shr 8 and 0xFF).toInt(),
                (preset.eyeColorHex and 0xFF).toInt()
            ),
            gazeX = (pose.headYaw * 0.35f * scale).coerceIn(-5f * scale, 5f * scale),
            gazeY = (pose.headPitch * 0.25f * scale).coerceIn(-4f * scale, 4f * scale),
            scale = scale
        )

        // 7. Refined Nose
        val noseY = headCenterY + (22f * scale)
        val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 205, 140, 125)
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * scale
            strokeCap = Paint.Cap.ROUND
        }
        val nosePath = Path().apply {
            moveTo(headCenterX + (2f * scale), noseY - (14f * scale))
            lineTo(headCenterX + (4f * scale), noseY)
            lineTo(headCenterX - (3f * scale), noseY)
        }
        canvas.drawPath(nosePath, nosePaint)

        // 8. Articulated Lips & Lip-Sync Viseme Rig
        val mouthY = headCenterY + (56f * scale) + (pose.jawOpen * 6f * scale)
        val mouthW = (42f * scale * pose.mouthWidth).coerceIn(28f * scale, 68f * scale)
        val mouthOpen = (pose.jawOpen * 36f * scale).coerceAtLeast(0f)
        val leftSmileLift = (pose.mouthSmileLeft * 8f * scale)
        val rightSmileLift = (pose.mouthSmileRight * 8f * scale)

        upperLipPaint.color = Color.rgb(215, 95, 115)
        lowerLipPaint.color = Color.rgb(230, 110, 130)

        if (pose.jawOpen < 0.04f) {
            // Closed smile / neutral lip line
            val lipLinePath = Path().apply {
                moveTo(headCenterX - (mouthW / 2f), mouthY - leftSmileLift)
                quadTo(headCenterX, mouthY + (2f * scale) - ((leftSmileLift + rightSmileLift) * 0.5f), headCenterX + (mouthW / 2f), mouthY - rightSmileLift)
            }
            lipContourPaint.color = Color.rgb(195, 75, 95)
            lipContourPaint.strokeWidth = 3.5f * scale
            canvas.drawPath(lipLinePath, lipContourPaint)

            val lowerLipHighlight = Path().apply {
                moveTo(headCenterX - (mouthW * 0.3f), mouthY + (6f * scale) - leftSmileLift * 0.5f)
                quadTo(headCenterX, mouthY + (9f * scale) - ((leftSmileLift + rightSmileLift) * 0.5f), headCenterX + (mouthW * 0.3f), mouthY + (6f * scale) - rightSmileLift * 0.5f)
            }
            val lowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(90, 215, 95, 115)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * scale
            }
            canvas.drawPath(lowerLipHighlight, lowerPaint)
        } else {
            val halfW = mouthW / 2f
            val topY = mouthY - (mouthOpen * 0.38f)
            val botY = mouthY + (mouthOpen * 0.62f)

            val mouthAperture = Path().apply {
                moveTo(headCenterX - halfW, mouthY - leftSmileLift)
                cubicTo(
                    headCenterX - (halfW * 0.5f), topY,
                    headCenterX + (halfW * 0.5f), topY,
                    headCenterX + halfW, mouthY - rightSmileLift
                )
                cubicTo(
                    headCenterX + (halfW * 0.5f), botY,
                    headCenterX - (halfW * 0.5f), botY,
                    headCenterX - halfW, mouthY - leftSmileLift
                )
                close()
            }
            canvas.drawPath(mouthAperture, mouthCavityPaint)

            // Upper teeth
            if (mouthOpen > 6f * scale && pose.visemeType != VisemeType.OH) {
                val teethH = (mouthOpen * 0.34f).coerceAtMost(10f * scale)
                val teethPath = Path().apply {
                    moveTo(headCenterX - (halfW * 0.65f), topY + (1.5f * scale))
                    quadTo(headCenterX, topY + teethH, headCenterX + (halfW * 0.65f), topY + (1.5f * scale))
                    close()
                }
                canvas.drawPath(teethPath, teethPaint)
            }

            // Tongue
            if (pose.jawOpen > 0.24f) {
                val tongueW = halfW * 0.44f
                val tongueH = mouthOpen * 0.32f
                canvas.drawOval(
                    RectF(headCenterX - tongueW, botY - (tongueH * 1.5f), headCenterX + tongueW, botY),
                    tonguePaint
                )
            }

            lipContourPaint.color = Color.rgb(205, 80, 100)
            lipContourPaint.strokeWidth = 3f * scale
            canvas.drawPath(mouthAperture, lipContourPaint)
        }

        // 9. Foreground Hair
        drawStylizedHairFront(canvas, headCenterX, headCenterY, preset, scale)

        // 10. Cyber Host Visor
        if (preset.id == "preset_nova") {
            drawCyberHeadset(canvas, headCenterX, headCenterY, accentColor, scale)
        }

        // 11. Upper Body Arms & Gestures
        drawUpperBodyAndHands(
            canvas = canvas,
            frameRect = frameRect,
            pose = pose,
            baseScale = scale,
            skinColor = Color.rgb(245, 212, 192),
            sleeveColor = baseColor
        )
    }

    private fun drawExpressiveEye(
        canvas: Canvas,
        eyeCenterX: Float,
        eyeCenterY: Float,
        rx: Float,
        ry: Float,
        blink: Float,
        irisColor: Int,
        gazeX: Float,
        gazeY: Float,
        scale: Float
    ) {
        val effectiveRy = (ry * (1f - blink)).coerceAtLeast(1f)

        val eyeRect = RectF(eyeCenterX - rx, eyeCenterY - effectiveRy, eyeCenterX + rx, eyeCenterY + effectiveRy)
        canvas.drawOval(eyeRect, eyeScleraPaint)

        if (blink < 0.85f) {
            canvas.save()
            canvas.clipRect(eyeRect)

            val irisRadius = 9.5f * scale
            val irisX = eyeCenterX + gazeX
            val irisY = eyeCenterY + gazeY

            irisPaint.color = irisColor
            canvas.drawCircle(irisX, irisY, irisRadius, irisPaint)
            canvas.drawCircle(irisX, irisY, irisRadius * 0.52f, pupilPaint)

            canvas.drawCircle(irisX - (2.5f * scale), irisY - (2.5f * scale), 2.8f * scale, catchlightPaint)
            canvas.drawCircle(irisX + (3f * scale), irisY + (2.5f * scale), 1.4f * scale, catchlightPaint)

            canvas.restore()
        }

        val lashPath = Path().apply {
            moveTo(eyeCenterX - rx - (2f * scale), eyeCenterY - (effectiveRy * 0.4f))
            quadTo(eyeCenterX, eyeCenterY - effectiveRy - (1f * scale), eyeCenterX + rx + (2f * scale), eyeCenterY - (effectiveRy * 0.4f))
        }
        lashPaint.strokeWidth = 3f * scale
        canvas.drawPath(lashPath, lashPaint)

        val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 60, 40, 50)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        val creasePath = Path().apply {
            moveTo(eyeCenterX - (rx * 0.8f), eyeCenterY - ry - (4f * scale))
            quadTo(eyeCenterX, eyeCenterY - ry - (7f * scale), eyeCenterX + (rx * 0.8f), eyeCenterY - ry - (4f * scale))
        }
        canvas.drawPath(creasePath, creasePaint)
    }

    private fun drawStylizedHairFront(
        canvas: Canvas,
        headCenterX: Float,
        headCenterY: Float,
        preset: CharacterPreset,
        scale: Float
    ) {
        val hairColor = when (preset.id) {
            "preset_nova" -> Color.rgb(55, 32, 95)
            "preset_aria" -> Color.rgb(225, 60, 130)
            "preset_ethan" -> Color.rgb(65, 48, 40)
            "preset_marcus" -> Color.rgb(32, 38, 48)
            else -> Color.rgb(105, 58, 28)
        }
        hairPaint.color = hairColor

        val fringePath = Path().apply {
            moveTo(headCenterX - (96f * scale), headCenterY - (30f * scale))
            cubicTo(
                headCenterX - (70f * scale), headCenterY - (75f * scale),
                headCenterX - (30f * scale), headCenterY - (60f * scale),
                headCenterX - (15f * scale), headCenterY - (48f * scale)
            )
            cubicTo(
                headCenterX + (5f * scale), headCenterY - (58f * scale),
                headCenterX + (40f * scale), headCenterY - (72f * scale),
                headCenterX + (96f * scale), headCenterY - (30f * scale)
            )
            cubicTo(
                headCenterX + (80f * scale), headCenterY - (115f * scale),
                headCenterX - (80f * scale), headCenterY - (115f * scale),
                headCenterX - (96f * scale), headCenterY - (30f * scale)
            )
            close()
        }
        canvas.drawPath(fringePath, hairPaint)

        hairHighlightPaint.color = Color.argb(45, 255, 255, 255)
        hairHighlightPaint.style = Paint.Style.STROKE
        hairHighlightPaint.strokeWidth = 3f * scale
        val highlightPath = Path().apply {
            moveTo(headCenterX - (50f * scale), headCenterY - (92f * scale))
            quadTo(headCenterX, headCenterY - (98f * scale), headCenterX + (50f * scale), headCenterY - (92f * scale))
        }
        canvas.drawPath(highlightPath, hairHighlightPaint)
    }

    private fun drawCyberHeadset(
        canvas: Canvas,
        headCenterX: Float,
        headCenterY: Float,
        accentColor: Int,
        scale: Float
    ) {
        val earY = headCenterY + (5f * scale)
        val headsetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 24, 38)
            style = Paint.Style.FILL
        }
        val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(
            RectF(headCenterX - (98f * scale), earY - (18f * scale), headCenterX - (85f * scale), earY + (18f * scale)),
            6f * scale, 6f * scale, headsetPaint
        )
        canvas.drawCircle(headCenterX - (91.5f * scale), earY, 4f * scale, ledPaint)

        canvas.drawRoundRect(
            RectF(headCenterX + (85f * scale), earY - (18f * scale), headCenterX + (98f * scale), earY + (18f * scale)),
            6f * scale, 6f * scale, headsetPaint
        )
        canvas.drawCircle(headCenterX + (91.5f * scale), earY, 4f * scale, ledPaint)
    }

    /**
     * Renders articulated upper body arms and expressive hands based on live camera pose detection.
     */
    private fun drawUpperBodyAndHands(
        canvas: Canvas,
        frameRect: RectF,
        pose: FacialPose,
        baseScale: Float,
        skinColor: Int,
        sleeveColor: Int
    ) {
        armPaint.color = sleeveColor
        armPaint.strokeWidth = 24f * baseScale
        handPaint.color = skinColor

        val cx = frameRect.centerX()
        val cy = frameRect.centerY()
        val shoulderY = cy + (130f * baseScale)
        val leftShoulderX = cx - (150f * baseScale)
        val rightShoulderX = cx + (150f * baseScale)

        // Helper to draw single articulated arm & hand
        fun drawArm(
            isLeft: Boolean,
            shoulderX: Float,
            shoulderY: Float,
            wrist: com.example.data.model.FacialPoint?,
            gesture: HandGesture
        ) {
            if (wrist == null && gesture == HandGesture.IDLE) return

            val targetWristX = if (wrist != null) {
                frameRect.left + (wrist.x * frameRect.width())
            } else {
                if (isLeft) shoulderX - (40f * baseScale) else shoulderX + (40f * baseScale)
            }

            val targetWristY = if (wrist != null) {
                frameRect.top + (wrist.y * frameRect.height())
            } else {
                shoulderY + (120f * baseScale)
            }

            // Anatomically bounded elbow joint
            val midX = (shoulderX + targetWristX) / 2f + (if (isLeft) -35f * baseScale else 35f * baseScale)
            val midY = (shoulderY + targetWristY) / 2f + (20f * baseScale)

            // Draw arm limb
            val armPath = Path().apply {
                moveTo(shoulderX, shoulderY)
                quadTo(midX, midY, targetWristX, targetWristY)
            }
            canvas.drawPath(armPath, armPaint)

            // Draw expressive stylized hand
            val handRadius = 14f * baseScale
            canvas.drawCircle(targetWristX, targetWristY, handRadius, handPaint)

            when (gesture) {
                HandGesture.WAVING -> {
                    // Waving palm with motion lines
                    val wavePath = Path().apply {
                        moveTo(targetWristX - handRadius, targetWristY - handRadius)
                        lineTo(targetWristX, targetWristY - (handRadius * 2.2f))
                        lineTo(targetWristX + handRadius, targetWristY - handRadius)
                        close()
                    }
                    canvas.drawPath(wavePath, handPaint)

                    // Motion arcs
                    val motionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(130, 6, 182, 212)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.5f * baseScale
                    }
                    canvas.drawArc(
                        RectF(targetWristX - (handRadius * 2.5f), targetWristY - (handRadius * 3f), targetWristX + (handRadius * 2.5f), targetWristY),
                        -120f, 60f, false, motionPaint
                    )
                }
                HandGesture.POINTING -> {
                    // Extended index finger
                    val dir = if (isLeft) -1f else 1f
                    val fingerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = skinColor
                        style = Paint.Style.STROKE
                        strokeWidth = 6f * baseScale
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(
                        targetWristX, targetWristY,
                        targetWristX + (dir * 28f * baseScale), targetWristY - (18f * baseScale),
                        fingerPaint
                    )
                }
                HandGesture.OPEN_PALM, HandGesture.RAISED -> {
                    // Spread fingers
                    val fingerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = skinColor
                        style = Paint.Style.STROKE
                        strokeWidth = 4.5f * baseScale
                        strokeCap = Paint.Cap.ROUND
                    }
                    for (i in -2..2) {
                        val angle = Math.toRadians(-90.0 + (i * 18.0))
                        val len = 20f * baseScale
                        canvas.drawLine(
                            targetWristX, targetWristY,
                            (targetWristX + cos(angle) * len).toFloat(),
                            (targetWristY + sin(angle) * len).toFloat(),
                            fingerPaint
                        )
                    }
                }
                else -> {
                    // Resting hand
                }
            }
        }

        drawArm(true, leftShoulderX, shoulderY, pose.leftWrist, pose.leftHandGesture)
        drawArm(false, rightShoulderX, shoulderY, pose.rightWrist, pose.rightHandGesture)
    }

    private fun drawLandmarksOverlay(canvas: Canvas, frameRect: RectF, landmarks: FacialLandmarks) {
        val w = frameRect.width()
        val h = frameRect.height()
        val ox = frameRect.left
        val oy = frameRect.top

        fun toScreenX(normX: Float) = ox + (normX * w)
        fun toScreenY(normY: Float) = oy + (normY * h)

        val pts = listOf(
            landmarks.leftEyebrowInner,
            landmarks.leftEyebrowPeak,
            landmarks.leftEyebrowOuter,
            landmarks.rightEyebrowInner,
            landmarks.rightEyebrowPeak,
            landmarks.rightEyebrowOuter,
            landmarks.leftEyeCenter,
            landmarks.rightEyeCenter,
            landmarks.noseTip,
            landmarks.mouthLeftCorner,
            landmarks.upperLipCenter,
            landmarks.mouthRightCorner,
            landmarks.lowerLipCenter,
            landmarks.chin,
            landmarks.leftJaw,
            landmarks.rightJaw
        )

        // Eyebrow lines
        canvas.drawLine(toScreenX(landmarks.leftEyebrowOuter.x), toScreenY(landmarks.leftEyebrowOuter.y),
            toScreenX(landmarks.leftEyebrowPeak.x), toScreenY(landmarks.leftEyebrowPeak.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.leftEyebrowPeak.x), toScreenY(landmarks.leftEyebrowPeak.y),
            toScreenX(landmarks.leftEyebrowInner.x), toScreenY(landmarks.leftEyebrowInner.y), landmarkLinePaint)

        canvas.drawLine(toScreenX(landmarks.rightEyebrowInner.x), toScreenY(landmarks.rightEyebrowInner.y),
            toScreenX(landmarks.rightEyebrowPeak.x), toScreenY(landmarks.rightEyebrowPeak.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.rightEyebrowPeak.x), toScreenY(landmarks.rightEyebrowPeak.y),
            toScreenX(landmarks.rightEyebrowOuter.x), toScreenY(landmarks.rightEyebrowOuter.y), landmarkLinePaint)

        // Eyes & nose
        canvas.drawLine(toScreenX(landmarks.leftEyeCenter.x), toScreenY(landmarks.leftEyeCenter.y),
            toScreenX(landmarks.rightEyeCenter.x), toScreenY(landmarks.rightEyeCenter.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.leftEyeCenter.x), toScreenY(landmarks.leftEyeCenter.y),
            toScreenX(landmarks.noseTip.x), toScreenY(landmarks.noseTip.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.rightEyeCenter.x), toScreenY(landmarks.rightEyeCenter.y),
            toScreenX(landmarks.noseTip.x), toScreenY(landmarks.noseTip.y), landmarkLinePaint)

        // Mouth contour
        canvas.drawLine(toScreenX(landmarks.mouthLeftCorner.x), toScreenY(landmarks.mouthLeftCorner.y),
            toScreenX(landmarks.upperLipCenter.x), toScreenY(landmarks.upperLipCenter.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.upperLipCenter.x), toScreenY(landmarks.upperLipCenter.y),
            toScreenX(landmarks.mouthRightCorner.x), toScreenY(landmarks.mouthRightCorner.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.mouthRightCorner.x), toScreenY(landmarks.mouthRightCorner.y),
            toScreenX(landmarks.lowerLipCenter.x), toScreenY(landmarks.lowerLipCenter.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.lowerLipCenter.x), toScreenY(landmarks.lowerLipCenter.y),
            toScreenX(landmarks.mouthLeftCorner.x), toScreenY(landmarks.mouthLeftCorner.y), landmarkLinePaint)

        // Jaw & Chin
        canvas.drawLine(toScreenX(landmarks.leftJaw.x), toScreenY(landmarks.leftJaw.y),
            toScreenX(landmarks.chin.x), toScreenY(landmarks.chin.y), landmarkLinePaint)
        canvas.drawLine(toScreenX(landmarks.chin.x), toScreenY(landmarks.chin.y),
            toScreenX(landmarks.rightJaw.x), toScreenY(landmarks.rightJaw.y), landmarkLinePaint)

        pts.forEach { pt ->
            canvas.drawCircle(toScreenX(pt.x), toScreenY(pt.y), 6f, landmarkDotPaint)
        }
    }
}

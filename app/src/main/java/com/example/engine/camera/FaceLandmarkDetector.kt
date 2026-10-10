package com.example.engine.camera

import android.graphics.Bitmap
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPoint
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FaceLandmarkDetector {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.10f)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun autoDetectLandmarks(bitmap: Bitmap): FacialLandmarks = withContext(Dispatchers.Default) {
        try {
            // Downscale very large bitmaps for fast and reliable ML Kit inference
            val maxDim = maxOf(bitmap.width, bitmap.height)
            val scaledBitmap = if (maxDim > 1024) {
                val scale = 1024f / maxDim
                val sw = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val sh = (bitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            } else {
                bitmap
            }

            val image = InputImage.fromBitmap(scaledBitmap, 0)
            val task = detector.process(image)
            val faces = Tasks.await(task)

            if (!faces.isNullOrEmpty()) {
                val face = faces[0]
                val bw = scaledBitmap.width.toFloat().coerceAtLeast(1f)
                val bh = scaledBitmap.height.toFloat().coerceAtLeast(1f)
                val bounds = face.boundingBox

                fun norm(x: Float, y: Float): FacialPoint {
                    return FacialPoint(
                        x = (x / bw).coerceIn(0.02f, 0.98f),
                        y = (y / bh).coerceIn(0.02f, 0.98f)
                    )
                }

                // Eye landmarks
                val leftEyePos = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                val rightEyePos = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                val noseBasePos = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeftPos = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRightPos = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
                val mouthBottomPos = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

                // Contours
                val upperLipTop = face.getContour(FaceContour.UPPER_LIP_TOP)?.points
                val lowerLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points
                val leftBrowTop = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points
                val rightBrowTop = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points
                val faceContour = face.getContour(FaceContour.FACE)?.points

                val upperLipPoint = when {
                    !upperLipTop.isNullOrEmpty() -> {
                        val mid = upperLipTop[upperLipTop.size / 2]
                        norm(mid.x, mid.y)
                    }
                    noseBasePos != null && mouthBottomPos != null -> {
                        norm(noseBasePos.x, noseBasePos.y + (mouthBottomPos.y - noseBasePos.y) * 0.35f)
                    }
                    else -> {
                        norm(bounds.exactCenterX(), bounds.top + bounds.height() * 0.72f)
                    }
                }

                val lowerLipPoint = when {
                    !lowerLipBottom.isNullOrEmpty() -> {
                        val mid = lowerLipBottom[lowerLipBottom.size / 2]
                        norm(mid.x, mid.y)
                    }
                    mouthBottomPos != null -> {
                        norm(mouthBottomPos.x, mouthBottomPos.y)
                    }
                    else -> {
                        norm(bounds.exactCenterX(), bounds.top + bounds.height() * 0.82f)
                    }
                }

                val mouthCenterPoint = FacialPoint(
                    x = (upperLipPoint.x + lowerLipPoint.x) / 2f,
                    y = (upperLipPoint.y + lowerLipPoint.y) / 2f
                )

                val mouthLeftPoint = when {
                    mouthLeftPos != null -> norm(mouthLeftPos.x, mouthLeftPos.y)
                    else -> norm(bounds.left + bounds.width() * 0.32f, mouthCenterPoint.y * bh)
                }

                val mouthRightPoint = when {
                    mouthRightPos != null -> norm(mouthRightPos.x, mouthRightPos.y)
                    else -> norm(bounds.left + bounds.width() * 0.68f, mouthCenterPoint.y * bh)
                }

                val leftEyePoint = when {
                    leftEyePos != null -> norm(leftEyePos.x, leftEyePos.y)
                    else -> norm(bounds.left + bounds.width() * 0.35f, bounds.top + bounds.height() * 0.40f)
                }

                val rightEyePoint = when {
                    rightEyePos != null -> norm(rightEyePos.x, rightEyePos.y)
                    else -> norm(bounds.left + bounds.width() * 0.65f, bounds.top + bounds.height() * 0.40f)
                }

                // Eyebrow multi-points (inner, peak, outer)
                val leftBrowInner = if (!leftBrowTop.isNullOrEmpty()) {
                    norm(leftBrowTop.last().x, leftBrowTop.last().y)
                } else FacialPoint(leftEyePoint.x + 0.05f, (leftEyePoint.y - 0.05f).coerceAtLeast(0.04f))

                val leftBrowPeak = if (!leftBrowTop.isNullOrEmpty()) {
                    val mid = leftBrowTop[leftBrowTop.size / 2]
                    norm(mid.x, mid.y)
                } else FacialPoint(leftEyePoint.x, (leftEyePoint.y - 0.06f).coerceAtLeast(0.04f))

                val leftBrowOuter = if (!leftBrowTop.isNullOrEmpty()) {
                    norm(leftBrowTop.first().x, leftBrowTop.first().y)
                } else FacialPoint(leftEyePoint.x - 0.06f, (leftEyePoint.y - 0.04f).coerceAtLeast(0.04f))

                val rightBrowInner = if (!rightBrowTop.isNullOrEmpty()) {
                    norm(rightBrowTop.first().x, rightBrowTop.first().y)
                } else FacialPoint(rightEyePoint.x - 0.05f, (rightEyePoint.y - 0.05f).coerceAtLeast(0.04f))

                val rightBrowPeak = if (!rightBrowTop.isNullOrEmpty()) {
                    val mid = rightBrowTop[rightBrowTop.size / 2]
                    norm(mid.x, mid.y)
                } else FacialPoint(rightEyePoint.x, (rightEyePoint.y - 0.06f).coerceAtLeast(0.04f))

                val rightBrowOuter = if (!rightBrowTop.isNullOrEmpty()) {
                    norm(rightBrowTop.last().x, rightBrowTop.last().y)
                } else FacialPoint(rightEyePoint.x + 0.06f, (rightEyePoint.y - 0.04f).coerceAtLeast(0.04f))

                val nosePoint = when {
                    noseBasePos != null -> norm(noseBasePos.x, noseBasePos.y)
                    else -> norm(bounds.exactCenterX(), bounds.top + bounds.height() * 0.58f)
                }

                val chinPoint = norm(
                    bounds.exactCenterX(),
                    (bounds.bottom.toFloat()).coerceAtMost(bh * 0.98f)
                )

                val leftJawPoint = if (!faceContour.isNullOrEmpty()) {
                    val pt = faceContour[faceContour.size / 4]
                    norm(pt.x, pt.y)
                } else norm(bounds.left.toFloat(), bounds.top + bounds.height() * 0.75f)

                val rightJawPoint = if (!faceContour.isNullOrEmpty()) {
                    val pt = faceContour[(faceContour.size * 3) / 4]
                    norm(pt.x, pt.y)
                } else norm(bounds.right.toFloat(), bounds.top + bounds.height() * 0.75f)

                val leftShoulderPoint = FacialPoint(
                    x = (leftEyePoint.x - 0.18f).coerceAtLeast(0.05f),
                    y = (chinPoint.y + 0.16f).coerceAtMost(0.96f)
                )
                val rightShoulderPoint = FacialPoint(
                    x = (rightEyePoint.x + 0.18f).coerceAtMost(0.95f),
                    y = (chinPoint.y + 0.16f).coerceAtMost(0.96f)
                )

                FacialLandmarks(
                    leftEyeCenter = leftEyePoint,
                    rightEyeCenter = rightEyePoint,
                    leftEyebrowInner = leftBrowInner,
                    leftEyebrowPeak = leftBrowPeak,
                    leftEyebrowOuter = leftBrowOuter,
                    rightEyebrowInner = rightBrowInner,
                    rightEyebrowPeak = rightBrowPeak,
                    rightEyebrowOuter = rightBrowOuter,
                    noseTip = nosePoint,
                    mouthCenter = mouthCenterPoint,
                    mouthLeftCorner = mouthLeftPoint,
                    mouthRightCorner = mouthRightPoint,
                    upperLipCenter = upperLipPoint,
                    lowerLipCenter = lowerLipPoint,
                    chin = chinPoint,
                    leftJaw = leftJawPoint,
                    rightJaw = rightJawPoint,
                    leftShoulder = leftShoulderPoint,
                    rightShoulder = rightShoulderPoint
                )
            } else {
                getPortraitDefaultLandmarks()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getPortraitDefaultLandmarks()
        }
    }

    private fun getPortraitDefaultLandmarks(): FacialLandmarks {
        return FacialLandmarks(
            leftEyeCenter = FacialPoint(0.38f, 0.36f),
            rightEyeCenter = FacialPoint(0.62f, 0.36f),
            leftEyebrowInner = FacialPoint(0.44f, 0.31f),
            leftEyebrowPeak = FacialPoint(0.38f, 0.29f),
            leftEyebrowOuter = FacialPoint(0.31f, 0.32f),
            rightEyebrowInner = FacialPoint(0.56f, 0.31f),
            rightEyebrowPeak = FacialPoint(0.62f, 0.29f),
            rightEyebrowOuter = FacialPoint(0.69f, 0.32f),
            noseTip = FacialPoint(0.50f, 0.46f),
            mouthCenter = FacialPoint(0.50f, 0.54f),
            mouthLeftCorner = FacialPoint(0.42f, 0.54f),
            mouthRightCorner = FacialPoint(0.58f, 0.54f),
            upperLipCenter = FacialPoint(0.50f, 0.52f),
            lowerLipCenter = FacialPoint(0.50f, 0.56f),
            chin = FacialPoint(0.50f, 0.66f),
            leftJaw = FacialPoint(0.28f, 0.58f),
            rightJaw = FacialPoint(0.72f, 0.58f),
            leftShoulder = FacialPoint(0.20f, 0.85f),
            rightShoulder = FacialPoint(0.80f, 0.85f)
        )
    }
}

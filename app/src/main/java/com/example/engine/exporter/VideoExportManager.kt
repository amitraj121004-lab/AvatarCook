package com.example.engine.exporter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.data.local.ExportedVideoEntity
import com.example.data.model.AnimationSettings
import com.example.data.model.AspectRatioType
import com.example.data.model.CharacterPreset
import com.example.data.model.ExportResolution
import com.example.data.model.ExportSettings
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPose
import com.example.data.model.VisemeCue
import com.example.engine.audio.AudioWaveformExtractor
import com.example.engine.rig.AvatarRenderer
import com.example.engine.rig.FacialRig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class VideoExportManager(private val context: Context) {

    private val renderer = AvatarRenderer()
    private val facialRig = FacialRig()

    @Volatile
    private var isCancelled = false

    // Persistent state across drain cycles
    private var videoTrackIndex = -1
    private var muxerStarted = false
    private var samplesWritten = 0

    fun cancelExport() {
        isCancelled = true
    }

    suspend fun exportVideo(
        title: String,
        preset: CharacterPreset,
        customBitmap: Bitmap? = null,
        landmarks: FacialLandmarks = preset.defaultLandmarks,
        visemes: List<VisemeCue>,
        totalDurationMs: Long,
        animationSettings: AnimationSettings,
        exportSettings: ExportSettings,
        modeUsed: String,
        onProgress: (Float) -> Unit
    ): ExportedVideoEntity = withContext(Dispatchers.Default) {
        isCancelled = false
        videoTrackIndex = -1
        muxerStarted = false
        samplesWritten = 0

        val durationMs = totalDurationMs.coerceAtLeast(2000L)

        // Calculate strictly 16-pixel aligned dimensions to ensure hardware codec compatibility
        val (videoWidth, videoHeight) = calculateDimensions(exportSettings.resolution, animationSettings.aspectRatio)

        val videosDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val timeStamp = System.currentTimeMillis()
        val videoFile = File(videosDir, "avatar_${timeStamp}.mp4")
        val thumbFile = File(videosDir, "thumb_${timeStamp}.jpg")

        val fps = exportSettings.fps.coerceIn(24, 30)
        val totalFrames = ((durationMs / 1000.0) * fps).roundToInt().coerceAtLeast(30)
        val frameIntervalUs = 1_000_000L / fps

        val frameBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(videoWidth * videoHeight)
        val yuvBuffer = ByteArray(videoWidth * videoHeight * 3 / 2)

        var muxer: MediaMuxer? = null
        var videoCodec: MediaCodec? = null

        try {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, calculateBitrate(videoWidth, videoHeight))
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoCodec.start()

            muxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var firstFrameThumbnailSaved = false

            for (frameIndex in 0 until totalFrames) {
                if (isCancelled) {
                    throw InterruptedException("Export cancelled by creator.")
                }

                val currentMs = (frameIndex * 1000L) / fps
                val cue = AudioWaveformExtractor.getVisemeAtTimestamp(visemes, currentMs)

                val rawPose = FacialPose(
                    jawOpen = (cue.intensity * 0.95f).coerceIn(0f, 1f),
                    mouthWidth = when (cue.type) {
                        com.example.data.model.VisemeType.EE -> 1.35f
                        com.example.data.model.VisemeType.OH -> 0.72f
                        else -> 1.0f
                    },
                    lipPucker = if (cue.type == com.example.data.model.VisemeType.OH) 0.85f else 0.05f,
                    visemeType = cue.type,
                    audioAmplitude = cue.intensity
                )
                val finalPose = facialRig.updatePose(rawPose, animationSettings, currentMs)

                // Render into canvas
                renderer.render(
                    canvas = frameCanvas,
                    width = videoWidth,
                    height = videoHeight,
                    pose = finalPose,
                    preset = preset,
                    customBitmap = customBitmap,
                    landmarks = landmarks,
                    settings = animationSettings,
                    showLandmarksOverlay = false
                )

                // Save thumbnail
                if (!firstFrameThumbnailSaved && frameIndex >= 4.coerceAtMost(totalFrames - 1)) {
                    FileOutputStream(thumbFile).use { fos ->
                        frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    firstFrameThumbnailSaved = true
                }

                // Convert to YUV420SemiPlanar
                frameBitmap.getPixels(argbPixels, 0, videoWidth, 0, 0, videoWidth, videoHeight)
                encodeYUV420SP(yuvBuffer, argbPixels, videoWidth, videoHeight)

                // Queue input buffer with retry to prevent dropped frames
                var inputBufferIndex = -1
                var retries = 0
                while (inputBufferIndex < 0 && retries < 5) {
                    inputBufferIndex = videoCodec.dequeueInputBuffer(15000)
                    if (inputBufferIndex < 0) {
                        retries++
                        Thread.sleep(3)
                    }
                }

                if (inputBufferIndex >= 0) {
                    val inputBuffer = videoCodec.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    val bytesToPut = minOf(yuvBuffer.size, inputBuffer?.capacity() ?: yuvBuffer.size)
                    inputBuffer?.put(yuvBuffer, 0, bytesToPut)
                    val ptsUs = frameIndex * frameIntervalUs
                    val flags = if (frameIndex == totalFrames - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    videoCodec.queueInputBuffer(inputBufferIndex, 0, bytesToPut, ptsUs, flags)
                }

                // Drain output buffers
                drainCodec(videoCodec, muxer, bufferInfo, isEos = false)

                onProgress((frameIndex + 1).toFloat() / totalFrames)
            }

            // Signal and drain EOS
            drainCodec(videoCodec, muxer, bufferInfo, isEos = true)

            if (!firstFrameThumbnailSaved) {
                FileOutputStream(thumbFile).use { fos ->
                    frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
            }

            val fileSizeBytes = if (videoFile.exists()) videoFile.length() else 0L

            ExportedVideoEntity(
                title = title.ifBlank { "Talking Avatar Project" },
                characterName = if (customBitmap != null) "Custom Avatar" else preset.name,
                videoPath = videoFile.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else null,
                durationMs = durationMs,
                resolution = "${videoWidth}x${videoHeight}",
                aspectRatio = animationSettings.aspectRatio.displayName.substringBefore(" "),
                fps = fps,
                fileSizeBytes = fileSizeBytes,
                modeUsed = modeUsed,
                createdAt = System.currentTimeMillis()
            )

        } finally {
            try {
                videoCodec?.stop()
            } catch (_: Exception) {}
            try {
                videoCodec?.release()
            } catch (_: Exception) {}
            try {
                if (muxerStarted && samplesWritten > 0) {
                    muxer?.stop()
                }
            } catch (_: Exception) {}
            try {
                muxer?.release()
            } catch (_: Exception) {}
            if (!frameBitmap.isRecycled) {
                frameBitmap.recycle()
            }
        }
    }

    private fun drainCodec(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        isEos: Boolean
    ) {
        var noOutputCounter = 0
        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        val newFormat = codec.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    noOutputCounter = 0
                }
                outputBufferIndex >= 0 -> {
                    noOutputCounter = 0
                    val encodedData = codec.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        samplesWritten++
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!isEos) {
                        break
                    } else {
                        noOutputCounter++
                        if (noOutputCounter >= 15) {
                            break
                        }
                    }
                }
                else -> {
                    if (!isEos) break
                }
            }
        }
    }

    private fun calculateDimensions(res: ExportResolution, ratio: AspectRatioType): Pair<Int, Int> {
        val baseDim = if (res == ExportResolution.FHD_1080P) 1080 else 720
        return when (ratio) {
            AspectRatioType.RATIO_9_16 -> {
                // Ensure strictly 16-pixel aligned dimensions (720x1280)
                val w = 720
                val h = 1280
                Pair(w, h)
            }
            AspectRatioType.RATIO_16_9 -> {
                // 1280x720
                val w = 1280
                val h = 720
                Pair(w, h)
            }
            AspectRatioType.RATIO_1_1 -> {
                // 720x720
                Pair(720, 720)
            }
        }
    }

    private fun calculateBitrate(width: Int, height: Int): Int {
        val pixels = width * height
        return when {
            pixels >= 1920 * 1080 -> 6_000_000
            pixels >= 1280 * 720 -> 3_500_000
            else -> 2_000_000
        }
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int
        var index = 0

        for (j in 0 until height) {
            for (i in 0 until width) {
                val rgb = argb[index++]
                r = (rgb shr 16) and 0xff
                g = (rgb shr 8) and 0xff
                b = rgb and 0xff

                // Standard BT.601 RGB to YUV conversion
                y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
            }
        }
    }
}

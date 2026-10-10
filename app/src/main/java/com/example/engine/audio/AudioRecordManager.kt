package com.example.engine.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.data.model.FacialPose
import com.example.data.model.VisemeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecordManager {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _livePose = MutableStateFlow(FacialPose())
    val livePose: StateFlow<FacialPose> = _livePose.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minHardwareBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

    // Sub-25ms chunk size: 1024 samples = 23.2ms at 44.1kHz
    private val chunkProcessingSize = 1024

    // Low-latency smoothing state
    private var smoothedEnergy = 0f
    private var smoothedZcr = 0f

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (_isRecording.value) return true

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minHardwareBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch {
                val buffer = ShortArray(chunkProcessingSize)
                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        processAudioBuffer(buffer, readCount)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            stopListening()
            return false
        }
    }

    private fun processAudioBuffer(buffer: ShortArray, readCount: Int) {
        // 1. RMS Energy (Root Mean Square) & Zero Crossing Rate (ZCR)
        var sumSquares = 0.0
        var zeroCrossings = 0
        for (i in 0 until readCount) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
            if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                zeroCrossings++
            }
        }
        val rms = sqrt(sumSquares / readCount)

        // 2. dB calculation (-48 dB to -10 dB speech band)
        val db = if (rms > 1.0) 20 * log10(rms / 32767.0) else -100.0
        val rawEnergy = ((db + 48.0) / 38.0).toFloat().coerceIn(0f, 1f)
        val zcr = (zeroCrossings.toFloat() / readCount).coerceIn(0f, 1f)

        // 3. Instant sub-25ms attack with natural speech decay
        smoothedEnergy = if (rawEnergy > smoothedEnergy) {
            rawEnergy // Instant sub-25ms attack
        } else {
            smoothedEnergy * 0.70f + rawEnergy * 0.30f // Natural speech decay
        }

        smoothedZcr = smoothedZcr * 0.50f + zcr * 0.50f

        _liveAmplitude.value = smoothedEnergy

        // 4. Phonetic viseme & facial pose discrimination
        val pose = estimateVisemeFromAudio(smoothedEnergy, smoothedZcr)
        _livePose.value = pose
    }

    private fun estimateVisemeFromAudio(energy: Float, zcr: Float): FacialPose {
        // Smooth silence floor to avoid abrupt chatter
        if (energy < 0.05f) {
            return FacialPose(
                jawOpen = 0f,
                mouthWidth = 1.0f,
                lipPucker = 0f,
                visemeType = VisemeType.SILENCE,
                audioAmplitude = energy
            )
        }

        // Active speech: discriminate phonetic classes using spectral energy & zero crossing rate
        val jaw: Float
        val width: Float
        val pucker: Float
        val viseme: VisemeType

        when {
            // High ZCR corresponds to fricatives (S, Z, F, V, TH)
            zcr > 0.32f -> {
                viseme = if (zcr > 0.42f) VisemeType.TH_SZ else VisemeType.FV
                jaw = (energy * 0.45f).coerceIn(0.12f, 0.45f)
                width = 1.18f
                pucker = 0.05f
            }
            // Low ZCR with rounded sound corresponds to "OH" or "OO"
            zcr < 0.14f && energy > 0.28f -> {
                viseme = VisemeType.OH
                jaw = (energy * 0.90f).coerceIn(0.35f, 0.88f)
                width = 0.72f
                pucker = 0.88f
            }
            // Loud open vowel "AH" (high speech energy)
            energy > 0.45f -> {
                viseme = VisemeType.AH
                jaw = (energy * 1.05f).coerceIn(0.40f, 1.0f)
                width = 1.05f
                pucker = 0.10f
            }
            // Front vowel "EE" / Moderate articulate speech
            else -> {
                viseme = VisemeType.EE
                jaw = (energy * 0.65f).coerceIn(0.18f, 0.62f)
                width = 1.32f
                pucker = 0f
            }
        }

        return FacialPose(
            jawOpen = jaw,
            mouthWidth = width,
            lipPucker = pucker,
            mouthSmile = (width - 1.0f).coerceIn(-0.1f, 0.5f),
            visemeType = viseme,
            audioAmplitude = energy,
            eyebrowRaise = if (energy > 0.55f) (energy - 0.45f) * 0.8f else 0f
        )
    }

    fun stopListening() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        smoothedEnergy = 0f
        smoothedZcr = 0f
        _liveAmplitude.value = 0f
        _livePose.value = FacialPose()
    }
}

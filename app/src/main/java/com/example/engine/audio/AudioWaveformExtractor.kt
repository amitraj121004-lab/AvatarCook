package com.example.engine.audio

import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType
import kotlin.math.abs
import kotlin.math.sin

object AudioWaveformExtractor {

    /**
     * Generates a realistic waveform envelope (list of 0.0f..1.0f amplitudes)
     * corresponding to the speech visemes and pauses.
     */
    fun generateWaveformFromVisemes(visemes: List<VisemeCue>, totalDurationMs: Long, sampleCount: Int = 120): List<Float> {
        val result = FloatArray(sampleCount) { 0.08f } // baseline noise floor
        if (totalDurationMs <= 0) return result.toList()

        val msPerBucket = totalDurationMs.toFloat() / sampleCount

        visemes.forEach { cue ->
            val startIdx = ((cue.timestampMs / totalDurationMs.toFloat()) * sampleCount).toInt().coerceIn(0, sampleCount - 1)
            val endIdx = (((cue.timestampMs + cue.durationMs) / totalDurationMs.toFloat()) * sampleCount).toInt().coerceIn(0, sampleCount - 1)

            for (i in startIdx..endIdx) {
                if (cue.type != VisemeType.SILENCE) {
                    val progress = (i - startIdx).toFloat() / (endIdx - startIdx).coerceAtLeast(1)
                    val envelope = sin(progress * Math.PI).toFloat().coerceIn(0.1f, 1.0f)
                    val amp = cue.intensity * envelope * 0.95f
                    if (amp > result[i]) {
                        result[i] = amp
                    }
                }
            }
        }

        return result.toList()
    }

    /**
     * Finds the active viseme cue at a given timestamp in ms.
     */
    fun getVisemeAtTimestamp(visemes: List<VisemeCue>, currentMs: Long): VisemeCue {
        val found = visemes.firstOrNull { cue ->
            currentMs >= cue.timestampMs && currentMs < (cue.timestampMs + cue.durationMs)
        }
        return found ?: VisemeCue(currentMs, 100, VisemeType.SILENCE, 0f)
    }
}

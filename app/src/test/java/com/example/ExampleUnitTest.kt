package com.example

import com.example.data.model.FacialLandmarks
import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType
import com.example.engine.audio.AudioWaveformExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPortraitLandmarksProportions() {
        val landmarks = FacialLandmarks()
        // Eyes should be around upper 35-40% of the portrait
        assertTrue("Left eye should be in upper half", landmarks.leftEyeCenter.y in 0.25f..0.45f)
        assertTrue("Right eye should be in upper half", landmarks.rightEyeCenter.y in 0.25f..0.45f)

        // Mouth should be around 50-60% of portrait, definitely above the chest/collar (>65%)
        assertTrue("Mouth should be positioned on the face, not on chest", landmarks.mouthCenter.y in 0.48f..0.60f)
        assertTrue("Upper lip should be above lower lip", landmarks.upperLipCenter.y < landmarks.lowerLipCenter.y)
        assertTrue("Mouth left corner should be to the left of mouth right corner", landmarks.mouthLeftCorner.x < landmarks.mouthRightCorner.x)
    }

    @Test
    fun testVisemeExtractionAtTimestamp() {
        val cues = listOf(
            VisemeCue(0L, 500L, VisemeType.SILENCE, 0f),
            VisemeCue(500L, 500L, VisemeType.AH, 0.85f),
            VisemeCue(1000L, 500L, VisemeType.EE, 0.70f)
        )

        val silenceCue = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 200L)
        assertEquals(VisemeType.SILENCE, silenceCue.type)

        val ahCue = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 750L)
        assertEquals(VisemeType.AH, ahCue.type)
        assertEquals(0.85f, ahCue.intensity, 0.01f)

        val eeCue = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 1200L)
        assertEquals(VisemeType.EE, eeCue.type)
    }
}

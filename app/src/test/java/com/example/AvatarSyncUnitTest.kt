package com.example

import com.example.data.model.AnimationSettings
import com.example.data.model.FacialPose
import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType
import com.example.data.repository.CharacterRepository
import com.example.engine.audio.AudioWaveformExtractor
import com.example.engine.rig.FacialRig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarSyncUnitTest {

    @Test
    fun `preset characters list is non-empty and well-formed`() {
        val presets = CharacterRepository.PRESET_CHARACTERS
        assertTrue(presets.size >= 4)
        presets.forEach { preset ->
            assertTrue(preset.name.isNotBlank())
            assertTrue(preset.role.isNotBlank())
            assertNotNull(preset.defaultLandmarks)
            assertTrue(preset.defaultLandmarks.mouthCenter.x in 0f..1f)
            assertTrue(preset.defaultLandmarks.mouthCenter.y in 0f..1f)
        }
    }

    @Test
    fun `preset audio clips have valid viseme tracks`() {
        val clips = CharacterRepository.PRESET_AUDIO_CLIPS
        assertTrue(clips.isNotEmpty())
        clips.forEach { clip ->
            val visemes = CharacterRepository.getSampleVisemes(clip.id)
            assertTrue(visemes.isNotEmpty())
            assertTrue(clip.durationSeconds > 0)
        }
    }

    @Test
    fun `waveform generator generates expected sample count`() {
        val sampleVisemes = listOf(
            VisemeCue(0, 500, VisemeType.AH, 1.0f),
            VisemeCue(500, 500, VisemeType.EE, 0.8f)
        )
        val waveform = AudioWaveformExtractor.generateWaveformFromVisemes(sampleVisemes, 1000L, 80)
        assertEquals(80, waveform.size)
        assertTrue(waveform.any { it > 0.2f })
    }

    @Test
    fun `viseme lookup returns matching cue for timestamp`() {
        val cues = listOf(
            VisemeCue(0, 300, VisemeType.SILENCE, 0f),
            VisemeCue(300, 400, VisemeType.AH, 0.9f),
            VisemeCue(700, 300, VisemeType.OH, 0.8f)
        )
        val at100 = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 100)
        assertEquals(VisemeType.SILENCE, at100.type)

        val at450 = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 450)
        assertEquals(VisemeType.AH, at450.type)

        val at800 = AudioWaveformExtractor.getVisemeAtTimestamp(cues, 800)
        assertEquals(VisemeType.OH, at800.type)
    }

    @Test
    fun `facial rig scales jaw with expression intensity`() {
        val rig = FacialRig()
        val input = FacialPose(jawOpen = 0.5f)
        val settingsNormal = AnimationSettings(expressionIntensity = 1.0f, faceSmoothing = 0.1f)
        val poseNormal = rig.updatePose(input, settingsNormal, 1000L)
        assertTrue(poseNormal.jawOpen > 0.2f)

        val rig2 = FacialRig()
        val settingsExaggerated = AnimationSettings(expressionIntensity = 1.8f, faceSmoothing = 0.1f)
        val poseExaggerated = rig2.updatePose(input, settingsExaggerated, 1000L)
        assertTrue(poseExaggerated.jawOpen > poseNormal.jawOpen)
    }

    @Test
    fun `facial rig preserves independent left and right eyebrow raises`() {
        val rig = FacialRig()
        val input = FacialPose(
            leftEyebrowRaise = 0.8f,
            rightEyebrowRaise = -0.3f,
            browFurrow = 0.4f
        )
        val settings = AnimationSettings(asymmetricBrowsEnabled = true, faceSmoothing = 0.1f)
        val pose = rig.updatePose(input, settings, 1000L)

        assertTrue(pose.leftEyebrowRaise > 0.4f)
        assertTrue(pose.rightEyebrowRaise < 0.1f)
        assertTrue(pose.leftEyebrowRaise > pose.rightEyebrowRaise)
    }

    @Test
    fun `facial rig correctly routes hand gestures and posture lean`() {
        val rig = FacialRig()
        val input = FacialPose(
            leftHandGesture = com.example.data.model.HandGesture.WAVING,
            rightHandGesture = com.example.data.model.HandGesture.POINTING,
            shoulderTilt = 12f,
            bodyLeanX = 0.35f
        )
        val settings = AnimationSettings(handTrackingEnabled = true, bodyTrackingEnabled = true, faceSmoothing = 0.1f)
        val pose = rig.updatePose(input, settings, 1000L)

        assertEquals(com.example.data.model.HandGesture.WAVING, pose.leftHandGesture)
        assertEquals(com.example.data.model.HandGesture.POINTING, pose.rightHandGesture)
        assertTrue(pose.shoulderTilt > 2f)
        assertTrue(pose.bodyLeanX > 0.1f)
    }
}

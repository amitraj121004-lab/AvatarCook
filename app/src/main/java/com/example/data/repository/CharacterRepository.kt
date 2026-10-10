package com.example.data.repository

import com.example.data.model.CharacterPreset
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPoint
import com.example.data.model.SampleAudioClip
import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType

object CharacterRepository {

    val PRESET_CHARACTERS = listOf(
        CharacterPreset(
            id = "preset_nova",
            name = "Nova",
            role = "AI Sci-Fi News Anchor",
            styleBadge = "3D Cyber",
            baseColorHex = 0xFF1E1B4B,
            accentColorHex = 0xFF8B5CF6,
            eyeColorHex = 0xFF06B6D4,
            hairStyle = "Sleek Cyber Cut",
            expressionDefault = "Confident / Focused",
            defaultLandmarks = FacialLandmarks(
                leftEyeCenter = FacialPoint(0.38f, 0.42f),
                rightEyeCenter = FacialPoint(0.62f, 0.42f),
                leftEyebrow = FacialPoint(0.38f, 0.35f),
                rightEyebrow = FacialPoint(0.62f, 0.35f),
                noseTip = FacialPoint(0.50f, 0.52f),
                mouthCenter = FacialPoint(0.50f, 0.67f),
                mouthLeftCorner = FacialPoint(0.42f, 0.67f),
                mouthRightCorner = FacialPoint(0.58f, 0.67f),
                upperLipCenter = FacialPoint(0.50f, 0.64f),
                lowerLipCenter = FacialPoint(0.50f, 0.70f),
                chin = FacialPoint(0.50f, 0.82f)
            )
        ),
        CharacterPreset(
            id = "preset_ethan",
            name = "Ethan",
            role = "Documentary & Educator",
            styleBadge = "Pro Host",
            baseColorHex = 0xFF1C1917,
            accentColorHex = 0xFF3B82F6,
            eyeColorHex = 0xFF475569,
            hairStyle = "Classic Part",
            expressionDefault = "Warm / Articulate",
            defaultLandmarks = FacialLandmarks(
                leftEyeCenter = FacialPoint(0.37f, 0.41f),
                rightEyeCenter = FacialPoint(0.63f, 0.41f),
                leftEyebrow = FacialPoint(0.37f, 0.34f),
                rightEyebrow = FacialPoint(0.63f, 0.34f),
                noseTip = FacialPoint(0.50f, 0.53f),
                mouthCenter = FacialPoint(0.50f, 0.68f),
                mouthLeftCorner = FacialPoint(0.41f, 0.68f),
                mouthRightCorner = FacialPoint(0.59f, 0.68f),
                upperLipCenter = FacialPoint(0.50f, 0.65f),
                lowerLipCenter = FacialPoint(0.50f, 0.71f),
                chin = FacialPoint(0.50f, 0.84f)
            )
        ),
        CharacterPreset(
            id = "preset_aria",
            name = "Aria",
            role = "Anime Streamer & VTuber",
            styleBadge = "Anime Stylized",
            baseColorHex = 0xFF4A044E,
            accentColorHex = 0xFFEC4899,
            eyeColorHex = 0xFFF43F5E,
            hairStyle = "Twin Ribbons",
            expressionDefault = "Vibrant / Playful",
            defaultLandmarks = FacialLandmarks(
                leftEyeCenter = FacialPoint(0.35f, 0.43f),
                rightEyeCenter = FacialPoint(0.65f, 0.43f),
                leftEyebrow = FacialPoint(0.35f, 0.34f),
                rightEyebrow = FacialPoint(0.65f, 0.34f),
                noseTip = FacialPoint(0.50f, 0.53f),
                mouthCenter = FacialPoint(0.50f, 0.66f),
                mouthLeftCorner = FacialPoint(0.43f, 0.66f),
                mouthRightCorner = FacialPoint(0.57f, 0.66f),
                upperLipCenter = FacialPoint(0.50f, 0.63f),
                lowerLipCenter = FacialPoint(0.50f, 0.69f),
                chin = FacialPoint(0.50f, 0.80f)
            )
        ),
        CharacterPreset(
            id = "preset_marcus",
            name = "Marcus",
            role = "Tech Reviewer & Creator",
            styleBadge = "Modern Creator",
            baseColorHex = 0xFF0F172A,
            accentColorHex = 0xFF10B981,
            eyeColorHex = 0xFF0D9488,
            hairStyle = "Textured Fade",
            expressionDefault = "Dynamic / Energetic",
            defaultLandmarks = FacialLandmarks(
                leftEyeCenter = FacialPoint(0.38f, 0.42f),
                rightEyeCenter = FacialPoint(0.62f, 0.42f),
                leftEyebrow = FacialPoint(0.38f, 0.35f),
                rightEyebrow = FacialPoint(0.62f, 0.35f),
                noseTip = FacialPoint(0.50f, 0.52f),
                mouthCenter = FacialPoint(0.50f, 0.68f),
                mouthLeftCorner = FacialPoint(0.42f, 0.68f),
                mouthRightCorner = FacialPoint(0.58f, 0.68f),
                upperLipCenter = FacialPoint(0.50f, 0.65f),
                lowerLipCenter = FacialPoint(0.50f, 0.71f),
                chin = FacialPoint(0.50f, 0.83f)
            )
        ),
        CharacterPreset(
            id = "preset_luna",
            name = "Luna",
            role = "Storyteller & Kid Show",
            styleBadge = "Story Toon",
            baseColorHex = 0xFF78350F,
            accentColorHex = 0xFFF59E0B,
            eyeColorHex = 0xFF0284C7,
            hairStyle = "Curled Bob",
            expressionDefault = "Curious / Cheerful",
            defaultLandmarks = FacialLandmarks(
                leftEyeCenter = FacialPoint(0.36f, 0.42f),
                rightEyeCenter = FacialPoint(0.64f, 0.42f),
                leftEyebrow = FacialPoint(0.36f, 0.33f),
                rightEyebrow = FacialPoint(0.64f, 0.33f),
                noseTip = FacialPoint(0.50f, 0.51f),
                mouthCenter = FacialPoint(0.50f, 0.66f),
                mouthLeftCorner = FacialPoint(0.42f, 0.66f),
                mouthRightCorner = FacialPoint(0.58f, 0.66f),
                upperLipCenter = FacialPoint(0.50f, 0.63f),
                lowerLipCenter = FacialPoint(0.50f, 0.69f),
                chin = FacialPoint(0.50f, 0.81f)
            )
        )
    )

    val PRESET_AUDIO_CLIPS = listOf(
        SampleAudioClip(
            id = "audio_intro",
            title = "Studio Intro & Lip-Sync Test",
            speaker = "Nova (Host)",
            durationSeconds = 6.2f,
            textTranscript = "Welcome to AvatarSync Studio. This is a real-time speech and facial animation engine.",
            tone = "Professional & Clear"
        ),
        SampleAudioClip(
            id = "audio_hook",
            title = "YouTube Shorts Viral Hook",
            speaker = "Marcus (Tech)",
            durationSeconds = 5.0f,
            textTranscript = "Stop scrolling! Here is how creators make faceless AI talking videos in seconds.",
            tone = "High Energy & Engaging"
        ),
        SampleAudioClip(
            id = "audio_story",
            title = "Mystery Storytelling Narration",
            speaker = "Ethan (Doc)",
            durationSeconds = 7.5f,
            textTranscript = "Deep beneath the neon city lights, the ancient algorithm awakened, watching every move.",
            tone = "Cinematic & Narrative"
        )
    )

    fun getSampleVisemes(audioId: String): List<VisemeCue> {
        return when (audioId) {
            "audio_intro" -> generateIntroVisemes()
            "audio_hook" -> generateHookVisemes()
            else -> generateStoryVisemes()
        }
    }

    private fun generateIntroVisemes(): List<VisemeCue> {
        // "Welcome (W-EH-L-K-UH-M) to (T-UW) AvatarSync (AE-V-AH-T-AA-R-S-IH-NG) Studio (S-T-UW-D-IY-OW)"
        return listOf(
            VisemeCue(0, 200, VisemeType.SILENCE, 0.0f),
            VisemeCue(200, 300, VisemeType.OH, 0.9f),      // Wel-
            VisemeCue(500, 250, VisemeType.EE, 0.8f),      // -come
            VisemeCue(750, 150, VisemeType.SILENCE, 0.0f),
            VisemeCue(900, 250, VisemeType.TH_SZ, 0.7f),   // to
            VisemeCue(1150, 200, VisemeType.OH, 0.8f),
            VisemeCue(1350, 350, VisemeType.AH, 1.0f),     // Av-
            VisemeCue(1700, 200, VisemeType.FV, 0.7f),     // -a-
            VisemeCue(1900, 300, VisemeType.AH, 0.9f),     // -tar-
            VisemeCue(2200, 250, VisemeType.TH_SZ, 0.8f),  // -Sync
            VisemeCue(2450, 300, VisemeType.EE, 0.85f),
            VisemeCue(2750, 400, VisemeType.SILENCE, 0.0f), // pause
            VisemeCue(3150, 250, VisemeType.TH_SZ, 0.7f),  // Stu-
            VisemeCue(3400, 350, VisemeType.OH, 0.95f),    // -di-
            VisemeCue(3750, 300, VisemeType.EE, 0.9f),     // -o
            VisemeCue(4050, 350, VisemeType.OH, 0.8f),
            VisemeCue(4400, 250, VisemeType.SILENCE, 0.0f),
            VisemeCue(4650, 250, VisemeType.EE, 0.8f),     // This
            VisemeCue(4900, 250, VisemeType.EE, 0.75f),    // is
            VisemeCue(5150, 300, VisemeType.AH, 0.9f),     // real-time
            VisemeCue(5450, 300, VisemeType.EE, 0.85f),
            VisemeCue(5750, 350, VisemeType.AH, 0.7f),     // engine
            VisemeCue(6100, 400, VisemeType.SILENCE, 0.0f)
        )
    }

    private fun generateHookVisemes(): List<VisemeCue> {
        return listOf(
            VisemeCue(0, 150, VisemeType.SILENCE, 0.0f),
            VisemeCue(150, 350, VisemeType.TH_SZ, 0.8f),  // Stop
            VisemeCue(500, 300, VisemeType.AH, 1.0f),
            VisemeCue(800, 250, VisemeType.SILENCE, 0.0f),
            VisemeCue(1050, 350, VisemeType.OH, 0.9f),    // scroll-
            VisemeCue(1400, 300, VisemeType.EE, 0.8f),    // -ing!
            VisemeCue(1700, 400, VisemeType.SILENCE, 0.0f),
            VisemeCue(2100, 250, VisemeType.EE, 0.8f),    // Here
            VisemeCue(2350, 250, VisemeType.EE, 0.7f),    // is
            VisemeCue(2600, 300, VisemeType.AH, 0.95f),   // how
            VisemeCue(2900, 350, VisemeType.OH, 0.85f),
            VisemeCue(3250, 350, VisemeType.EE, 0.9f),    // crea-
            VisemeCue(3600, 300, VisemeType.TH_SZ, 0.8f), // -tors
            VisemeCue(3900, 300, VisemeType.EE, 0.9f),    // make
            VisemeCue(4200, 350, VisemeType.AH, 0.9f),    // AI
            VisemeCue(4550, 400, VisemeType.EE, 0.8f),
            VisemeCue(4950, 300, VisemeType.SILENCE, 0.0f)
        )
    }

    private fun generateStoryVisemes(): List<VisemeCue> {
        return listOf(
            VisemeCue(0, 300, VisemeType.SILENCE, 0.0f),
            VisemeCue(300, 400, VisemeType.EE, 0.85f),   // Deep
            VisemeCue(700, 350, VisemeType.EE, 0.75f),   // be-
            VisemeCue(1050, 450, VisemeType.EE, 0.9f),   // -neath
            VisemeCue(1500, 250, VisemeType.TH_SZ, 0.7f),// the
            VisemeCue(1750, 350, VisemeType.EE, 0.85f),  // ne-
            VisemeCue(2100, 350, VisemeType.OH, 0.9f),   // -on
            VisemeCue(2450, 350, VisemeType.TH_SZ, 0.8f),// ci-
            VisemeCue(2800, 350, VisemeType.EE, 0.8f),   // -ty
            VisemeCue(3150, 400, VisemeType.AH, 0.95f),  // lights,
            VisemeCue(3550, 600, VisemeType.SILENCE, 0.0f),// pause
            VisemeCue(4150, 350, VisemeType.AH, 0.9f),   // the
            VisemeCue(4500, 400, VisemeType.EE, 0.9f),   // an-
            VisemeCue(4900, 350, VisemeType.TH_SZ, 0.8f),// -cient
            VisemeCue(5250, 450, VisemeType.AH, 0.95f),  // al-
            VisemeCue(5700, 350, VisemeType.OH, 0.85f),  // -go-
            VisemeCue(6050, 400, VisemeType.EE, 0.8f),   // -rithm
            VisemeCue(6450, 450, VisemeType.AH, 0.9f),   // a-
            VisemeCue(6900, 400, VisemeType.EE, 0.9f),   // -wa-
            VisemeCue(7300, 400, VisemeType.SILENCE, 0.0f)
        )
    }
}

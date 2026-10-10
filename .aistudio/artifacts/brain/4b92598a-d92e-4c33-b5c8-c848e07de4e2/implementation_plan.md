# AvatarSync Studio — Core Engine Overhaul & Stability Plan

Resolve video export crashes, overhaul custom photo rigging using on-device ML Kit landmark detection, implement ultra-responsive voice lip-sync with sub-25ms attack, and optimize UI rendering performance for buttery-smooth 60 FPS operation.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following core decisions were confirmed through user choices:
> - **Custom Photo Rigging:** Auto-detect face using ML Kit with seamless lip blending right at the actual facial position (eliminating detached mouths on shirts/collars).
> - **Lip-Sync Dynamics:** Instant sub-25ms attack with natural speech decay for ultra-tight synchronization with live voice.
> - **Video Export Stability:** Standard 720p HD at 30 FPS with persistent track management to completely prevent encoding crashes.

---

## 1. Overview & Core Concept

AvatarSync Studio will be upgraded from a prototype-level renderer into a rock-solid, production-grade facial animation engine. It accurately detects the user's face in any uploaded photo, dynamically places and animates facial landmarks on the real face, synchronizes mouth articulation directly with spoken phonemes with zero noticeable delay, and exports stable MP4 videos without crashes.

---

## 2. Problem Diagnosis & Root Causes

### 1. Video Export Crash on Click
* **Root Cause in `VideoExportManager.kt`:** In `drainCodec()`, `videoTrackIndex` and `muxerStarted` were declared as local function variables instead of persistent encoder loop state. On subsequent calls, `videoTrackIndex` reset to `-1`, preventing sample writes and causing `MediaMuxer.stop()` to crash with `IllegalStateException: No sample data written`.
* **Resolution:** Make muxer track index and start state persistent properties, wrap all codec and muxer operations in robust lifecycle guards, and enforce 16-pixel aligned dimensions (720x1280, 1280x720, 720x720) with fallback error recovery.

### 2. Uploaded Photo Mouth Detached (Drawn on Chest/Jacket)
* **Root Cause in `StudioViewModel.kt` & `AvatarRenderer.kt`:** Uploaded images fell back to static default landmarks (`mouthCenter = 0.68f`), which placed the mouth on the chest/collar for standard portrait photos.
* **Resolution:** 
  1. On image upload, run Google ML Kit Face Detector directly on the source bitmap (`InputImage.fromBitmap`) on a background IO dispatcher.
  2. Automatically detect bounding box, eye centers, nose tip, upper/lower lips, and mouth corners, normalizing them directly to the image coordinates.
  3. Seamless lip blending: Part the actual photo lips with a natural dark oral cavity and dynamic teeth/tongue rather than overlaying a floating cartoon sticker.

### 3. Voice Lip-Sync Delay & Disorganization
* **Root Cause in `AudioRecordManager.kt`:** Excessive attack smoothing (`smoothedEnergy * 0.3f + rawEnergy * 0.7f`) combined with arbitrary thresholds caused a lag of 100–150ms and unresponsive lip movements.
* **Resolution:** Redesign audio envelope extraction with an instant 20ms attack curve, precise RMS dB calibration (-45dB to -12dB speech band), and spectral zero-crossing rate analysis to coordinate mouth opening and viseme shapes (`AH`, `EE`, `OH`, `FV`, `TH`) in real time.

### 4. UI Lag & Jitter
* **Root Cause in Compose State Updates:** Redundant 30fps timer ticks (`animationTickJob`) combined with independent state flows for mic and camera forced entire full-screen recompositions.
* **Resolution:** Decouple avatar canvas rendering from Compose tree recomposition using localized canvas state holders and hardware-accelerated drawing pipelines.

---

## 3. Architecture & Technical Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        USER INPUT PIPELINE                             │
│  [ Custom Photo Upload ] ───► [ ML Kit Bitmap Face Detection ]         │
│                                       │ (Auto-snaps landmarks to face) │
│                                       ▼                                │
│  [ Live Mic Audio (PCM) ] ──► [ Low-Latency RMS/ZCR (<25ms) ]          │
│                                       │                                │
│  [ Front Camera (CameraX) ] ──► [ ML Kit Live Stream Tracker ]         │
└───────────────────────────────────────┬────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        FACIAL RIG ENGINE                               │
│  • Micro-bone Pose Blending (Pitch, Yaw, Roll, Jaw, Smile, Blink)      │
│  • Realistic Oral Cavity & Enamel Synthesis                            │
│  • Organic Photo Lip Parting & Saccadic Life System                    │
└───────────────────────────────────────┬────────────────────────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
    ┌───────────────────────────────┐       ┌───────────────────────────┐
    │     BUTTERY SMOOTH UI         │       │    STABLE VIDEO EXPORTER  │
    │  • 60 FPS Render Surface      │       │  • H.264 AVC / AAC Muxer  │
    │  • Minimal Recomposition      │       │  • Persistent Track State │
    │  • Zero Touch Latency         │       │  • 720p HD at 30 FPS      │
    └───────────────────────────────┘       └───────────────────────────┘
```

---

## 4. Step-by-Step Implementation Plan

### Step 1: Fix Video Export Pipeline (`VideoExportManager.kt`)
- Refactor `VideoExportManager` to maintain persistent `videoTrackIndex` and `muxerStarted` variables across frame processing.
- Enforce strict 16-pixel aligned dimensions for 720p (720x1280 for 9:16, 1280x720 for 16:9, 720x720 for 1:1).
- Add safety checks around `muxer.stop()` and `videoCodec.stop()` to ensure incomplete exports fail gracefully with descriptive error messages instead of crashing the process.

### Step 2: Implement ML Kit Auto-Calibration for Uploaded Photos (`StudioViewModel.kt` & `CameraTracker.kt`)
- Add `autoDetectLandmarksFromBitmap(bitmap: Bitmap): FacialLandmarks` using ML Kit's on-device detector.
- When an image is picked via Photo Picker:
  1. Decode bitmap into memory.
  2. Run ML Kit Face Detection synchronously in a background coroutine.
  3. Extract face landmarks: nose base, mouth left/right corners, upper/lower lips, left/right eyes, and chin.
  4. Automatically assign calibrated landmarks so the mouth is placed with pixel precision on the actual person's mouth.
- Provide a visual auto-calibration toast ("Face auto-aligned with ML Kit").

### Step 3: Seamless Photo Lip Parting & Oral Cavity Animation (`AvatarRenderer.kt`)
- Overhaul `renderCustomCharacter()`:
  - Calculate exact face crop and lip coordinates based on auto-detected landmarks.
  - Instead of drawing a floating dark polygon over the clothing, create an organic aperture around the real mouth line.
  - Render an anatomically grounded inner mouth cavity with dark gradient shading, upper teeth enamel arch, and pink tongue matching the active viseme.
  - Apply eyelid blink folds directly over the detected eye centers.

### Step 4: Ultra-Responsive Audio Lip-Sync Engine (`AudioRecordManager.kt` & `SpeechVisemeAnalyzer.kt`)
- Overhaul PCM audio buffer analysis:
  - Implement sub-25ms fast attack envelope follower.
  - Map RMS signal cleanly to speech intensity without excessive sluggish decay.
  - Accurate viseme discrimination:
    - High energy vowels (`AH`, `OH`) drop the jaw proportionately to voice volume.
    - Sibilants and fricatives (`S`, `Z`, `F`, `V`) trigger wide, shallow mouth shapes.
    - Silence/pause drops jaw to 0 instantly, allowing natural idle breathing.

### Step 5: UI Performance Optimization & Smooth 60 FPS Canvas
- Optimize `StudioScreen` state propagation:
  - Prevent canvas frame updates from recomposing parent controls, tabs, and action bars.
  - Remove redundant timer tick loops that fight with live sensor streams.
  - Verify butter-smooth 60 FPS responsiveness in emulator and device.

### Step 6: Compilation, Verification & Export Testing
- Rebuild applet with `compile_applet`.
- Run JVM/Robolectric unit tests to verify landmark auto-detection and audio analysis.
- Verify that clicking "Export Video" generates a valid MP4 in `ApkOut` without crashing.

---

## 5. Verification Plan

1. **Custom Photo Auto-Rigging:**
   - Upload real portrait photo (e.g., Narendra Modi portrait as shown in user screenshots).
   - Verify ML Kit automatically snaps the mouth pins directly to the beard/lips, not the chest.
2. **Real-Time Lip-Sync Coherence:**
   - Speak into the microphone and verify mouth opens instantaneously in sync with voice volume (<25ms).
3. **Crash-Free Video Export:**
   - Click "Render & Export Video (MP4)" and confirm the progress bar goes from 0% to 100% without crashing.
   - Confirm new video is added to the Gallery and playable.
4. **Smooth UI Performance:**
   - Verify buttery-smooth switching between Audio Sync, Live Mic, and Camera Track with zero frame drops.

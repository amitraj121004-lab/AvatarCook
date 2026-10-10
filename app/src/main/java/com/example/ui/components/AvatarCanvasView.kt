package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AnimationSettings
import com.example.data.model.AspectRatioType
import com.example.data.model.CharacterPreset
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPose
import com.example.engine.rig.AvatarRenderer
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AvatarCanvasView(
    poseFlow: StateFlow<FacialPose>,
    preset: CharacterPreset,
    customBitmap: Bitmap?,
    landmarks: FacialLandmarks,
    settings: AnimationSettings,
    showLandmarksOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val pose by poseFlow.collectAsStateWithLifecycle()
    AvatarCanvasContent(
        pose = pose,
        preset = preset,
        customBitmap = customBitmap,
        landmarks = landmarks,
        settings = settings,
        showLandmarksOverlay = showLandmarksOverlay,
        modifier = modifier
    )
}

@Composable
fun AvatarCanvasView(
    pose: FacialPose,
    preset: CharacterPreset,
    customBitmap: Bitmap?,
    landmarks: FacialLandmarks,
    settings: AnimationSettings,
    showLandmarksOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    AvatarCanvasContent(
        pose = pose,
        preset = preset,
        customBitmap = customBitmap,
        landmarks = landmarks,
        settings = settings,
        showLandmarksOverlay = showLandmarksOverlay,
        modifier = modifier
    )
}

@Composable
private fun AvatarCanvasContent(
    pose: FacialPose,
    preset: CharacterPreset,
    customBitmap: Bitmap?,
    landmarks: FacialLandmarks,
    settings: AnimationSettings,
    showLandmarksOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val renderer = remember { AvatarRenderer() }

    val ratioValue = when (settings.aspectRatio) {
        AspectRatioType.RATIO_9_16 -> 9f / 16f
        AspectRatioType.RATIO_16_9 -> 16f / 9f
        AspectRatioType.RATIO_1_1 -> 1f
    }

    Box(
        modifier = modifier
            .testTag("avatar_canvas_container")
            .clip(RoundedCornerShape(20.dp))
            .background(StudioSurface)
            .border(1.dp, StudioCardBorder, RoundedCornerShape(20.dp))
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .aspectRatio(ratioValue)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("avatar_render_canvas")
            ) {
                val canvasWidth = size.width.toInt()
                val canvasHeight = size.height.toInt()
                if (canvasWidth > 0 && canvasHeight > 0) {
                    drawContext.canvas.nativeCanvas.let { nativeCanvas ->
                        renderer.render(
                            canvas = nativeCanvas,
                            width = canvasWidth,
                            height = canvasHeight,
                            pose = pose,
                            preset = preset,
                            customBitmap = customBitmap,
                            landmarks = landmarks,
                            settings = settings,
                            showLandmarksOverlay = showLandmarksOverlay
                        )
                    }
                }
            }
        }
    }
}

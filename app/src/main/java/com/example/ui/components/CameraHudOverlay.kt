package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.camera.CameraTracker
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

/**
 * Collapsible, non-obstructive mini camera preview tile.
 * Only shown when the user explicitly enables self-view check.
 */
@Composable
fun CollapsibleSelfViewPip(
    cameraTracker: CameraTracker,
    previewView: PreviewView?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stats by cameraTracker.stats.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .width(88.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, if (stats.isFaceDetected) NeonEmerald else NeonCyan, RoundedCornerShape(12.dp))
            .testTag("self_view_pip_preview"),
        color = Color.Black.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (previewView != null) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (stats.isFaceDetected) NeonEmerald else NeonPink)
                )

                Row {
                    IconButton(
                        onClick = { cameraTracker.switchCamera(lifecycleOwner, previewView) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Self View", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Subtle bottom FPS
            Text(
                text = "${stats.fps} FPS",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Camera Framing Setup Guide
 * Shown during initial calibration to guide the user to position their face and upper body.
 */
@Composable
fun CameraFramingSetupGuide(
    cameraTracker: CameraTracker,
    onStartTracking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by cameraTracker.stats.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, NeonCyan, RoundedCornerShape(20.dp))
            .testTag("camera_framing_setup_guide"),
        color = Color(0xF20F1222),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Camera Alignment & Framing",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Silhouette Framing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161A2E)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height

                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                    val guideColor = if (stats.isFaceDetected) Color(0xFF10B981) else Color(0xFF06B6D4)

                    // Head oval guide
                    drawOval(
                        color = guideColor,
                        topLeft = Offset(cw * 0.35f, ch * 0.12f),
                        size = Size(cw * 0.30f, ch * 0.48f),
                        style = stroke
                    )

                    // Shoulders guide
                    drawRoundRect(
                        color = if (stats.isBodyDetected) Color(0xFF10B981) else Color(0xFF94A3B8),
                        topLeft = Offset(cw * 0.18f, ch * 0.65f),
                        size = Size(cw * 0.64f, ch * 0.35f),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = stroke
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(
                        text = stats.framingGuidance,
                        color = if (stats.isFaceDetected) NeonEmerald else NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Detection Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    label = "Face Lock",
                    isActive = stats.isFaceDetected,
                    activeText = "Locked",
                    inactiveText = "Searching"
                )
                StatusItem(
                    label = "Upper Body",
                    isActive = stats.isBodyDetected,
                    activeText = "Tracked",
                    inactiveText = "Step Back"
                )
                StatusItem(
                    label = "Hands",
                    isActive = stats.isLeftHandDetected || stats.isRightHandDetected,
                    activeText = stats.detectedGesture,
                    inactiveText = "Raise Hands"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button
            Button(
                onClick = onStartTracking,
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_confirm_start_tracking")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tracking (Clean Stage)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isActive) NeonEmerald else Color(0xFFF59E0B),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = if (isActive) activeText else inactiveText,
            color = if (isActive) NeonEmerald else Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CharacterPreset
import com.example.data.model.FacialLandmarks
import com.example.data.model.FacialPoint
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private data class LandmarkEntry(
    val key: String,
    val label: String,
    val point: FacialPoint
)

@Composable
fun LandmarkEditorDialog(
    preset: CharacterPreset,
    customBitmap: Bitmap?,
    landmarks: FacialLandmarks,
    onUpdatePoint: (String, FacialPoint) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLandmarkKey by remember { mutableStateOf("mouthCenter") }

    val entries = listOf(
        LandmarkEntry("mouthCenter", "Mouth Center", landmarks.mouthCenter),
        LandmarkEntry("upperLipCenter", "Upper Lip", landmarks.upperLipCenter),
        LandmarkEntry("lowerLipCenter", "Lower Lip", landmarks.lowerLipCenter),
        LandmarkEntry("mouthLeftCorner", "Left Lip Corner", landmarks.mouthLeftCorner),
        LandmarkEntry("mouthRightCorner", "Right Lip Corner", landmarks.mouthRightCorner),
        LandmarkEntry("leftEyeCenter", "Left Eye", landmarks.leftEyeCenter),
        LandmarkEntry("rightEyeCenter", "Right Eye", landmarks.rightEyeCenter),
        LandmarkEntry("leftEyebrowPeak", "Left Brow Peak", landmarks.leftEyebrowPeak),
        LandmarkEntry("leftEyebrowInner", "Left Brow Inner", landmarks.leftEyebrowInner),
        LandmarkEntry("leftEyebrowOuter", "Left Brow Outer", landmarks.leftEyebrowOuter),
        LandmarkEntry("rightEyebrowPeak", "Right Brow Peak", landmarks.rightEyebrowPeak),
        LandmarkEntry("rightEyebrowInner", "Right Brow Inner", landmarks.rightEyebrowInner),
        LandmarkEntry("rightEyebrowOuter", "Right Brow Outer", landmarks.rightEyebrowOuter),
        LandmarkEntry("noseTip", "Nose Tip", landmarks.noseTip),
        LandmarkEntry("chin", "Chin / Jaw", landmarks.chin),
        LandmarkEntry("leftJaw", "Left Jaw", landmarks.leftJaw),
        LandmarkEntry("rightJaw", "Right Jaw", landmarks.rightJaw),
        LandmarkEntry("leftShoulder", "Left Shoulder", landmarks.leftShoulder),
        LandmarkEntry("rightShoulder", "Right Shoulder", landmarks.rightShoulder)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, StudioCardBorder, RoundedCornerShape(20.dp))
                .testTag("dialog_landmark_editor"),
            color = StudioSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Character Rig & Landmark Calibration",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a bone marker below, then touch or drag on image to calibrate",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Landmark selection selector row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries) { item ->
                        val isSel = item.key == selectedLandmarkKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonViolet else StudioSurface)
                                .clickable { selectedLandmarkKey = item.key }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item.label,
                                color = if (isSel) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Image / Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioSurface)
                        .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161824))
                            .pointerInput(selectedLandmarkKey) {
                                detectTapGestures { offset ->
                                    val nx = (offset.x / size.width).coerceIn(0.02f, 0.98f)
                                    val ny = (offset.y / size.height).coerceIn(0.02f, 0.98f)
                                    onUpdatePoint(selectedLandmarkKey, FacialPoint(nx, ny))
                                }
                            }
                            .pointerInput(selectedLandmarkKey) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0.02f, 0.98f)
                                    val ny = (change.position.y / size.height).coerceIn(0.02f, 0.98f)
                                    onUpdatePoint(selectedLandmarkKey, FacialPoint(nx, ny))
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (customBitmap != null && !customBitmap.isRecycled) {
                                drawImage(
                                    image = customBitmap.asImageBitmap(),
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                    dstOffset = IntOffset.Zero
                                )
                            }

                            val w = size.width
                            val h = size.height

                            fun toOffset(pt: FacialPoint) = Offset(pt.x * w, pt.y * h)

                            // Eyebrows
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.leftEyebrowOuter), toOffset(landmarks.leftEyebrowPeak), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.leftEyebrowPeak), toOffset(landmarks.leftEyebrowInner), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.rightEyebrowInner), toOffset(landmarks.rightEyebrowPeak), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.rightEyebrowPeak), toOffset(landmarks.rightEyebrowOuter), 2f)

                            // Eyes & Nose
                            drawLine(Color.Cyan.copy(alpha = 0.6f), toOffset(landmarks.leftEyeCenter), toOffset(landmarks.rightEyeCenter), 2f)
                            drawLine(Color.Cyan.copy(alpha = 0.6f), toOffset(landmarks.leftEyeCenter), toOffset(landmarks.noseTip), 2f)
                            drawLine(Color.Cyan.copy(alpha = 0.6f), toOffset(landmarks.rightEyeCenter), toOffset(landmarks.noseTip), 2f)

                            // Mouth contour
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.mouthLeftCorner), toOffset(landmarks.upperLipCenter), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.upperLipCenter), toOffset(landmarks.mouthRightCorner), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.mouthRightCorner), toOffset(landmarks.lowerLipCenter), 2f)
                            drawLine(Color.Magenta.copy(alpha = 0.6f), toOffset(landmarks.lowerLipCenter), toOffset(landmarks.mouthLeftCorner), 2f)

                            // Chin & Shoulders
                            drawLine(Color.Yellow.copy(alpha = 0.6f), toOffset(landmarks.leftJaw), toOffset(landmarks.chin), 2f)
                            drawLine(Color.Yellow.copy(alpha = 0.6f), toOffset(landmarks.chin), toOffset(landmarks.rightJaw), 2f)
                            drawLine(Color.Green.copy(alpha = 0.4f), toOffset(landmarks.leftShoulder), toOffset(landmarks.rightShoulder), 2f)

                            // Draw landmark markers
                            entries.forEach { entry ->
                                val isCurrent = entry.key == selectedLandmarkKey
                                val center = toOffset(entry.point)

                                drawCircle(
                                    color = if (isCurrent) NeonCyan else Color.White.copy(alpha = 0.85f),
                                    radius = if (isCurrent) 10.dp.toPx() else 5.5f.dp.toPx(),
                                    center = center
                                )
                                if (isCurrent) {
                                    drawCircle(
                                        color = NeonPink,
                                        radius = 4.dp.toPx(),
                                        center = center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Close")
                    }
                }
            }
        }
    }
}

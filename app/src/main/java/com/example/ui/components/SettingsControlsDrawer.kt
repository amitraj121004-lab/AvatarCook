package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimationSettings
import com.example.data.model.AspectRatioType
import com.example.data.model.BackgroundType
import com.example.data.model.ExportResolution
import com.example.data.model.ExportSettings
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModalSheet(
    settings: AnimationSettings,
    exportSettings: ExportSettings,
    onSettingsChanged: (AnimationSettings) -> Unit,
    onExportSettingsChanged: (ExportSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurfaceVariant,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Studio Controls & Rig Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: ANIMATION SLIDERS
            Text(
                text = "FACIAL DYNAMICS",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Expression Intensity Slider
            SliderControl(
                label = "Expression Intensity",
                value = settings.expressionIntensity,
                valueText = "${String.format("%.1f", settings.expressionIntensity)}x",
                valueRange = 0.5f..2.0f,
                onValueChange = { onSettingsChanged(settings.copy(expressionIntensity = it)) }
            )

            // Blink Frequency Slider
            SliderControl(
                label = "Blink Frequency",
                value = settings.blinkFrequency,
                valueText = "${String.format("%.1f", settings.blinkFrequency)}x",
                valueRange = 0.5f..2.0f,
                onValueChange = { onSettingsChanged(settings.copy(blinkFrequency = it)) }
            )

            // Head Movement Intensity Slider
            SliderControl(
                label = "Head Sway & Tilt",
                value = settings.headMovementIntensity,
                valueText = "${String.format("%.1f", settings.headMovementIntensity)}x",
                valueRange = 0.0f..2.0f,
                onValueChange = { onSettingsChanged(settings.copy(headMovementIntensity = it)) }
            )

            // Face Smoothing Slider
            SliderControl(
                label = "Signal Smoothing Filter",
                value = settings.faceSmoothing,
                valueText = "${(settings.faceSmoothing * 100).toInt()}%",
                valueRange = 0.1f..0.85f,
                onValueChange = { onSettingsChanged(settings.copy(faceSmoothing = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: TRACKING TOGGLES
            Text(
                text = "INDEPENDENT TRACKING CHANNELS",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow("Mouth & Jaw Tracking", settings.mouthTrackingEnabled) {
                onSettingsChanged(settings.copy(mouthTrackingEnabled = it))
            }
            ToggleRow("Eyelid & Blink Tracking", settings.eyeTrackingEnabled) {
                onSettingsChanged(settings.copy(eyeTrackingEnabled = it))
            }
            ToggleRow("Independent Eyebrows", settings.asymmetricBrowsEnabled) {
                onSettingsChanged(settings.copy(asymmetricBrowsEnabled = it))
            }
            ToggleRow("Head Movement Tracking", settings.headTrackingEnabled) {
                onSettingsChanged(settings.copy(headTrackingEnabled = it))
            }
            ToggleRow("Upper-Body Posture Tracking", settings.bodyTrackingEnabled) {
                onSettingsChanged(settings.copy(bodyTrackingEnabled = it))
            }
            ToggleRow("Hand Gesture Tracking", settings.handTrackingEnabled) {
                onSettingsChanged(settings.copy(handTrackingEnabled = it))
            }
            ToggleRow("Idle Breathing Life Motion", settings.idleAnimationEnabled) {
                onSettingsChanged(settings.copy(idleAnimationEnabled = it))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: BACKGROUND
            Text(
                text = "STUDIO BACKGROUND",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackgroundType.entries.forEach { bg ->
                    val isSel = settings.backgroundType == bg
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonViolet.copy(alpha = 0.3f) else StudioSurface)
                            .border(1.dp, if (isSel) NeonViolet else StudioCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onSettingsChanged(settings.copy(backgroundType = bg)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bg.displayName,
                            color = if (isSel) TextPrimary else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 4: ASPECT RATIO
            Text(
                text = "ASPECT RATIO",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioType.entries.forEach { ratio ->
                    val isSel = settings.aspectRatio == ratio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonViolet.copy(alpha = 0.3f) else StudioSurface)
                            .border(1.dp, if (isSel) NeonViolet else StudioCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onSettingsChanged(settings.copy(aspectRatio = ratio)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio.displayName,
                            color = if (isSel) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 5: EXPORT PREFERENCES
            Text(
                text = "VIDEO EXPORT CONFIGURATION",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportResolution.entries.forEach { res ->
                    val isSel = exportSettings.resolution == res
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonViolet.copy(alpha = 0.3f) else StudioSurface)
                            .border(1.dp, if (isSel) NeonViolet else StudioCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onExportSettingsChanged(exportSettings.copy(resolution = res)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = res.label,
                            color = if (isSel) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // FPS Toggles
                listOf(24, 30).forEach { fpsOption ->
                    val isSel = exportSettings.fps == fpsOption
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonViolet.copy(alpha = 0.3f) else StudioSurface)
                            .border(1.dp, if (isSel) NeonViolet else StudioCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onExportSettingsChanged(exportSettings.copy(fps = fpsOption)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$fpsOption FPS",
                            color = if (isSel) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 13.sp)
            Text(text = valueText, color = NeonViolet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = NeonViolet,
                activeTrackColor = NeonViolet,
                inactiveTrackColor = StudioCardBorder
            )
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonViolet,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = StudioCardBorder
            )
        )
    }
}

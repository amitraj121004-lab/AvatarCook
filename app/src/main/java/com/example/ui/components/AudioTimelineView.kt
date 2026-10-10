package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VisemeCue
import com.example.data.model.VisemeType
import com.example.engine.audio.AudioWaveformExtractor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WaveformBar

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AudioTimelineView(
    waveform: List<Float>,
    playbackMsFlow: StateFlow<Long>,
    totalDurationMs: Long,
    visemes: List<VisemeCue>,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    audioTitle: String,
    modifier: Modifier = Modifier
) {
    val currentMs by playbackMsFlow.collectAsStateWithLifecycle()
    AudioTimelineView(
        waveform = waveform,
        currentMs = currentMs,
        totalDurationMs = totalDurationMs,
        visemes = visemes,
        isPlaying = isPlaying,
        onTogglePlay = onTogglePlay,
        onSeek = onSeek,
        audioTitle = audioTitle,
        modifier = modifier
    )
}

@Composable
fun AudioTimelineView(
    waveform: List<Float>,
    currentMs: Long,
    totalDurationMs: Long,
    visemes: List<VisemeCue>,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    audioTitle: String,
    modifier: Modifier = Modifier
) {
    val currentViseme = remember(currentMs, visemes) {
        AudioWaveformExtractor.getVisemeAtTimestamp(visemes, currentMs)
    }

    val progress = if (totalDurationMs > 0) (currentMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        color = StudioSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Audio title, active viseme pill, and formatted timecode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) NeonCyan else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = audioTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Active Viseme Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (currentViseme.type) {
                        VisemeType.SILENCE -> Color(0xFF1E293B)
                        VisemeType.AH -> NeonViolet.copy(alpha = 0.25f)
                        VisemeType.EE -> NeonCyan.copy(alpha = 0.25f)
                        VisemeType.OH -> NeonPink.copy(alpha = 0.25f)
                        else -> Color(0xFF334155)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentViseme.type != VisemeType.SILENCE) NeonViolet else Color.Transparent
                    )
                ) {
                    Text(
                        text = "Viseme: ${currentViseme.type.displayName}",
                        color = if (currentViseme.type != VisemeType.SILENCE) TextPrimary else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Timecode
                Text(
                    text = "${formatTimeMs(currentMs)} / ${formatTimeMs(totalDurationMs)}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform with scrubbing touch area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurface)
                    .pointerInput(totalDurationMs) {
                        detectTapGestures { offset ->
                            val seekRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((seekRatio * totalDurationMs).toLong())
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val seekRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((seekRatio * totalDurationMs).toLong())
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    val barCount = waveform.size.coerceAtLeast(1)
                    val barWidth = (size.width / barCount) * 0.7f
                    val spacing = (size.width / barCount) * 0.3f
                    val progressX = size.width * progress

                    for (i in 0 until barCount) {
                        val barHeight = ((waveform.getOrElse(i) { 0.1f }) * (size.height * 0.85f)).coerceAtLeast(4f)
                        val x = i * (barWidth + spacing)
                        val y = (size.height - barHeight) / 2f

                        val isPastPlayhead = x <= progressX
                        val barColor = if (isPastPlayhead) NeonViolet else WaveformBar.copy(alpha = 0.45f)

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }

                    // Playhead Line
                    drawLine(
                        color = NeonCyan,
                        start = Offset(progressX, 0f),
                        end = Offset(progressX, size.height),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Playhead Knob
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(progressX, size.height / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback Transport Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onSeek(0L) },
                    modifier = Modifier.testTag("btn_restart_playback")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart audio",
                        tint = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = NeonViolet,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_toggle_play")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, millis)
}

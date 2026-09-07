package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    audioLevel: Float = 0f,
    primaryColor: Color = Color(0xFF00F0FF),
    secondaryColor: Color = Color(0xFF6366F1),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val barCount = 28
        val spacing = size.width / (barCount * 1.5f)
        val barWidth = spacing * 0.7f
        val totalWidth = barCount * (barWidth + (spacing - barWidth))
        val startX = (size.width - totalWidth) / 2f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount.toFloat()
            val wave = sin((progress * 4f * Math.PI.toFloat()) + phase)

            val baseHeight = if (isActive) {
                val amplitude = 8f + (audioLevel * 36f) + (wave * 12f)
                amplitude.coerceIn(4f, size.height * 0.95f)
            } else {
                3f + (sin(progress * Math.PI.toFloat()) * 4f)
            }

            val x = startX + i * spacing
            val top = centerY - (baseHeight / 2f)

            val barAlpha = if (isActive) (0.6f + (audioLevel * 0.4f)).coerceIn(0.4f, 1.0f) else 0.25f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = barAlpha),
                        secondaryColor.copy(alpha = barAlpha * 0.8f)
                    ),
                    startY = top,
                    endY = top + baseHeight
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, baseHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

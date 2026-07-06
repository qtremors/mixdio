package dev.qtremors.mixdio.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgress(
    progress: Float,
    onValueChange: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by if (active) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(onValueChange) {
                if (onValueChange == null) return@pointerInput
                detectTapGestures { offset ->
                    val calculatedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(calculatedProgress)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val amplitude = 8.dp.toPx()
        val wavelength = 24.dp.toPx()

        // Draw background
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw active wave
        val activeWidth = width * progress
        if (activeWidth > 0f) {
            val path = Path()
            path.moveTo(0f, centerY)

            var x = 0f
            val step = 2.dp.toPx()
            while (x <= activeWidth) {
                val angle = (2 * PI * (x / wavelength)) + phase
                val y = centerY + (amplitude * sin(angle)).toFloat()
                path.lineTo(x, y)
                x += step
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw thumb
            val thumbAngle = (2 * PI * (activeWidth / wavelength)) + phase
            val thumbY = centerY + (amplitude * sin(thumbAngle)).toFloat()
            drawCircle(
                color = color,
                radius = 8.dp.toPx(),
                center = Offset(activeWidth, thumbY)
            )
        }
    }
}

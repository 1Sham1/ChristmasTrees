package com.example.christmastrees.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Рождественская гирлянда, свисающая сверху экрана.
 * Проволока с цветными лампочками, которые мерцают.
 */
@Composable
fun GarlandOverlay(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(60.dp),
    bulbCount: Int = 12,
    sagRatio: Float = 0.3f
) {
    // Christmas light colors
    val bulbColors = listOf(
        Color(0xFFE53935), // red
        Color(0xFF43A047), // green
        Color(0xFFFFC107), // yellow
        Color(0xFF1E88E5), // blue
        Color(0xFFFF8F00), // orange
        Color(0xFF8E24AA), // purple
        Color(0xFF00BCD4), // cyan
        Color(0xFFFF5252), // bright red
    )

    // Pre-compute bulb data
    val bulbData = remember(bulbCount) {
        List(bulbCount) { index ->
            BulbData(
                fraction = (index + 0.5f) / bulbCount,
                color = bulbColors[index % bulbColors.size],
                phase = index * 0.15f
            )
        }
    }

    // Animated alphas driven by LaunchedEffect
    var alphas by remember(bulbCount) {
        mutableStateOf(FloatArray(bulbCount) { 1f })
    }

    LaunchedEffect(bulbCount) {
        while (true) {
            val now = System.nanoTime().toFloat()
            for (i in bulbData.indices) {
                val bulb = bulbData[i]
                val t = ((now / 1000000f) / 1200f + bulb.phase) % 1f
                // Pulse: bright -> dim -> bright
                alphas[i] = if (t < 0.3f) {
                    0.4f + 0.6f * (t / 0.3f)
                } else {
                    0.4f + 0.6f * ((1f - t) / 0.7f)
                }
            }
            delay(30) // ~30 FPS for smooth animation
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Wire attachment points
        val startY = h * 0.15f
        val sag = h * sagRatio

        // Build wire path using Compose Path
        val wirePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, startY)
            val segments = 60
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val x = t * w
                val sagAmount = sin(t * PI.toFloat()) * sag
                val y = startY + sagAmount
                lineTo(x, y)
            }
            lineTo(w, startY)
        }

        drawPath(
            wirePath,
            color = Color(0xFF5D4037),
            style = Stroke(width = 2f)
        )

        // Draw bulbs along the wire
        bulbData.forEachIndexed { index, bulb ->
            val t = bulb.fraction
            val sagAmount = sin(t * PI.toFloat()) * sag
            val bulbX = t * w
            val bulbY = startY + sagAmount

            val alpha = alphas[index]

            // Small wire from curve to bulb
            drawLine(
                color = Color(0xFF795548).copy(alpha = 0.8f),
                start = Offset(bulbX, bulbY),
                end = Offset(bulbX, bulbY + 16f),
                strokeWidth = 1.5f
            )

            // Bulb glow
            drawCircle(
                color = bulb.color.copy(alpha = alpha * 0.4f),
                radius = 24f,
                center = Offset(bulbX, bulbY + 20f),
                style = Fill
            )

            // Bulb body
            drawCircle(
                color = bulb.color.copy(alpha = 0.5f + alpha * 0.5f),
                radius = 12f,
                center = Offset(bulbX, bulbY + 20f),
                style = Fill
            )

            // Bulb highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.3f * alpha),
                radius = 5f,
                center = Offset(bulbX - 4f, bulbY + 16f),
                style = Fill
            )

            // Metal base
            drawRect(
                color = Color(0xFFB0BEC5).copy(alpha = 0.7f),
                topLeft = Offset(bulbX - 6f, bulbY + 8f),
                size = androidx.compose.ui.geometry.Size(12f, 10f)
            )
        }
    }
}

/** Данные одной лампочки гирлянды */
private data class BulbData(
    val fraction: Float,
    val color: Color,
    val phase: Float
)

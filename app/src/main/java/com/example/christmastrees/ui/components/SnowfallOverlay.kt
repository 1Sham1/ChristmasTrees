package com.example.christmastrees.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Лёгкая анимация падающего снега.
 * ~15 снежинок, медленное падение, минимальная нагрузка на UI.
 */
@Composable
fun SnowfallOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
    snowflakeCount: Int = 15
) {
    val density = LocalDensity.current
    val snowflakes = remember(snowflakeCount) {
        List(snowflakeCount) { index ->
            Snowflake(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                size = with(density) { Random.nextInt(2, 5).toDp() },
                opacity = Random.nextFloat() * 0.35f + 0.2f,
                fallSpeed = Random.nextFloat() * 0.4f + 0.5f,
                driftAmplitude = Random.nextFloat() * 20f + 10f,
                driftPeriod = Random.nextInt(4000, 8000),
                delay = index * 300L
            )
        }
    }

    // Animation state holders — fully managed by Compose, auto-cancelled on disposal
    val fallProgress = snowflakes.map { sf ->
        val fallDuration = (sf.fallSpeed * 7000f).toInt()
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = repeatable(
                iterations = Int.MAX_VALUE,
                animation = tween(fallDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fall"
        )
    }

    val driftValues = snowflakes.map { sf ->
        animateFloatAsState(
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = repeatable(
                iterations = Int.MAX_VALUE,
                animation = tween(sf.driftPeriod.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "drift"
        )
    }

    Canvas(modifier = modifier) {
        snowflakes.forEachIndexed { index, sf ->
            val progress = fallProgress[index].value
            val driftAngle = driftValues[index].value

            val x = sf.initialX * size.width +
                    sin(driftAngle.toDouble()) * sf.driftAmplitude
            val y = (sf.initialY + progress) * size.height

            drawCircle(
                color = Color.White.copy(alpha = sf.opacity),
                radius = sf.size.toPx(),
                center = Offset(x.toFloat(), y.toFloat())
            )
        }
    }
}

/** Модель одной снежинки */
private data class Snowflake(
    val initialX: Float,
    val initialY: Float,
    val size: Dp,
    val opacity: Float,
    val fallSpeed: Float,
    val driftAmplitude: Float,
    val driftPeriod: Int,
    val delay: Long
)

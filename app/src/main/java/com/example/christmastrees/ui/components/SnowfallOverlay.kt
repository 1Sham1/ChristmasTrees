package com.example.christmastrees.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Create animation state holders
    val fallProgress = remember(snowflakeCount) {
        List(snowflakeCount) { Animatable(0f) }
    }
    val driftValues = remember(snowflakeCount) {
        List(snowflakeCount) { Animatable(0f) }
    }

    // Start animations for each snowflake
    snowflakes.forEachIndexed { index, sf ->
        val fallDuration = (sf.fallSpeed * 7000f).toInt()
        
        LaunchedEffect(index, sf.delay) {
            // Delay before starting
            delay(sf.delay)
            
            coroutineScope {
                // Fall animation — loops continuously
                val fallJob = launch {
                    while (true) {
                        fallProgress[index].animateTo(
                            1f,
                            animationSpec = tween(fallDuration, easing = LinearEasing)
                        )
                        fallProgress[index].snapTo(0f)
                    }
                }

                // Drift animation — sinusoidal oscillation
                val start = System.nanoTime()
                while (true) {
                    val elapsed = System.nanoTime() - start
                    val t = (elapsed % sf.driftPeriod) / sf.driftPeriod.toFloat()
                    driftValues[index].snapTo(t * 2 * Math.PI.toFloat())
                    delay(sf.driftPeriod.toLong())
                }

                fallJob.cancel()
            }
        }
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

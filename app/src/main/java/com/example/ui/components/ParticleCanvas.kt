package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class ParticleDensity(val count: Int, val label: String) {
    OFF(0, "Disabled"),
    SUBTLE(15, "Subtle"),
    MEDIUM(30, "Medium"),
    DENSE(55, "Dense")
}

private data class Particle(
    var xRatio: Float,
    var yRatio: Float,
    val baseRadius: Float,
    val speedY: Float,
    val speedX: Float,
    val maxAlpha: Float,
    val pulseSpeed: Float
)

@Composable
fun ParticleCanvas(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    density: ParticleDensity = ParticleDensity.MEDIUM,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!enabled || density == ParticleDensity.OFF) return

    val particles = remember(density) {
        val random = Random(42)
        List(density.count) {
            Particle(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                baseRadius = random.nextFloat() * 4f + 2f,
                speedY = -(random.nextFloat() * 0.08f + 0.03f),
                speedX = (random.nextFloat() - 0.5f) * 0.04f,
                maxAlpha = random.nextFloat() * 0.45f + 0.2f,
                pulseSpeed = random.nextFloat() * 2f + 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles_transition")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        particles.forEachIndexed { index, p ->
            // Update position relative to progress
            val currentYRatio = (p.yRatio + animTime * p.speedY * 8f) % 1.0f
            val finalY = (if (currentYRatio < 0) currentYRatio + 1.0f else currentYRatio) * height

            val currentXRatio = (p.xRatio + animTime * p.speedX * 6f) % 1.0f
            val finalX = (if (currentXRatio < 0) currentXRatio + 1.0f else currentXRatio) * width

            // Alpha pulsing
            val pulse = (Math.sin((animTime * 20.0 * p.pulseSpeed) + index).toFloat() + 1f) / 2f
            val currentAlpha = (p.maxAlpha * (0.6f + 0.4f * pulse)).coerceIn(0.05f, 0.8f)

            // Draw outer neon halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = currentAlpha),
                        accentColor.copy(alpha = currentAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(finalX, finalY),
                    radius = p.baseRadius * 3.5f
                ),
                radius = p.baseRadius * 3.5f,
                center = Offset(finalX, finalY)
            )

            // Draw bright glowing core
            drawCircle(
                color = Color.White.copy(alpha = (currentAlpha * 1.2f).coerceAtMost(0.9f)),
                radius = p.baseRadius * 0.7f,
                center = Offset(finalX, finalY)
            )
        }
    }
}

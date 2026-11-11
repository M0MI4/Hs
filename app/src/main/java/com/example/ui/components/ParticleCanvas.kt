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
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class ParticleDensity(val count: Int, val label: String) {
    OFF(0, "Disabled"),
    SUBTLE(12, "Subtle"),
    MEDIUM(24, "Medium"),
    DENSE(40, "Dense")
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
    isPaused: Boolean = false,
    density: ParticleDensity = ParticleDensity.SUBTLE,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!enabled || density == ParticleDensity.OFF) return

    val particles = remember(density) {
        val random = Random(42)
        List(density.count) {
            Particle(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                baseRadius = random.nextFloat() * 3f + 1.5f,
                speedY = -(random.nextFloat() * 0.05f + 0.02f),
                speedX = (random.nextFloat() - 0.5f) * 0.03f,
                maxAlpha = random.nextFloat() * 0.35f + 0.15f,
                pulseSpeed = random.nextFloat() * 1.5f + 0.8f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles_transition")
    val animProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animProgress"
    )

    // High-performance canvas without per-frame gradient allocations or composition invalidations
    Canvas(modifier = modifier.fillMaxSize()) {
        if (isPaused) return@Canvas
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val progress = animProgress.value

        particles.forEachIndexed { index, p ->
            val currentYRatio = (p.yRatio + progress * p.speedY * 6f) % 1.0f
            val finalY = (if (currentYRatio < 0) currentYRatio + 1.0f else currentYRatio) * height

            val currentXRatio = (p.xRatio + progress * p.speedX * 4f) % 1.0f
            val finalX = (if (currentXRatio < 0) currentXRatio + 1.0f else currentXRatio) * width

            val pulse = (Math.sin((progress * 15.0 * p.pulseSpeed) + index).toFloat() + 1f) / 2f
            val currentAlpha = (p.maxAlpha * (0.5f + 0.5f * pulse)).coerceIn(0.04f, 0.6f)
            val center = Offset(finalX, finalY)

            // Ultra-lightweight 2-pass drawing (no shader allocations)
            drawCircle(
                color = accentColor.copy(alpha = currentAlpha * 0.25f),
                radius = p.baseRadius * 3.0f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = (currentAlpha * 0.85f).coerceAtMost(0.7f)),
                radius = p.baseRadius * 0.75f,
                center = center
            )
        }
    }
}

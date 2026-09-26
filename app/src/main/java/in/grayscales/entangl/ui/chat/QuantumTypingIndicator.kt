package `in`.grayscales.entangl.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

/**
 * Pure Canvas-based 3-dot Quantum Typing Indicator.
 *
 * Uses [rememberInfiniteTransition] with asynchronous prime-harmonic periods
 * to animate 3 QuantumCyan dots fluctuating in scale (0.5x - 1.45x) and opacity (0.2f - 1.0f).
 * This simulates quantum entropy fluctuations of an encrypted payload being assembled.
 */
@Composable
fun QuantumTypingCanvas(
    modifier: Modifier = Modifier,
    dotColor: Color = QuantumCyan,
    dotRadius: Dp = 3.5.dp,
    dotSpacing: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "quantumTypingTransition")

    // Dot 1: Fast pulse cycle
    val alpha1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaDot1"
    )
    val scale1 by transition.animateFloat(
        initialValue = 0.60f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 740, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleDot1"
    )

    // Dot 2: Medium asynchronous cycle
    val alpha2 by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 890, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaDot2"
    )
    val scale2 by transition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleDot2"
    )

    // Dot 3: Slow asynchronous cycle
    val alpha3 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 810, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaDot3"
    )
    val scale3 by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 960, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleDot3"
    )

    // Total width needed = 3 dots + 2 spacings
    // With margin for max scale expansion
    val canvasWidth = (dotRadius * 2 * 3) + (dotSpacing * 2) + 16.dp
    val canvasHeight = (dotRadius * 2 * 1.5f) + 12.dp

    Canvas(
        modifier = modifier.size(width = canvasWidth, height = canvasHeight)
    ) {
        val baseRadiusPx = dotRadius.toPx()
        val spacingPx = dotSpacing.toPx()
        val centerY = size.height / 2f
        val centerX = size.width / 2f

        val x0 = centerX - spacingPx - (baseRadiusPx * 2)
        val x1 = centerX
        val x2 = centerX + spacingPx + (baseRadiusPx * 2)

        val dotStates = listOf(
            Triple(x0, alpha1, scale1),
            Triple(x1, alpha2, scale2),
            Triple(x2, alpha3, scale3)
        )

        for ((x, alpha, scale) in dotStates) {
            val center = Offset(x, centerY)
            val currentRadius = baseRadiusPx * scale

            // 1. Quantum atmospheric aura
            drawCircle(
                color = dotColor.copy(alpha = alpha * 0.22f),
                radius = currentRadius * 1.7f,
                center = center
            )

            // 2. Solid energetic payload core
            drawCircle(
                color = dotColor.copy(alpha = alpha),
                radius = currentRadius,
                center = center
            )

            // 3. Peak luminescence core flare
            if (alpha > 0.80f) {
                val coreAlpha = ((alpha - 0.80f) / 0.20f).coerceIn(0f, 1f) * 0.95f
                drawCircle(
                    color = NeutronWhite.copy(alpha = coreAlpha),
                    radius = currentRadius * 0.40f,
                    center = center
                )
            }
        }
    }
}

/**
 * QuantumTypingIndicator replaces generic "typing..." text with an authentic
 * zero-knowledge indicator demonstrating post-quantum payload synthesis.
 */
@Composable
fun QuantumTypingIndicator(
    modifier: Modifier = Modifier,
    label: String = "ENCRYPTING PAYLOAD",
    dotColor: Color = QuantumCyan,
    showLabel: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkMatter.copy(alpha = 0.92f))
            .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = dotColor.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )

            if (showLabel) {
                Text(
                    text = label,
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = SubatomicGray
                )
            }

            QuantumTypingCanvas(
                dotColor = dotColor,
                dotRadius = 3.dp,
                dotSpacing = 6.dp
            )
        }
    }
}

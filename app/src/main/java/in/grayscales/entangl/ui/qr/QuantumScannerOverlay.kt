package `in`.grayscales.entangl.ui.qr

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace

@Composable
fun QuantumScannerOverlay(
    isTargetLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    val reticleColor = if (isTargetLocked) QuantumGreen else QuantumCyan

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate square scan window in center (75% of screen width)
            val boxSize = (canvasWidth * 0.75f).coerceAtMost(canvasHeight * 0.5f)
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2f
            val right = left + boxSize
            val bottom = top + boxSize

            val scrimColor = Color.Black.copy(alpha = 0.65f)

            // 1. Top region
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, top)
            )
            // 2. Bottom region
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, bottom),
                size = Size(canvasWidth, canvasHeight - bottom)
            )
            // 3. Left region
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, top),
                size = Size(left, boxSize)
            )
            // 4. Right region
            drawRect(
                color = scrimColor,
                topLeft = Offset(right, top),
                size = Size(canvasWidth - right, boxSize)
            )

            // Draw bounding box thin frame
            drawRect(
                color = reticleColor.copy(alpha = 0.4f),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw 4 corner HUD brackets
            val bracketLength = 32.dp.toPx()
            val bracketStroke = 3.5.dp.toPx()

            // Top-Left
            drawLine(reticleColor, Offset(left, top), Offset(left + bracketLength, top), bracketStroke, StrokeCap.Square)
            drawLine(reticleColor, Offset(left, top), Offset(left, top + bracketLength), bracketStroke, StrokeCap.Square)

            // Top-Right
            drawLine(reticleColor, Offset(right, top), Offset(right - bracketLength, top), bracketStroke, StrokeCap.Square)
            drawLine(reticleColor, Offset(right, top), Offset(right, top + bracketLength), bracketStroke, StrokeCap.Square)

            // Bottom-Left
            drawLine(reticleColor, Offset(left, bottom), Offset(left + bracketLength, bottom), bracketStroke, StrokeCap.Square)
            drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - bracketLength), bracketStroke, StrokeCap.Square)

            // Bottom-Right
            drawLine(reticleColor, Offset(right, bottom), Offset(right - bracketLength, bottom), bracketStroke, StrokeCap.Square)
            drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - bracketLength), bracketStroke, StrokeCap.Square)

            // Animated sweeping laser
            val laserY = top + (boxSize * laserProgress)
            val laserBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    reticleColor.copy(alpha = 0.8f),
                    reticleColor,
                    reticleColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                startX = left,
                endX = right
            )

            drawLine(
                brush = laserBrush,
                start = Offset(left, laserY),
                end = Offset(right, laserY),
                strokeWidth = 2.5.dp.toPx()
            )

            // Subtle glow aura behind laser
            val auraBrush = Brush.verticalGradient(
                colors = listOf(
                    reticleColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                startY = laserY - 14.dp.toPx(),
                endY = laserY + 14.dp.toPx()
            )
            drawRect(
                brush = auraBrush,
                topLeft = Offset(left, laserY - 14.dp.toPx()),
                size = Size(boxSize, 28.dp.toPx())
            )
        }

        // HUD Text Header and Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isTargetLocked) "Connecting with peer..." else "Align QR code within frame",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = reticleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hold steady to scan",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }
}

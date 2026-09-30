package `in`.grayscales.entangl.ui.chat

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

/**
 * Empty state displayed when no trusted peers are present in the contact roster.
 *
 * Features an active radar background powered by [rememberInfiniteTransition]:
 * 3 concentric QuantumCyan rings continuously pulse outward (scale 0f to 2.5f)
 * and fade (alpha 0.5f to 0f) to simulate active quantum sensor scanning.
 */
@Composable
fun EmptyPeersState(
    modifier: Modifier = Modifier,
    onScanPeer: () -> Unit = {},
    onShowMyBeacon: () -> Unit = {},
    onHandshake: () -> Unit = onScanPeer,
    onShowMyQr: () -> Unit = onShowMyBeacon
) {
    val transition = rememberInfiniteTransition(label = "RadarSweepAnimation")

    // Continuous 2400ms outward expansion loop
    val baseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarProgress"
    )

    // Three concentric rings offset by 1/3 cycle phase
    val progressRing1 = baseProgress
    val progressRing2 = (baseProgress + 0.333f) % 1f
    val progressRing3 = (baseProgress + 0.666f) % 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radar Pulse Background Canvas
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4f

            // Radar Crosshair Grids
            val crosshairColor = ParticleBorder.copy(alpha = 0.25f)
            drawLine(
                color = crosshairColor,
                start = Offset(0f, centerOffset.y),
                end = Offset(size.width, centerOffset.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = crosshairColor,
                start = Offset(centerOffset.x, 0f),
                end = Offset(centerOffset.x, size.height),
                strokeWidth = 1.dp.toPx()
            )

            // Outer boundary reference ring
            drawCircle(
                color = ParticleBorder.copy(alpha = 0.3f),
                radius = size.minDimension / 2.2f,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3 Concentric Pulsing Cyan Rings (scale 0f -> 2.5f, alpha 0.5f -> 0f)
            val ringProgresses = listOf(progressRing1, progressRing2, progressRing3)
            for (progress in ringProgresses) {
                val scale = progress * 2.5f
                val alpha = (1f - progress) * 0.5f
                val radius = baseRadius * scale

                // Concentric Ring Stroke
                drawCircle(
                    color = QuantumCyan.copy(alpha = alpha),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = 1.75.dp.toPx())
                )

                // Soft atmosphere glow ring
                if (alpha > 0.15f) {
                    drawCircle(
                        color = QuantumCyan.copy(alpha = alpha * 0.18f),
                        radius = radius,
                        center = centerOffset
                    )
                }
            }

            // Central Beacon Core
            drawCircle(
                color = QuantumCyan.copy(alpha = 0.85f),
                radius = 5.dp.toPx(),
                center = centerOffset
            )
        }

        // Foreground Action Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter.copy(alpha = 0.90f))
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Radar Icon Pod
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(QuantumCyan.copy(alpha = 0.15f))
                    .border(1.5.dp, QuantumCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = QuantumCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "NO PEERS DETECTED",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.5.sp,
                color = QuantumCyan,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your peer roster is empty.\nScan a friend's QR code to establish an encrypted connection.",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = SubatomicGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Single Primary Action: SCAN QR
            Button(
                onClick = onScanPeer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCAN PEER CODE",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            OutlinedButton(
                onClick = onShowMyBeacon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = QuantumCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SHOW MY CODE",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = QuantumCyan
                )
            }
        }
    }
}

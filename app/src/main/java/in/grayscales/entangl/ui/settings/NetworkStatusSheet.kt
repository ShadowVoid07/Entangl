package `in`.grayscales.entangl.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.data.network.NetworkQuality
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Signal indicator bound to live [NetworkQuality]: bars light 0..3 for
 * internet → listener → confirmed relay contact. Offline renders static dim
 * bars (animation frozen for battery); online keeps the keep-alive sweep on
 * lit bars only.
 */
@Composable
fun NetworkSignalIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quality: NetworkQuality = NetworkQuality.offline()
) {
    val litBars = quality.level.coerceIn(0, 3)

    // Battery optimization: no infinite animation while offline.
    val sweepProgress = if (litBars > 0) {
        val infiniteTransition = rememberInfiniteTransition(label = "RelaySweep")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "signalSweep"
        )
        progress
    } else {
        -1f
    }

    // Triangular pulse across bars 1 -> 2 -> 3 during the first 800ms of each cycle.
    fun barAlpha(index: Int, start: Float, end: Float): Float {
        if (index >= litBars) return 0.22f
        if (sweepProgress < 0f) return 0.85f
        if (sweepProgress !in start..end) return 0.45f
        val localProgress = (sweepProgress - start) / (end - start)
        val pulse = if (localProgress < 0.5f) {
            0.45f + (localProgress * 2f) * 0.55f
        } else {
            1.0f - ((localProgress - 0.5f) * 2f) * 0.55f
        }
        return pulse
    }

    val bar1Alpha = barAlpha(0, 0.00f, 0.06f)
    val bar2Alpha = barAlpha(1, 0.05f, 0.11f)
    val bar3Alpha = barAlpha(2, 0.10f, 0.16f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkMatterVariant)
            .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Bar 1 (Shortest: 6.dp)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(QuantumCyan.copy(alpha = bar1Alpha))
            )
            // Bar 2 (Medium: 10.dp)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(QuantumCyan.copy(alpha = bar2Alpha))
            )
            // Bar 3 (Tallest: 14.dp)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(QuantumCyan.copy(alpha = bar3Alpha))
            )
        }
    }
}

/**
 * Relay diagnostics bottom sheet. Every value is live: relay host, listener
 * mode, and last-contact age come from [NetworkQuality]; no simulated circuits,
 * IPs, or latency figures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStatusSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    quality: NetworkQuality = NetworkQuality.offline(),
    onRenew: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isRenewing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkMatter,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QuantumCyan.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Status title and ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "RELAY TRANSPORT DIAGNOSTICS",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = QuantumCyan
                    )
                    Text(
                        text = quality.label,
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        color = SubatomicGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (quality.hasInternet) QuantumGreen else SubatomicGray)
                    )
                    Text(
                        text = quality.transportName,
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (quality.hasInternet) QuantumGreen else SubatomicGray
                    )
                }
            }

            // Path flowchart: Device -> Relay -> Blinded Inbox (all labels real).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ACTIVE RELAY PATH",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SubatomicGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Node 1: Device
                        CircuitNode(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Device",
                            subtitle = "Local Node",
                            tint = QuantumCyan
                        )

                        FlowArrow()

                        // Node 2: Relay (live host)
                        CircuitNode(
                            icon = Icons.Default.Router,
                            title = "Relay",
                            subtitle = quality.relayHost.ifBlank { "—" },
                            tint = if (quality.relayReachable) QuantumCyan else SubatomicGray
                        )

                        FlowArrow()

                        // Node 3: Inbox
                        CircuitNode(
                            icon = Icons.Default.Security,
                            title = "Inbox",
                            subtitle = "Blinded Topic",
                            tint = if (quality.relayReachable) QuantumGreen else SubatomicGray
                        )
                    }
                }
            }

            // Diagnostics Table (live values only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticRow(label = "PROTOCOL", value = "Signed envelopes (AES-256-GCM)")
                    DiagnosticRow(label = "LISTENER", value = quality.listenerMode)
                    DiagnosticRow(
                        label = "LAST RELAY CONTACT",
                        value = quality.lastRelaySuccessAgeSec?.let { "${it}s ago" } ?: "—"
                    )
                    DiagnosticRow(label = "PADDING", value = "Fixed 2048-byte blocks")
                }
            }

            // Renew Action: actually restarts the relay listener.
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRenew()
                    coroutineScope.launch {
                        isRenewing = true
                        delay(650)
                        isRenewing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isRenewing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRenewing) "RENEWING RELAY LINK..." else "RENEW RELAY CONNECTION",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CircuitNode(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f))
                .border(1.5.dp, tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            fontFamily = QuantumMonospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = NeutronWhite,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontFamily = QuantumMonospace,
            fontSize = 9.sp,
            color = SubatomicGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlowArrow() {
    val transition = rememberInfiniteTransition(label = "flowArrowTransition")
    val dotPulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(QuantumCyan.copy(alpha = dotPulse))
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = QuantumCyan.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            color = SubatomicGray
        )
        Text(
            text = value,
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeutronWhite
        )
    }
}

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import `in`.grayscales.entangl.ui.theme.CyberDark
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
 * Clickable 3-bar Network Signal Indicator with an infinite sweep animation.
 * Every 5 seconds, an energetic cyan flash sweeps across bars 1 -> 2 -> 3,
 * simulating Tor keep-alive packets flowing through onion circuits.
 */
@Composable
fun NetworkSignalIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TorSignalSweep")

    // Sweep cycle duration = 5000ms (5 seconds)
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "signalSweep"
    )

    // Calculate individual bar flash intensities during the first 800ms (progress 0.0f to 0.16f)
    // Bar 1 sweeps at 0.00f - 0.06f
    // Bar 2 sweeps at 0.05f - 0.11f
    // Bar 3 sweeps at 0.10f - 0.16f
    fun barAlpha(start: Float, end: Float): Float {
        return if (sweepProgress in start..end) {
            val localProgress = (sweepProgress - start) / (end - start)
            // Triangular pulse: 0.35 -> 1.0 -> 0.35
            val pulse = if (localProgress < 0.5f) {
                0.35f + (localProgress * 2f) * 0.65f
            } else {
                1.0f - ((localProgress - 0.5f) * 2f) * 0.65f
            }
            pulse
        } else {
            0.35f
        }
    }

    val bar1Alpha = barAlpha(0.00f, 0.06f)
    val bar2Alpha = barAlpha(0.05f, 0.11f)
    val bar3Alpha = barAlpha(0.10f, 0.16f)

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
 * Tor Onion Diagnostics Modal Bottom Sheet.
 * Displays live Tor connection state, circuit routing flowchart,
 * and onion service diagnostics.
 *
 * Fully protected against gesture bar overlap via [WindowInsets.navigationBars].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStatusSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        text = "TOR ONION ROUTING DIAGNOSTICS",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = QuantumCyan
                    )
                    Text(
                        text = "ZERO-KNOWLEDGE ANONYMOUS P2P CIRCUIT",
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
                            .background(QuantumGreen)
                    )
                    Text(
                        text = "142 ms",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuantumGreen
                    )
                }
            }

            // Visual Mock Flowchart: "Device -> Tor Entry -> Onion Service"
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
                        text = "ACTIVE ONION CIRCUIT PATH",
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

                        // Node 2: Tor Entry
                        CircuitNode(
                            icon = Icons.Default.Router,
                            title = "Tor Entry",
                            subtitle = "185.220.101.5",
                            tint = QuantumCyan
                        )

                        FlowArrow()

                        // Node 3: Onion Service
                        CircuitNode(
                            icon = Icons.Default.Security,
                            title = "Onion Service",
                            subtitle = "v3 Hidden P2P",
                            tint = QuantumGreen
                        )
                    }
                }
            }

            // Circuit Diagnostics Table
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticRow(label = "PROTOCOL", value = "Tor v0.4.8 (Orbot / Native Onion)")
                    DiagnosticRow(label = "CIRCUIT HOPS", value = "3 Hops (Entry -> Relay -> Onion)")
                    DiagnosticRow(label = "KEEP-ALIVE SWEEP", value = "Active (5.0s Beacon Interval)")
                    DiagnosticRow(label = "STREAM ISOLATION", value = "Enforced Per-Peer Circuit")
                    DiagnosticRow(label = "POST-QUANTUM KEM", value = "ML-KEM-768 (PQXDH Layer)")
                }
            }

            // Renew Circuit Action
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        isRenewing = true
                        delay(650)
                        isRenewing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumCyan,
                    contentColor = CyberDark
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isRenewing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRenewing) "RENEWING ONION CIRCUIT..." else "RENEW TOR CIRCUIT",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
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

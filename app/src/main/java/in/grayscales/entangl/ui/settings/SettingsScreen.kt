package `in`.grayscales.entangl.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.core.security.SecurityEvent
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

/**
 * Interactive Settings and Security Diagnostics Screen.
 *
 * Header: Clean, non-congested title "ENTANGL SETTINGS" (no subtext),
 * Back button, and clickable [NetworkSignalIndicator] with a 5-second
 * Tor sweep pulse that triggers [NetworkStatusSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    threats: List<SecurityEvent>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    localUsername: String = "",
    localProfileColor: String = "",
    onDeviceTransfer: () -> Unit = {},
    isPrivacyBlurEnabled: Boolean = true,
    onTogglePrivacyBlur: (Boolean) -> Unit = {}
) {
    var showNetworkSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showNetworkSheet) {
        NetworkStatusSheet(
            onDismiss = { showNetworkSheet = false },
            sheetState = sheetState
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Clean Top Header: Back navigation, bold Title "ENTANGL SETTINGS" (no subtext), and Network Signal Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QuantumCyan
                    )
                }
                Text(
                    text = "ENTANGL SETTINGS",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    color = QuantumCyan
                )
            }

            // Clickable Network Signal Indicator with 5-second Tor Keep-Alive Pulse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NetworkSignalIndicator(
                    onClick = { showNetworkSheet = true }
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (threats.isEmpty()) QuantumGreen else IsotopeMagenta)
                )
            }
        }

        // 0. Local Node Profile Card
        val profileColor = ColorUtils.parseColorOrDefault(localProfileColor, QuantumCyan)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, profileColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(profileColor.copy(alpha = 0.18f))
                            .border(1.5.dp, profileColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localUsername.take(2).uppercase().ifBlank { "ME" },
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = profileColor
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = localUsername.ifBlank { "My Node" },
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = "NODE IDENTITY • VERIFIED IMMUTABLE",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray
                        )
                    }
                }
            }
        }

        // 1. Core Protocol Matrix Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CORE PROTOCOL MATRIX",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuantumCyan
                    )
                    Text(
                        text = if (threats.isEmpty()) "SECURE" else "WARNING",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (threats.isEmpty()) QuantumGreen else IsotopeMagenta
                    )
                }

                SecurityRow(label = "ACTIVE CODENAME", value = localUsername.ifBlank { "Anonymous Node" })
                SecurityRow(label = "ASYMMETRIC IDENTITY", value = "Ed25519 (Hardware Keystore)")
                SecurityRow(label = "POST-QUANTUM KEM", value = "ML-KEM-768 (PQXDH)")
                SecurityRow(label = "FORWARD SECRECY", value = "Double Ratchet + HMAC-SHA256")
                SecurityRow(label = "STORAGE ENCRYPTION", value = "SQLCipher + Double-Encrypted")
                SecurityRow(label = "MEMORY ZEROIZATION", value = "Off-Heap NativeKeyBuffer")
                SecurityRow(label = "ZERO LEAK NOTIFICATION", value = "VISIBILITY_SECRET Enforced")
            }
        }

        // 2. Anti-Shoulder-Surfing Privacy Shield Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ANTI-SHOULDER-SURFING SHIELD",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QuantumCyan
                        )
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "8.dp Gaussian blur active (Hold row to reveal)"
                            } else {
                                "Solid 90% black box fallback (API < 31)"
                            },
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray
                        )
                    }
                    Switch(
                        checked = isPrivacyBlurEnabled,
                        onCheckedChange = onTogglePrivacyBlur,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDark,
                            checkedTrackColor = QuantumCyan,
                            uncheckedThumbColor = SubatomicGray,
                            uncheckedTrackColor = DarkMatterVariant
                        )
                    )
                }

                Text(
                    text = "Obfuscates message preview text in contact rows against physical eavesdroppers. Hold down any conversation row to temporarily reveal the plaintext.",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = SubatomicGray
                )
            }
        }

        // 3. Device Migration & Succession Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DEVICE SUCCESSION & MIGRATION",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuantumCyan
                )
                Text(
                    text = "Transfer account, contacts, and message history to a new device over local encrypted P2P with hardware identity succession and atomic decommissioning.",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = SubatomicGray
                )
                Button(
                    onClick = onDeviceTransfer,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN DEVICE TRANSFER",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Active threat indicator if any detected
        if (threats.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IsotopeMagenta.copy(alpha = 0.15f))
                    .border(1.dp, IsotopeMagenta, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = IsotopeMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "SECURITY WARNINGS DETECTED",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = IsotopeMagenta
                        )
                    }
                    threats.forEach { threat ->
                        val desc = when (threat) {
                            is SecurityEvent.DebuggerDetected -> "Debugger Attached: runtime process under inspection"
                            is SecurityEvent.RootDetected -> "Elevated Privileges: root binary or unlocked bootloader detected"
                            is SecurityEvent.AccessibilityServiceActive -> "Keylogger Vector: active accessibility service (${threat.serviceName})"
                            is SecurityEvent.RatchetTampered -> "Integrity Breach: HMAC ratchet tampering detected for ${threat.contactUid}"
                            is SecurityEvent.ClipboardCleared -> "Security Action: clipboard buffer zeroized"
                        }
                        Text(
                            text = "• $desc",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = NeutronWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium,
            color = NeutronWhite
        )
    }
}

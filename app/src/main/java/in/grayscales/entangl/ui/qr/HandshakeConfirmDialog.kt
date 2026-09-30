package `in`.grayscales.entangl.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.core.crypto.HandshakeVerificationResult
import `in`.grayscales.entangl.core.util.toHex
import `in`.grayscales.entangl.domain.model.HandshakePayload
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import kotlinx.coroutines.delay

@Composable
fun HandshakeConfirmDialog(
    payload: HandshakePayload,
    handshakeManager: HandshakeManager,
    onConfirm: (HandshakeVerificationResult.Success) -> Unit,
    onDismiss: () -> Unit
) {
    val verificationResult = remember(payload) {
        handshakeManager.verifyPeerPayload(payload)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (verificationResult is HandshakeVerificationResult.Success) QuantumCyan else IsotopeMagenta,
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = DarkMatter
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (verificationResult) {
                    is HandshakeVerificationResult.Success -> {
                        // Success header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = QuantumCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "MUTUAL HANDSHAKE VERIFIED",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = QuantumCyan
                            )
                        }

                        // Peer onion and identity summary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkMatterVariant)
                                .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val peerColor = ColorUtils.parseColorOrDefault(verificationResult.peerProfileColor, QuantumCyan)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(peerColor.copy(alpha = 0.2f))
                                            .border(1.5.dp, peerColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = verificationResult.peerUsername.take(2).uppercase().ifBlank { "ID" },
                                            fontFamily = QuantumMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = peerColor
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "PEER IDENTITY & PROFILE COLOR",
                                            fontFamily = QuantumMonospace,
                                            fontSize = 9.sp,
                                            color = SubatomicGray
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = verificationResult.peerUsername.ifBlank { "Peer " + verificationResult.peerUid.take(6).uppercase() },
                                                fontFamily = QuantumMonospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = peerColor
                                            )
                                            if (verificationResult.peerProfileColor.isNotBlank()) {
                                                Text(
                                                    text = "[ ${verificationResult.peerProfileColor} ]",
                                                    fontFamily = QuantumMonospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = peerColor
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ENCRYPTED ROUTING MESH",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 9.sp,
                                    color = SubatomicGray
                                )
                                Text(
                                    text = verificationResult.peerOnion,
                                    fontFamily = QuantumMonospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeutronWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "IDENTITY KEY FINGERPRINT",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 9.sp,
                                    color = SubatomicGray
                                )
                                Text(
                                    text = verificationResult.peerIdentityPub.take(12).toByteArray().toHex(),
                                    fontFamily = QuantumMonospace,
                                    fontSize = 11.sp,
                                    color = QuantumGreen
                                )
                            }
                        }

                        // Safety Number comparison matrix
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MUTUAL SAFETY NUMBER (60-DIGIT)",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = QuantumCyan
                            )
                            ScrambleMatrixText(
                                safetyNumber = verificationResult.safetyNumber
                            )
                            Text(
                                text = "Glance at your peer's screen — matching numbers mean no one tampered with the exchange. " +
                                    "The number stays available under the shield icon in chat for later checks.",
                                fontFamily = QuantumMonospace,
                                fontSize = 9.sp,
                                color = SubatomicGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                        }

                        // Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onConfirm(verificationResult) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECORD SCAN & CONTINUE",
                                    fontFamily = QuantumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SubatomicGray
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ParticleBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ABORT & RESCAN",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    is HandshakeVerificationResult.InvalidSignature -> {
                        // Error Header
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = IsotopeMagenta,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "CRYPTOGRAPHIC SIGNATURE INVALID",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = IsotopeMagenta
                        )
                        Text(
                            text = "The scanned payload signature could not be verified against the peer's Ed25519 identity key. Possible MITM alteration detected.",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = NeutronWhite,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = IsotopeMagenta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DISMISS",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is HandshakeVerificationResult.Expired -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = IsotopeMagenta,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "UPLINK NONCE EXPIRED",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = IsotopeMagenta
                        )
                        Text(
                            text = "This QR code has exceeded its 60-second validity window. Please ask your peer to regenerate their handshake QR code.",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = NeutronWhite,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = IsotopeMagenta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "RESCAN FRESH QR",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is HandshakeVerificationResult.Error -> {
                        Text(
                            text = "HANDSHAKE ERROR",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            color = IsotopeMagenta
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = IsotopeMagenta)
                        ) {
                            Text("DISMISS")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3x4 Cryptographic Safety Matrix Display:
 * Formats the 60-digit safety number into 12 distinct 5-digit monospace blocks across 3 rows and 4 columns.
 * Performs a 500ms entry scramble with randomized alphanumeric characters accompanied by ~60ms throttled
 * TextHandleMove haptic pulses, locking into the true fingerprint with a definitive LongPress confirmation buzz.
 */
@Composable
fun ScrambleMatrixText(
    safetyNumber: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Extract exactly 60 digits and partition into 12 chunks of 5 digits (3 rows x 4 columns)
    val targetChunks = remember(safetyNumber) {
        val digitsOnly = safetyNumber.filter { it.isDigit() }
        val normalized = if (digitsOnly.length >= 60) {
            digitsOnly.take(60)
        } else {
            digitsOnly.padEnd(60, '0')
        }
        normalized.chunked(5) // Exactly 12 blocks of 5 digits
    }

    var isLocked by remember { mutableStateOf(false) }
    var displayedChunks by remember { mutableStateOf(targetChunks) }

    LaunchedEffect(safetyNumber) {
        // Plan §3: safety is 60 digits — scramble with digits only (no hex letters).
        val charset = "0123456789"
        val startTime = System.currentTimeMillis()
        val durationMs = 500L
        val throttleMs = 60L

        isLocked = false

        while (System.currentTimeMillis() - startTime < durationMs) {
            displayedChunks = List(12) {
                (1..5).map { charset.random() }.joinToString("")
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(throttleMs)
        }

        // Lock in the final verified safety number
        displayedChunks = targetChunks
        isLocked = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkMatterVariant)
            .border(
                1.dp,
                if (!isLocked) QuantumCyan.copy(alpha = 0.5f) else QuantumCyan,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3 rows x 4 columns = 12 blocks of 5 digits = 60 digits
        for (rowIndex in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIndex in 0 until 4) {
                    val chunkIndex = rowIndex * 4 + colIndex
                    val chunkText = displayedChunks.getOrElse(chunkIndex) { "•••••" }
                    Text(
                        text = chunkText,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = if (!isLocked) QuantumCyan.copy(alpha = 0.85f) else NeutronWhite,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


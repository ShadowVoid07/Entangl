package `in`.grayscales.entangl.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

@Composable
fun DeviceMigrationConfirmDialog(
    payload: TransferQrPayload,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, QuantumCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkMatter)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(QuantumCyan.copy(alpha = 0.15f))
                        .border(1.dp, QuantumCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = QuantumCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DEVICE MIGRATION DETECTED",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = QuantumCyan,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ENCRYPTED LOCAL HANDOFF BEACON",
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = SubatomicGray
                    )
                }

                // Telemetry Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkMatterVariant)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SENDER NODE UID",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                color = SubatomicGray
                            )
                            Text(
                                text = payload.senderUid.take(12) + "...",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutronWhite
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "P2P TUNNEL ENDPOINT",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                color = SubatomicGray
                            )
                            Text(
                                text = "${payload.ip}:${payload.port}",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = QuantumGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CIPHER SUITE",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                color = SubatomicGray
                            )
                            Text(
                                text = "AES-256-GCM + X25519",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                color = QuantumCyan
                            )
                        }
                    }
                }

                Text(
                    text = "Do you want to establish an encrypted local link to import all contacts, messages, and cryptographic succession certificates from this device?",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = NeutronWhite,
                    textAlign = TextAlign.Center
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuantumCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "IMPORT NOW",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.7f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SubatomicGray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ParticleBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCEL",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

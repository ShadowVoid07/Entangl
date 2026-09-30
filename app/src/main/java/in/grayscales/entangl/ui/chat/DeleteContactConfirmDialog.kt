package `in`.grayscales.entangl.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

/**
 * Tactical security confirmation dialog guarding permanent cryptographic session destruction.
 */
@Composable
fun DeleteContactConfirmDialog(
    contact: Contact,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, IsotopeMagenta, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkMatter)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(IsotopeMagenta.copy(alpha = 0.15f))
                        .border(1.dp, IsotopeMagenta, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = IsotopeMagenta,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "TERMINATE ENTANGLEMENT?",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp,
                    color = IsotopeMagenta,
                    textAlign = TextAlign.Center
                )

                val name = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                Text(
                    text = "Are you sure you want to terminate entanglement with $name?\n\nAll Double Ratchet session keys and message history will be permanently wiped. You will need to perform an optical handshake again to reconnect.",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = SubatomicGray,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IsotopeMagenta,
                            contentColor = NeutronWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PURGE & TERMINATE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = NeutronWhite
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SubatomicGray),
                        border = BorderStroke(1.dp, ParticleBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCEL",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = SubatomicGray
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray

private enum class ContactOptionAction {
    CLEAR_HISTORY,
    BLOCK_TOGGLE,
    DELETE
}

/**
 * Per-contact management sheet opened from the contact row overflow menu.
 * Tap still opens chat; this dialog owns delete / clear-history / block.
 */
@Composable
fun ContactOptionsDialog(
    contact: Contact,
    onClearHistory: () -> Unit,
    onBlockToggle: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var pendingAction by remember { mutableStateOf<ContactOptionAction?>(null) }
    val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan
    val name = contact.displayName?.ifBlank { null } ?: "Peer ${contact.uid.take(6).uppercase()}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkMatter)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(peerColor.copy(alpha = 0.16f))
                            .border(1.5.dp, peerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = contact.displayName?.trim()?.take(2)?.uppercase()
                        if (!initials.isNullOrBlank() && initials.all { it.isLetterOrDigit() }) {
                            Text(
                                text = initials,
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = peerColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = peerColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = if (contact.isBlocked) "BLOCKED" else "MANAGE CONTACT",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (contact.isBlocked) IsotopeMagenta else SubatomicGray
                        )
                    }
                }

                if (pendingAction == null) {
                    ContactOptionRow(
                        icon = Icons.Default.ClearAll,
                        label = "Clear chat history",
                        hint = "Deletes messages, keeps keys",
                        onClick = { pendingAction = ContactOptionAction.CLEAR_HISTORY }
                    )
                    ContactOptionRow(
                        icon = Icons.Default.Block,
                        label = if (contact.isBlocked) "Unblock peer" else "Block peer",
                        hint = if (contact.isBlocked) "Resume messaging" else "Pause all messaging",
                        destructive = !contact.isBlocked,
                        onClick = { pendingAction = ContactOptionAction.BLOCK_TOGGLE }
                    )
                    ContactOptionRow(
                        icon = Icons.Default.Delete,
                        label = "Delete contact",
                        hint = "Wipes keys + history",
                        destructive = true,
                        onClick = { pendingAction = ContactOptionAction.DELETE }
                    )
                } else {
                    val (title, body, confirmLabel) = when (pendingAction) {
                        ContactOptionAction.CLEAR_HISTORY -> Triple(
                            "CLEAR CHAT HISTORY?",
                            "All stored messages with $name will be permanently deleted.\n\nKeys and handshake state are kept — messaging resumes immediately.",
                            "CLEAR HISTORY"
                        )
                        ContactOptionAction.BLOCK_TOGGLE -> if (contact.isBlocked) {
                            Triple(
                                "UNBLOCK PEER?",
                                "$name will be able to message you again over the existing encrypted channel.",
                                "UNBLOCK"
                            )
                        } else {
                            Triple(
                                "BLOCK PEER?",
                                "$name will be muted: incoming packets dropped, sending paused.\n\nKeys and history are kept for unblock.",
                                "BLOCK"
                            )
                        }
                        else -> Triple(
                            "DELETE CONTACT?",
                            "$name, session keys and full history will be permanently wiped.\n\nReconnect requires a fresh optical handshake.",
                            "DELETE"
                        )
                    }
                    Text(
                        text = title,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IsotopeMagenta,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = body,
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
                                when (pendingAction) {
                                    ContactOptionAction.CLEAR_HISTORY -> onClearHistory()
                                    ContactOptionAction.BLOCK_TOGGLE -> onBlockToggle()
                                    else -> onDelete()
                                }
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
                                text = confirmLabel,
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { pendingAction = null },
                            modifier = Modifier.weight(0.8f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SubatomicGray),
                            border = BorderStroke(1.dp, ParticleBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "BACK",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactOptionRow(
    icon: ImageVector,
    label: String,
    hint: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (destructive) IsotopeMagenta else QuantumCyan
        ),
        border = BorderStroke(
            1.dp,
            if (destructive) IsotopeMagenta.copy(alpha = 0.6f) else QuantumCyan.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = hint,
                    fontFamily = QuantumMonospace,
                    fontSize = 9.sp,
                    color = SubatomicGray
                )
            }
        }
    }
}

package `in`.grayscales.entangl.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.ui.chat.ChatScreen
import `in`.grayscales.entangl.ui.chat.ContactsScreen
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

@Composable
fun QuantumTwoPaneLayout(
    contacts: List<Contact>,
    activeContact: Contact?,
    messages: List<Message>,
    selfDestructDuration: Long?,
    onSelectContact: (Contact?) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptContact: (Contact) -> Unit = {},
    onSendMessage: (String) -> Unit,
    onSimulateIncoming: (String) -> Unit,
    onSetSelfDestruct: (Long?) -> Unit,
    onScanQr: () -> Unit,
    onShowMyQr: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(VoidBackground)) {
        val isTwoPane = maxWidth >= 600.dp

        if (isTwoPane) {
            // Foldable / Tablet / Landscape Dual-Pane layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Contacts List (360dp)
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                ) {
                    ContactsScreen(
                        contacts = contacts,
                        selectedContactUid = activeContact?.uid,
                        onSelectContact = { onSelectContact(it) },
                        onDeleteContact = onDeleteContact,
                        onAcceptContact = onAcceptContact,
                        onScanQr = onScanQr,
                        onShowMyQr = onShowMyQr,
                        onOpenDashboard = onOpenDashboard
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ParticleBorder)
                )

                // Right Pane: Active Chat or Standby Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (activeContact != null) {
                        ChatScreen(
                            contact = activeContact,
                            messages = messages,
                            selfDestructDuration = selfDestructDuration,
                            onSendMessage = onSendMessage,
                            onSimulateIncoming = onSimulateIncoming,
                            onSetSelfDestruct = onSetSelfDestruct,
                            onBack = { onSelectContact(null) },
                            showBackButton = false,
                            onShowMyQr = onShowMyQr,
                            onAcceptContact = { onAcceptContact(activeContact) },
                            onDeclineContact = {
                                onDeleteContact(activeContact)
                                onSelectContact(null)
                            }
                        )
                    } else {
                        // Standby Mission Control Panel
                        StandbyPane(onScanQr = onScanQr, onShowMyQr = onShowMyQr)
                    }
                }
            }
        } else {
            // Candybar Phone Single-Pane layout
            if (activeContact != null) {
                ChatScreen(
                    contact = activeContact,
                    messages = messages,
                    selfDestructDuration = selfDestructDuration,
                    onSendMessage = onSendMessage,
                    onSimulateIncoming = onSimulateIncoming,
                    onSetSelfDestruct = onSetSelfDestruct,
                    onBack = { onSelectContact(null) },
                    showBackButton = true,
                    onShowMyQr = onShowMyQr,
                    onAcceptContact = { onAcceptContact(activeContact) },
                    onDeclineContact = {
                        onDeleteContact(activeContact)
                        onSelectContact(null)
                    }
                )
            } else {
                ContactsScreen(
                    contacts = contacts,
                    selectedContactUid = null,
                    onSelectContact = { onSelectContact(it) },
                    onDeleteContact = onDeleteContact,
                    onAcceptContact = onAcceptContact,
                    onScanQr = onScanQr,
                    onShowMyQr = onShowMyQr,
                    onOpenDashboard = onOpenDashboard
                )
            }
        }
    }
}

@Composable
private fun StandbyPane(
    onScanQr: () -> Unit,
    onShowMyQr: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = QuantumCyan,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = "QUANTUM MISSION CONTROL",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                color = QuantumCyan,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select an entangled peer from the left console pane or initiate a new mutual optical handshake.",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = SubatomicGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onScanQr,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCAN PEER HANDSHAKE QR",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = onShowMyQr,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = QuantumCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DISPLAY MY UPLINK BEACON",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

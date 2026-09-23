package `in`.grayscales.entangl.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.R
import `in`.grayscales.entangl.domain.model.Contact
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

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    selectedContactUid: String?,
    onSelectContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptContact: (Contact) -> Unit = {},
    onScanQr: () -> Unit,
    onShowMyQr: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(16.dp)
    ) {
        // App header & action icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_entangl_logo),
                    contentDescription = "Entangl Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ENTANGL",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = QuantumCyan
                    )
                    Text(
                        text = "${contacts.size} connected peers",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        color = SubatomicGray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dashboard Console button
                IconButton(
                    onClick = onOpenDashboard,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Console",
                        tint = QuantumCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Show My QR button
                IconButton(
                    onClick = onShowMyQr,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "My QR",
                        tint = QuantumCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Scan Peer QR button
                IconButton(
                    onClick = onScanQr,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuantumCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            // Empty state card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_entangl_logo),
                        contentDescription = "Entangl Logo",
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No connected peers",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = QuantumCyan,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Scan a peer's QR code or share your QR code to establish cryptographic entanglement and start messaging.",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        color = SubatomicGray,
                        textAlign = TextAlign.Center
                    )

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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan QR Code",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onShowMyQr,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = QuantumCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Show My QR",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            // Contacts list
            val pendingConnections = contacts.filter { !it.isAccepted }
            val establishedContacts = contacts.filter { it.isAccepted }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pendingConnections.isNotEmpty()) {
                    item {
                        Text(
                            text = "INCOMING CONNECTION REQUESTS",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = QuantumCyan,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(pendingConnections, key = { "pending_" + it.uid }) { contact ->
                        PendingConnectionCard(
                            contact = contact,
                            onClick = { onSelectContact(contact) },
                            onAccept = { onAcceptContact(contact) },
                            onIgnore = { onDeleteContact(contact) }
                        )
                    }

                    if (establishedContacts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ESTABLISHED PEERS",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SubatomicGray,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                items(establishedContacts, key = { it.uid }) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = contact.uid == selectedContactUid,
                        onClick = { onSelectContact(contact) },
                        onDelete = { onDeleteContact(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingConnectionCard(
    contact: Contact,
    onClick: () -> Unit = {},
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkMatterVariant)
            .border(1.dp, QuantumCyan, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(QuantumCyan.copy(alpha = 0.2f))
                        .border(1.dp, QuantumCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = QuantumCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    val name = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Text(
                        text = name,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeutronWhite
                    )
                    Text(
                        text = "Scanned your code and can send messages",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = SubatomicGray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuantumCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "Accept",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SubatomicGray
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ParticleBorder),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "Ignore",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) QuantumCyan else ParticleBorder
    val surfaceColor = if (isSelected) DarkMatterVariant else DarkMatter
    val isPending = contact.safetyNumber.startsWith("Pending")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
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
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPending) IsotopeMagenta.copy(alpha = 0.2f) else QuantumCyan.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (isPending) IsotopeMagenta else QuantumCyan,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isPending) IsotopeMagenta else QuantumCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val displayName = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Text(
                        text = displayName,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeutronWhite
                    )

                    if (isPending) {
                        Text(
                            text = "Tap to view & share QR",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = IsotopeMagenta
                        )
                    } else {
                        Text(
                            text = "Active secure channel",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = SubatomicGray
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPending) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IsotopeMagenta.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PENDING",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsotopeMagenta
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete contact",
                        tint = SubatomicGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

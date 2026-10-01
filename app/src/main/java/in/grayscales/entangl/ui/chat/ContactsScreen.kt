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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.R
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.ui.home.ChatListRow
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import kotlinx.coroutines.delay

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    selectedContactUid: String?,
    onSelectContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    onHandshake: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onScanQr: () -> Unit = onHandshake,
    onShowMyQr: () -> Unit = onHandshake,
    onOpenDashboard: () -> Unit = onOpenSettings,
    onAcceptContact: (Contact) -> Unit = {},
    localUsername: String = "",
    localProfileColor: String = "",
    onUpdateProfile: ((newUsername: String, newColorHex: String) -> Unit)? = null,
    onClearChat: (Contact) -> Unit = {},
    onBlockToggle: (Contact) -> Unit = {},
    lastMessages: Map<String, Message?> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingContactToIgnore by remember { mutableStateOf<Contact?>(null) }
    var optionsContact by remember { mutableStateOf<Contact?>(null) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // Presence clock: re-evaluates liveness dots as peers go quiet. Cheap —
    // one recomposition per 30s, no per-row timers.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    if (pendingContactToIgnore != null) {
        DeleteContactConfirmDialog(
            contact = pendingContactToIgnore!!,
            onConfirm = {
                onDeleteContact(pendingContactToIgnore!!)
                pendingContactToIgnore = null
            },
            onDismiss = { pendingContactToIgnore = null }
        )
    }

    if (contactToDelete != null) {
        DeleteContactConfirmDialog(
            contact = contactToDelete!!,
            onConfirm = {
                onDeleteContact(contactToDelete!!)
                contactToDelete = null
            },
            onDismiss = { contactToDelete = null }
        )
    }

    optionsContact?.let { target ->
        ContactOptionsDialog(
            contact = target,
            onClearHistory = { onClearChat(target) },
            onBlockToggle = { onBlockToggle(target) },
            onDelete = { contactToDelete = target },
            onDismiss = { optionsContact = null }
        )
    }

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
                        text = buildString {
                            append("${contacts.size} peers")
                            val pending = contacts.count { !it.isAccepted }
                            if (pending > 0) append(" • $pending requests")
                        },
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        color = SubatomicGray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Consolidated Mutual Handshake button (48dp min touch per plan clarity)
                IconButton(
                    onClick = onHandshake,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuantumCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Mutual Handshake",
                        tint = CyberDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Settings button (48dp min touch)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = QuantumCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (contacts.isEmpty()) {
            EmptyPeersState(
                onScanPeer = onScanQr,
                onShowMyBeacon = onShowMyQr,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Peer Search Field when roster has > 2 contacts
            if (contacts.size > 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search peers by handle or UID...",
                            fontFamily = QuantumMonospace,
                            fontSize = 12.sp,
                            color = SubatomicGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = QuantumCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = SubatomicGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuantumCyan,
                        unfocusedBorderColor = ParticleBorder,
                        focusedTextColor = NeutronWhite,
                        unfocusedTextColor = NeutronWhite,
                        cursorColor = QuantumCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Filter contacts by search query
            val filteredContacts = if (searchQuery.isBlank()) contacts else {
                contacts.filter {
                    (it.displayName?.contains(searchQuery, ignoreCase = true) == true) ||
                    it.uid.contains(searchQuery, ignoreCase = true)
                }
            }

            val pendingConnections = filteredContacts.filter { !it.isAccepted }
            val establishedContacts = filteredContacts.filter { it.isAccepted && !it.isBlocked }
            val blockedContacts = filteredContacts.filter { it.isAccepted && it.isBlocked }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = SubatomicGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "NO PEERS MATCHING \"${searchQuery.uppercase()}\"",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SubatomicGray
                                )
                            }
                        }
                    }
                }

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
                            onAccept = {
                                onAcceptContact(contact)
                                onScanQr()
                            },
                            onIgnore = { pendingContactToIgnore = contact }
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
                    ChatListRow(
                        contact = contact,
                        isSelected = contact.uid == selectedContactUid,
                        onClick = { onSelectContact(contact) },
                        lastMessage = lastMessages[contact.uid],
                        onOptionsClick = { optionsContact = contact },
                        nowMillis = nowMillis
                    )
                }

                if (blockedContacts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BLOCKED",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = IsotopeMagenta,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                items(blockedContacts, key = { "blocked_" + it.uid }) { contact ->
                    ChatListRow(
                        contact = contact,
                        isSelected = contact.uid == selectedContactUid,
                        onClick = { onSelectContact(contact) },
                        lastMessage = lastMessages[contact.uid],
                        onOptionsClick = { optionsContact = contact },
                        nowMillis = nowMillis
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
    val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkMatterVariant)
            .border(1.dp, peerColor, RoundedCornerShape(12.dp))
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
                        .background(peerColor.copy(alpha = 0.2f))
                        .border(1.dp, peerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = contact.displayName?.trim()?.take(2)?.uppercase()
                    if (!initials.isNullOrBlank() && initials.length <= 2 && initials.all { it.isLetterOrDigit() }) {
                        Text(
                            text = initials,
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = peerColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = peerColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    val name = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeutronWhite
                        )
                        val rawColor = contact.profileColor
                        if (rawColor != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(DarkMatter)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = rawColor,
                                    fontFamily = QuantumMonospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = peerColor
                                )
                            }
                        }
                    }
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
                        contentColor = CyberDark
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "Verify & scan",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberDark
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



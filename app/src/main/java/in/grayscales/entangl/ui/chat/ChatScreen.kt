package `in`.grayscales.entangl.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    contact: Contact,
    messages: List<Message>,
    selfDestructDuration: Long?,
    onSendMessage: (String) -> Unit,
    onSetSelfDestruct: (Long?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onShowMyQr: (() -> Unit)? = null,
    onScanPeerQr: (() -> Unit)? = null,
    onAcceptContact: (() -> Unit)? = null,
    onDeclineContact: (() -> Unit)? = null,
    onUnblockContact: (() -> Unit)? = null,
    isPeerTyping: Boolean = false
) {
    var inputText by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showNetworkInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val zeroizingMessageIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message on new message or when keyboard opens
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteContactConfirmDialog(
            contact = contact,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeclineContact?.invoke()
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showSafetyDialog) {
        SafetyNumberDialog(contact = contact, onDismiss = { showSafetyDialog = false })
    }

    if (showNetworkInfoDialog) {
        NetworkInfoDialog(
            contact = contact,
            isUnaccepted = !contact.isAccepted,
            isPendingReciprocal = contact.safetyNumber.startsWith("Pending"),
            onDismiss = { showNetworkInfoDialog = false },
            onTerminateEntanglement = {
                showNetworkInfoDialog = false
                showDeleteConfirmDialog = true
            }
        )
    }

    val isUnaccepted = !contact.isAccepted
    val isPendingReciprocal = contact.safetyNumber.startsWith("Pending")
    // Banner only while undecrypted PENDING exists. Healed messages flip to DELIVERED
    // via unlock, so the banner clears itself instead of demanding SCAN forever.
    val hasEncryptedMessages = remember(messages) {
        messages.any { it.plaintext == "Encrypted message" && it.status == MessageStatus.PENDING }
    }
    val isPendingChannel = isUnaccepted || isPendingReciprocal || hasEncryptedMessages

    val statusDotColor = when {
        isUnaccepted -> IsotopeMagenta
        isPendingReciprocal -> QuantumCyan
        else -> QuantumGreen
    }

    // Battery optimization: Only pulse status dot when in pending connection state.
    // When the encrypted channel is established, freeze alpha to solid 1.0f to eliminate 60/120fps recompositions.
    val pulseAlpha = if (isPendingChannel) {
        val infiniteTransition = rememberInfiniteTransition(label = "statusDotTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "statusDotPulse"
        )
        alpha
    } else {
        1f
    }

    val statusSubtext = when {
        isUnaccepted -> "Pending connection request"
        isPendingReciprocal -> "Accepted • Reciprocal scan pending"
        else -> "Encrypted channel active"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        // Chat Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkMatter)
                .border(1.dp, ParticleBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showNetworkInfoDialog = true }
                    .padding(4.dp)
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = QuantumCyan
                        )
                    }
                }

                val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan

                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                            fontSize = 11.sp,
                            color = peerColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = peerColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor.copy(alpha = pulseAlpha))
                )

                Column {
                    val displayName = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeutronWhite
                        )
                        val rawColor = contact.profileColor
                        if (rawColor != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(DarkMatterVariant)
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
                        text = statusSubtext,
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = if (isUnaccepted) IsotopeMagenta else if (isPendingReciprocal) QuantumCyan else SubatomicGray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Self-Destruct Timer Pill
                val timerLabel = when (selfDestructDuration) {
                    null -> "TTL: OFF"
                    30_000L -> "TTL: 30s"
                    300_000L -> "TTL: 5m"
                    3_600_000L -> "TTL: 1h"
                    else -> "TTL: ON"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selfDestructDuration != null) IsotopeMagenta.copy(alpha = 0.15f) else DarkMatterVariant)
                        .border(
                            1.dp,
                            if (selfDestructDuration != null) IsotopeMagenta else ParticleBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            val next = when (selfDestructDuration) {
                                null -> 30_000L
                                30_000L -> 300_000L
                                300_000L -> 3_600_000L
                                else -> null
                            }
                            onSetSelfDestruct(next)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (selfDestructDuration != null) IsotopeMagenta else SubatomicGray,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = timerLabel,
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selfDestructDuration != null) IsotopeMagenta else SubatomicGray
                        )
                    }
                }

                // Safety Number Button
                IconButton(
                    onClick = { showSafetyDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Safety Number",
                        tint = QuantumCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 1. Incoming Connection Request Card (when peer scanned us, and we haven't accepted yet)
        if (isUnaccepted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .border(1.dp, QuantumCyan, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkMatter),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(QuantumCyan.copy(alpha = 0.15f))
                                .border(1.dp, QuantumCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = QuantumCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONNECTION REQUEST",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = QuantumCyan
                            )
                            val name = contact.displayName ?: "A peer"
                            Text(
                                text = "$name scanned your QR code and wants to establish an encrypted connection.",
                                fontFamily = QuantumMonospace,
                                fontSize = 11.sp,
                                color = NeutronWhite
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onAcceptContact?.invoke()
                                (onScanPeerQr ?: onShowMyQr)?.invoke()
                            },
                            modifier = Modifier.weight(1.2f),
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
                                text = "Scan to verify",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { onDeclineContact?.invoke() },
                            modifier = Modifier.weight(0.9f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = IsotopeMagenta
                            ),
                            border = BorderStroke(1.dp, IsotopeMagenta.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Decline",
                                fontFamily = QuantumMonospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else if (isPendingReciprocal || hasEncryptedMessages) {
            // Reciprocal QR Share Prompt Banner (when accepted but peer needs our QR code to reply, or incoming messages require decryption)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkMatterVariant)
                    .border(1.dp, if (hasEncryptedMessages) IsotopeMagenta.copy(alpha = 0.7f) else QuantumCyan.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasEncryptedMessages) "Encrypted messages received" else "Reciprocal verification pending",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (hasEncryptedMessages) IsotopeMagenta else QuantumCyan
                        )
                        Text(
                            text = if (hasEncryptedMessages) "Scan peer's QR code to decrypt incoming messages." else "Scan peer's QR code to mutually verify cryptographic safety numbers.",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = NeutronWhite
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onShowMyQr != null) {
                            OutlinedButton(
                                onClick = onShowMyQr,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = QuantumCyan
                                ),
                                border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = QuantumCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MY QR",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuantumCyan
                                )
                            }
                        }
                        val scanPeerAction = onScanPeerQr ?: onShowMyQr
                        if (scanPeerAction != null) {
                            Button(
                                onClick = scanPeerAction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasEncryptedMessages) IsotopeMagenta else QuantumCyan,
                                    contentColor = if (hasEncryptedMessages) NeutronWhite else CyberDark
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (hasEncryptedMessages) NeutronWhite else CyberDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hasEncryptedMessages) "SCAN & DECRYPT" else "SCAN PEER",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasEncryptedMessages) NeutronWhite else CyberDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty() && !isUnaccepted) {
                item {
                    EmptyChatBanner(
                        contact = contact,
                        selfDestructDuration = selfDestructDuration,
                        onSayHello = {
                            inputText = "👋 Hello!"
                        }
                    )
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isZeroizing = zeroizingMessageIds.contains(message.id),
                        onZeroized = {
                            zeroizingMessageIds.remove(message.id)
                        },
                        onScanPeerQr = onScanPeerQr
                    )
                }
            }
        }

        // Quantum Typing Indicator (simulating post-quantum encrypted payload synthesis)
        if (isPeerTyping) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                QuantumTypingIndicator(
                    label = "ENCRYPTING PAYLOAD",
                    dotColor = QuantumCyan
                )
            }
        }

        // Bottom Bar: Locked Info with 1-tap Accept when unaccepted, blocked bar when
        // blocked, or Input Bar when accepted
        if (isUnaccepted) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuantumCyan.copy(alpha = 0.4f)),
                color = DarkMatter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val peerName = contact.displayName ?: "this peer"
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan required from $peerName",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = "Both codes + safety match unlocks chat",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = SubatomicGray
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onAcceptContact?.invoke()
                            (onScanPeerQr ?: onShowMyQr)?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scan to unlock",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        } else if (contact.isBlocked) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IsotopeMagenta.copy(alpha = 0.4f)),
                color = DarkMatter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocked — messaging paused",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = "Incoming dropped, sending disabled",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = SubatomicGray
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { onUnblockContact?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = QuantumCyan
                        ),
                        border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Unblock",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = QuantumCyan
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkMatter)
                    .border(1.dp, ParticleBorder)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Type an encrypted message...",
                                fontFamily = QuantumMonospace,
                                fontSize = 13.sp,
                                color = SubatomicGray
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuantumCyan,
                            unfocusedBorderColor = ParticleBorder,
                            focusedTextColor = NeutronWhite,
                            unfocusedTextColor = NeutronWhite,
                            cursorColor = QuantumCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    // Dynamic Send Button with active / dimmed visual feedback
                    val isSendActive = inputText.isNotBlank()
                    IconButton(
                        onClick = {
                            if (isSendActive) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = isSendActive,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSendActive) QuantumCyan else DarkMatterVariant)
                            .border(1.dp, if (isSendActive) QuantumCyan else ParticleBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isSendActive) Color.Black else SubatomicGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatBanner(
    contact: Contact,
    selfDestructDuration: Long?,
    onSayHello: () -> Unit,
    modifier: Modifier = Modifier
) {
    val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan
    val peerName = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
    val ttlText = when (selfDestructDuration) {
        null -> "Disabled"
        30_000L -> "30 seconds"
        300_000L -> "5 minutes"
        3_600_000L -> "1 hour"
        else -> "Active"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(peerColor.copy(alpha = 0.18f))
                    .border(1.5.dp, peerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initials = contact.displayName?.trim()?.take(2)?.uppercase()
                if (!initials.isNullOrBlank() && initials.length <= 2 && initials.all { it.isLetterOrDigit() }) {
                    Text(
                        text = initials,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = peerColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = peerColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = peerName,
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NeutronWhite,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = QuantumGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "QUANTUM ENTANGLEMENT ACTIVE",
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuantumGreen
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, ParticleBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "• End-to-end encrypted with ML-KEM-768 & Double Ratchet",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = SubatomicGray,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "• Ephemeral messages (Self-destruct timer: $ttlText)",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = SubatomicGray,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "• Zero-knowledge: relayed peer-to-peer without central storage",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = SubatomicGray,
                        lineHeight = 14.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onSayHello,
                border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = QuantumCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "👋 SAY HELLO",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = QuantumCyan
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoDialog(
    contact: Contact,
    isUnaccepted: Boolean,
    isPendingReciprocal: Boolean,
    onDismiss: () -> Unit,
    onTerminateEntanglement: (() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QuantumCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkMatter),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = QuantumCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "UPLINK DIAGNOSTICS",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = QuantumCyan
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NetworkInfoRow(label = "PEER ID", value = contact.uid)
                    NetworkInfoRow(label = "RELAY NETWORK", value = "Active (Multi-cast)")
                    NetworkInfoRow(label = "ENCRYPTION", value = "ML-KEM-768 + Double Ratchet")
                    
                    val statusValue = when {
                        isUnaccepted -> "Awaiting local authorization"
                        isPendingReciprocal -> "Awaiting peer authorization"
                        else -> "SECURE & VERIFIED"
                    }
                    val statusColor = when {
                        isUnaccepted -> IsotopeMagenta
                        isPendingReciprocal -> QuantumCyan
                        else -> QuantumGreen
                    }
                    NetworkInfoRow(label = "SESSION STATE", value = statusValue, valueColor = statusColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkMatterVariant,
                            contentColor = QuantumCyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CLOSE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = QuantumCyan
                        )
                    }

                    if (onTerminateEntanglement != null) {
                        OutlinedButton(
                            onClick = onTerminateEntanglement,
                            modifier = Modifier.weight(1.3f),
                            border = BorderStroke(1.dp, IsotopeMagenta.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IsotopeMagenta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = IsotopeMagenta
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TERMINATE",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = IsotopeMagenta
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkInfoRow(label: String, value: String, valueColor: Color = NeutronWhite) {
    Column {
        Text(
            text = label,
            fontFamily = QuantumMonospace,
            fontSize = 9.sp,
            color = SubatomicGray
        )
        Text(
            text = value,
            fontFamily = QuantumMonospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

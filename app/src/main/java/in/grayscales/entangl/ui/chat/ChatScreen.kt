package `in`.grayscales.entangl.ui.chat

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Direction
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    contact: Contact,
    messages: List<Message>,
    selfDestructDuration: Long?,
    onSendMessage: (String) -> Unit,
    onSimulateIncoming: (String) -> Unit,
    onSetSelfDestruct: (Long?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onShowMyQr: (() -> Unit)? = null,
    onAcceptContact: (() -> Unit)? = null,
    onDeclineContact: (() -> Unit)? = null
) {
    var inputText by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showSafetyDialog) {
        SafetyNumberDialog(contact = contact, onDismiss = { showSafetyDialog = false })
    }

    val isUnaccepted = !contact.isAccepted
    val isPendingReciprocal = contact.safetyNumber.startsWith("Pending")

    val statusDotColor = when {
        isUnaccepted -> IsotopeMagenta
        isPendingReciprocal -> QuantumCyan
        else -> QuantumGreen
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Column {
                    val displayName = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Text(
                        text = displayName,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeutronWhite
                    )
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

        // 1. Incoming Connection Request Card (when peer scanned us and we haven't accepted yet)
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
                            onClick = { onAcceptContact?.invoke() },
                            modifier = Modifier.weight(1.2f),
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
                                text = "Accept",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
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
        } else if (isPendingReciprocal) {
            // 2. Reciprocal QR Share Prompt Banner (when accepted but peer needs our QR code to reply)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkMatterVariant)
                    .border(1.dp, QuantumCyan.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reciprocal verification pending",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = QuantumCyan
                        )
                        Text(
                            text = "Share or scan QR codes to mutually verify cryptographic safety numbers.",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = NeutronWhite
                        )
                    }
                    if (onShowMyQr != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onShowMyQr,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHARE QR",
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        // Bottom Bar: Locked Info when unaccepted, or Input Bar when accepted
        if (isUnaccepted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkMatter)
                    .border(1.dp, ParticleBorder)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val peerName = contact.displayName ?: "this peer"
                Text(
                    text = "Accept connection request from $peerName to enable messaging.",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    color = SubatomicGray,
                    textAlign = TextAlign.Center
                )
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
                                text = "Type a message...",
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
                        singleLine = true,
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

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(QuantumCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Demo simulate button
                    IconButton(
                        onClick = {
                            onSimulateIncoming("Peer response: Packet received and verified. Ratchet advanced.")
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkMatterVariant)
                            .border(1.dp, QuantumCyan, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Simulate Packet",
                            tint = QuantumCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    if (message.id.startsWith("accept-") || message.id.startsWith("status-") || message.id.startsWith("system-")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = DarkMatterVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = QuantumCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = message.plaintext,
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        color = NeutronWhite,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val isOutgoing = message.direction == Direction.OUTGOING
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) Color(0xFF0C2B38) else Color(0xFF191924)
    val borderColor = if (isOutgoing) QuantumCyan.copy(alpha = 0.5f) else ParticleBorder
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isOutgoing) 14.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 14.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isOutgoing) 14.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 14.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Clean message text — without brackets
                Text(
                    text = message.plaintext,
                    fontFamily = QuantumMonospace,
                    fontSize = 13.sp,
                    color = NeutronWhite,
                    lineHeight = 19.sp
                )

                // Metadata line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        color = SubatomicGray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.selfDestructAt != null) {
                            Text(
                                text = "TTL",
                                fontFamily = QuantumMonospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsotopeMagenta
                            )
                        }

                        if (isOutgoing) {
                            when (message.status) {
                                MessageStatus.PENDING -> {
                                    Text(
                                        text = "Sending...",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 9.sp,
                                        color = SubatomicGray
                                    )
                                }
                                MessageStatus.SENT -> {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Sent",
                                        tint = SubatomicGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                MessageStatus.DELIVERED, MessageStatus.READ -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        tint = QuantumCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

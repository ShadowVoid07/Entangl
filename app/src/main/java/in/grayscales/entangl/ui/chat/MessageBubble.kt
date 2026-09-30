package `in`.grayscales.entangl.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Direction
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.LunarGray
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MessageBubble renders individual chat items with security micro-animations:
 * 1. Zeroization Glitch: When [isZeroizing] is true, rapidly jitters horizontal offset
 *    and flashes alpha for 300ms (digital shredding), then collapses height to 0.dp.
 * 2. Ratcheted Epoch Badge: Tap the lock icon next to the timestamp to trigger a 3D
 *    [rotationY] flip revealing the active Double Ratchet epoch key (e.g. "EPOCH: 4A9F").
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    isZeroizing: Boolean = false,
    onZeroized: () -> Unit = {},
    onScanPeerQr: (() -> Unit)? = null
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        isVisible = true
    }

    // Zeroization Glitch State
    var glitchOffsetX by remember { mutableFloatStateOf(0f) }
    var glitchAlpha by remember { mutableFloatStateOf(1f) }
    var isCollapsing by remember { mutableStateOf(false) }

    val heightScale by animateFloatAsState(
        targetValue = if (isCollapsing) 0f else 1f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "zeroizationCollapse"
    )

    LaunchedEffect(isZeroizing) {
        if (isZeroizing) {
            // 300ms digital shredding sequence: rapid horizontal jitter & alpha flickering
            val jitterOffsets = listOf(-6f, 8f, -7f, 6f, -8f, 5f, -4f, 7f, -3f, 0f)
            val jitterAlphas = listOf(0.2f, 0.9f, 0.3f, 0.85f, 0.15f, 0.95f, 0.25f, 0.8f, 0.35f, 1f)

            for (i in jitterOffsets.indices) {
                glitchOffsetX = jitterOffsets[i]
                glitchAlpha = jitterAlphas[i]
                delay(30)
            }

            // Begin vertical collapse
            isCollapsing = true
            delay(250)
            onZeroized()
        }
    }

    // If fully zeroized and collapsed, remove from layout completely
    if (isCollapsing && heightScale <= 0.01f) {
        return
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ),
        modifier = modifier
            .graphicsLayer {
                translationX = glitchOffsetX * density
                alpha = if (isCollapsing) heightScale else glitchAlpha
                scaleY = heightScale
            }
            .clipToBounds()
    ) {
        if (message.id.startsWith("accept-") || message.id.startsWith("status-") || message.id.startsWith("system-")) {
            SystemStatusBubble(message = message)
        } else {
            UserMessageBubble(
                message = message,
                isZeroizing = isZeroizing,
                onScanPeerQr = onScanPeerQr
            )
        }
    }
}

@Composable
private fun SystemStatusBubble(message: Message) {
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
}

/**
 * Message frame per plan §4.1 (no bubbles): translucent data frame with neon
 * edge marker and bracket addressing. Tunneling entrance: fast fade + vertical
 * snap with haptic tick on send/receive (see ChatScreen send path).
 */
@Composable
private fun UserMessageBubble(
    message: Message,
    isZeroizing: Boolean,
    onScanPeerQr: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }

    fun copyWithAutoClear(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("entangl", text))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        isCopied = true
        // Plan §5: clipboard auto-clear within 30s.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                clipboard.clearPrimaryClip()
            } catch (_: Exception) {
            }
            isCopied = false
        }, 30_000L)
    }

    val isOutgoing = message.direction == Direction.OUTGOING
    val isEncrypted = message.plaintext == "Encrypted message"
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = when {
        isEncrypted -> Color(0xFF221124)
        isOutgoing -> Color(0xFF0C2B38)
        else -> Color(0xFF191924)
    }
    val borderColor = when {
        isZeroizing -> IsotopeMagenta
        isEncrypted -> IsotopeMagenta.copy(alpha = 0.7f)
        isOutgoing -> QuantumCyan.copy(alpha = 0.5f)
        else -> ParticleBorder
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    // Live TTL countdown (ticks each second while armed).
    var ttlRemainingSec by remember(message.id, message.selfDestructAt) {
        mutableStateOf(
            message.selfDestructAt?.let {
                ((it - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            }
        )
    }
    LaunchedEffect(message.id, message.selfDestructAt) {
        val target = message.selfDestructAt ?: return@LaunchedEffect
        while (true) {
            val remaining = target - System.currentTimeMillis()
            if (remaining <= 0) {
                ttlRemainingSec = 0L
                return@LaunchedEffect
            }
            ttlRemainingSec = remaining / 1000L
            delay(1000L)
        }
    }
    // Plan §4.1: holographic data frame — square-ish panel + neon edge rail.
    val edgeColor = when {
        isZeroizing -> IsotopeMagenta
        isEncrypted -> IsotopeMagenta.copy(alpha = 0.7f)
        isOutgoing -> QuantumCyan.copy(alpha = 0.6f)
        else -> QuantumGreen.copy(alpha = 0.45f)
    }
    val frameShape = RoundedCornerShape(4.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = isCopied,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(QuantumCyan)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "COPIED TO CLIPBOARD",
                    fontFamily = QuantumMonospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberDark
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(frameShape)
                .background(bubbleColor)
                .border(1.dp, borderColor, frameShape)
                .pointerInput(message.id) {
                    detectTapGestures(
                        onLongPress = {
                            if (!isEncrypted) {
                                copyWithAutoClear(message.plaintext)
                            }
                        }
                    )
                }
        ) {
            // Neon edge rail per plan §4.1 (left for incoming, right for outgoing).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(if (isOutgoing) Alignment.TopEnd else Alignment.TopStart)
                        .size(width = 2.dp, height = 28.dp)
                        .background(edgeColor)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                if (isEncrypted) {
                    EncryptedMessagePlaceholder(onScanPeerQr = onScanPeerQr)
                } else {
                    // Bracket-addressed body per plan §4.1: [ ... ].
                    Text(
                        text = "[ " + message.plaintext + " ]",
                        fontFamily = QuantumMonospace,
                        fontSize = 13.sp,
                        color = NeutronWhite,
                        lineHeight = 19.sp
                    )
                }

                // Metadata: time only + live TTL countdown + small status tags.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        color = LunarGray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.selfDestructAt != null && ttlRemainingSec != null) {
                            Text(
                                text = "[TTL " + formatTtl(ttlRemainingSec ?: 0L) + "]",
                                fontFamily = QuantumMonospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsotopeMagenta
                            )
                        }

                        if (isOutgoing) {
                            when (message.status) {
                                MessageStatus.PENDING -> {
                                    Text(
                                        text = "[SENDING]",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 9.sp,
                                        color = LunarGray
                                    )
                                }
                                MessageStatus.SENT -> {
                                    Text(
                                        text = "[SENT]",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 9.sp,
                                        color = LunarGray
                                    )
                                }
                                MessageStatus.DELIVERED, MessageStatus.READ -> {
                                    Text(
                                        text = "[DELIVERED]",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = QuantumCyan
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
}

/**
 * Formats TTL countdown as mm:ss (or hh:mm:ss beyond an hour).
 */
private fun formatTtl(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * Visual placeholder rendered when a message ciphertext has arrived but the cryptographic
 * session key has not yet been derived (e.g. peer's QR code has not yet been scanned).
 * Displays a cryptographic lock badge and a prominent button to scan peer's QR code.
 */
@Composable
private fun EncryptedMessagePlaceholder(
    onScanPeerQr: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = IsotopeMagenta.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, IsotopeMagenta.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = IsotopeMagenta,
                    modifier = Modifier.padding(4.dp).size(12.dp)
                )
            }
            Text(
                text = "Encrypted message",
                fontFamily = QuantumMonospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = IsotopeMagenta
            )
        }

        Text(
            text = "Peer's cryptographic key needed to decrypt this transmission.",
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            color = SubatomicGray,
            lineHeight = 14.sp
        )

        if (onScanPeerQr != null) {
            Button(
                onClick = onScanPeerQr,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumCyan.copy(alpha = 0.15f),
                    contentColor = QuantumCyan
                ),
                border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SCAN PEER'S QR TO DECRYPT",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

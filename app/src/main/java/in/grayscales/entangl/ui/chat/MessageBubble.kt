package `in`.grayscales.entangl.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Direction
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
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
import kotlin.math.abs

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
    onZeroized: () -> Unit = {}
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
                isZeroizing = isZeroizing
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

@Composable
private fun UserMessageBubble(
    message: Message,
    isZeroizing: Boolean
) {
    val isOutgoing = message.direction == Direction.OUTGOING
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) Color(0xFF0C2B38) else Color(0xFF191924)
    val borderColor = if (isZeroizing) {
        IsotopeMagenta
    } else if (isOutgoing) {
        QuantumCyan.copy(alpha = 0.5f)
    } else {
        ParticleBorder
    }
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
                // Plaintext message body
                Text(
                    text = message.plaintext,
                    fontFamily = QuantumMonospace,
                    fontSize = 13.sp,
                    color = NeutronWhite,
                    lineHeight = 19.sp
                )

                // Metadata row: Timestamp, 3D Flip Ratchet Epoch Badge, TTL, and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = timeFormatter.format(Date(message.timestamp)),
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray
                        )

                        // Ratcheted Epoch Badge: Tap for 3D rotationY flip
                        RatchetedEpochBadge(messageId = message.id)
                    }

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

/**
 * Interactive Ratcheted Epoch Badge with 3D [rotationY] Flip.
 *
 * Face A (Default): Compact lock icon indicating active ratchet encryption.
 * Face B (Flipped): Reveals mock deterministic epoch string (e.g. "EPOCH: 4A9F").
 */
@Composable
fun RatchetedEpochBadge(
    messageId: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isFlipped by remember { mutableStateOf(false) }

    val density = LocalDensity.current.density
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "epochBadge3DFlip"
    )

    // Compute mock deterministic 4-character hex epoch from message identifier
    val epochCode = remember(messageId) {
        val cleanHash = abs(messageId.hashCode()).toString(16).uppercase()
        "EPOCH: " + cleanHash.take(4).padStart(4, '0')
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFlipped) QuantumCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                0.5.dp,
                if (isFlipped) QuantumCyan.copy(alpha = 0.6f) else ParticleBorder.copy(alpha = 0.4f),
                RoundedCornerShape(4.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFlipped = !isFlipped
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // Front Face: Cryptographic Lock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Ratcheted Epoch (Tap to inspect)",
                    tint = QuantumCyan.copy(alpha = 0.8f),
                    modifier = Modifier.size(10.dp)
                )
            }
        } else {
            // Reverse Face: Rotated 180 degrees back so text is upright
            Row(
                modifier = Modifier.graphicsLayer { rotationY = 180f },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = QuantumGreen,
                    modifier = Modifier.size(9.dp)
                )
                Text(
                    text = epochCode,
                    fontFamily = QuantumMonospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantumCyan,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

package `in`.grayscales.entangl.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray


/**
 * ChatListRow provides a sleek, cybernetic contact row featuring:
 * 1. Monospaced contact details & avatar with peer profile tint.
 * 2. Status verification badge.
 * 3. Plain message preview text.
 * 4. Tap to open conversation, overflow menu for manage actions.
 */
@Composable
fun ChatListRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastMessage: Message? = null,
    onOptionsClick: (() -> Unit)? = null,
    // Clock snapshot for presence (parent ticks ~30s). Green means verified peer
    // traffic inside PRESENCE_ACTIVE_WINDOW_MS — never handshake state alone.
    nowMillis: Long = System.currentTimeMillis()
) {
    val isPending = !contact.isAccepted
    val isBlocked = contact.isBlocked
    val borderColor = when {
        isSelected -> QuantumCyan
        isBlocked -> IsotopeMagenta.copy(alpha = 0.6f)
        else -> ParticleBorder
    }
    val surfaceColor = if (isSelected) DarkMatterVariant else DarkMatter
    val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan
    val avatarTint = if (isPending || isBlocked) IsotopeMagenta else peerColor

    val previewText: String? = when {
        isBlocked -> "Blocked — messaging paused"
        isPending -> "Pending mutual handshake connection"
        lastMessage != null -> lastMessage.plaintext
        else -> null
    }

    Box(
        modifier = modifier
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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar Circle with status beacon
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarTint.copy(alpha = 0.16f))
                            .border(1.5.dp, avatarTint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = contact.displayName?.trim()?.take(2)?.uppercase()
                        if (!initials.isNullOrBlank() && initials.length <= 2 && initials.all { it.isLetterOrDigit() }) {
                            Text(
                                text = initials,
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = avatarTint
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = avatarTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Liveness pip: pending handshake = magenta; blocked = gray (muted
                    // regardless of traffic); accepted peers show green ONLY with
                    // fresh verified traffic, gray otherwise.
                    val lastSeen = contact.lastSeenAt ?: 0L
                    val isLive = !isPending && !isBlocked &&
                        (nowMillis - lastSeen) <= Contact.PRESENCE_ACTIVE_WINDOW_MS &&
                        lastSeen > 0L
                    val pipColor = when {
                        isPending -> IsotopeMagenta
                        isBlocked -> SubatomicGray
                        isLive -> QuantumGreen
                        else -> SubatomicGray
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pipColor)
                            .border(1.5.dp, DarkMatter, CircleShape)
                    )
                }

                // Contact name + preview
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val displayName = contact.displayName ?: "Peer ${contact.uid.take(6).uppercase()}"
                    Text(
                        text = displayName,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeutronWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Message preview (hidden when the chat is truly empty)
                    if (previewText != null) {
                        Text(
                            text = previewText,
                            color = if (isPending || isBlocked) IsotopeMagenta else SubatomicGray,
                            fontSize = 12.sp,
                            fontFamily = QuantumMonospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side status / badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isBlocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IsotopeMagenta.copy(alpha = 0.15f))
                            .border(0.5.dp, IsotopeMagenta, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BLOCKED",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsotopeMagenta
                        )
                    }
                } else if (isPending) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IsotopeMagenta.copy(alpha = 0.15f))
                            .border(0.5.dp, IsotopeMagenta, RoundedCornerShape(4.dp))
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
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = QuantumCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "ZKP",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray
                        )
                    }
                }
            }

            if (onOptionsClick != null) {
                androidx.compose.material3.IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Contact options",
                        tint = SubatomicGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

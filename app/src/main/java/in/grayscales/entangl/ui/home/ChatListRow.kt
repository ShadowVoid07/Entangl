package `in`.grayscales.entangl.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
 * Modifier extension applying an 8.dp Gaussian blur when [isBlurActive] is true,
 * strictly guarded by an SDK version check (Android 12 / API 31+).
 *
 * For API < 31, no unsupported blur modifier is attached to prevent rendering crashes.
 */
fun Modifier.privacyBlur(
    isBlurActive: Boolean,
    blurRadius: Dp = 8.dp
): Modifier {
    return if (isBlurActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = blurRadius)
    } else {
        this
    }
}

/**
 * Anti-Shoulder-Surfing Privacy Blur Text composable.
 *
 * - Applies an 8.dp Gaussian blur on API 31+ (Android S+).
 * - Fallback for API < 31: Renders a solid black 90% opacity overlay box covering the preview text.
 * - Reveals crisp plaintext when [isRevealed] is true (e.g. during physical press-and-hold).
 */
@Composable
fun PrivacyBlurText(
    text: String,
    isRevealed: Boolean,
    modifier: Modifier = Modifier,
    isPrivacyEnabled: Boolean = true,
    blurRadius: Dp = 8.dp,
    color: Color = SubatomicGray,
    fontSize: TextUnit = 12.sp,
    fontFamily: FontFamily = QuantumMonospace,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val isObfuscated = isPrivacyEnabled && !isRevealed

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // Base preview text
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.privacyBlur(
                isBlurActive = isObfuscated,
                blurRadius = blurRadius
            )
        )

        // API < 31 Fallback: Solid black 90% opacity overlay box
        if (isObfuscated && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.90f))
                    .border(0.5.dp, ParticleBorder, RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * ChatListRow provides a sleek, cybernetic contact row featuring:
 * 1. Monospaced contact details & avatar with peer profile tint.
 * 2. Status verification badge & safety state.
 * 3. 8.dp Gaussian privacy blur on message preview text (API >= 31).
 * 4. API < 31 fallback: solid black 90% opacity overlay box.
 * 5. Physical press-and-hold gesture via [pointerInput] to temporarily reveal the preview text.
 * 6. Quick tap to open conversation.
 */
@Composable
fun ChatListRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastMessage: Message? = null,
    isPrivacyBlurEnabled: Boolean = true
) {
    var isHolding by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val isPending = !contact.isAccepted
    val borderColor = if (isSelected) QuantumCyan else ParticleBorder
    val surfaceColor = if (isSelected) DarkMatterVariant else DarkMatter
    val peerColor = contact.profileColor?.let { ColorUtils.parseColorOrNull(it) } ?: QuantumCyan
    val avatarTint = if (isPending) IsotopeMagenta else peerColor

    val previewText = when {
        isPending -> "Pending mutual handshake connection"
        lastMessage != null -> lastMessage.plaintext
        else -> "Secure ratchet channel established"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .pointerInput(contact.uid) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val released = tryAwaitRelease()
                        isHolding = false
                        if (released) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
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

                    // Verification status pip
                    val pipColor = if (isPending) IsotopeMagenta else QuantumGreen
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pipColor)
                            .border(1.5.dp, DarkMatter, CircleShape)
                    )
                }

                // Name, Verification, and Message Preview
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        if (!isPending) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Verified Handshake",
                                tint = QuantumCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Message Preview with Anti-Shoulder-Surfing Privacy Blur
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrivacyBlurText(
                            text = previewText,
                            isRevealed = isHolding,
                            isPrivacyEnabled = isPrivacyBlurEnabled,
                            blurRadius = 8.dp,
                            color = if (isPending) IsotopeMagenta else SubatomicGray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Privacy shield icon showing active shoulder-surfing guard
                        if (isPrivacyBlurEnabled && !isPending) {
                            AnimatedVisibility(
                                visible = !isHolding,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Privacy Blur Active (Hold to reveal)",
                                    tint = SubatomicGray.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = isHolding,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Revealed",
                                    tint = QuantumCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side status / badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPending) {
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
        }
    }
}

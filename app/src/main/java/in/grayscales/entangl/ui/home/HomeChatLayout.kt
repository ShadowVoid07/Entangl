package `in`.grayscales.entangl.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.ui.chat.ChatScreen
import `in`.grayscales.entangl.ui.chat.ContactsScreen
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

/**
 * Material 3 Adaptive List-Detail Pane Scaffold.
 *
 * Provides responsive dual-pane presentation on foldables, tablets, and wide displays
 * (breakpoint >= [dualPaneBreakpointDp]), while handling list-to-detail push navigation
 * with fluid horizontal transitions on compact phone screens.
 */
@Composable
fun ListDetailPaneScaffold(
    isDetailVisible: Boolean,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dualPaneBreakpointDp: Dp = 600.dp,
    listPaneWidthDp: Dp = 360.dp,
    onBackFromDetail: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        val isDualPane = maxWidth >= dualPaneBreakpointDp

        if (isDualPane) {
            // Foldable / Tablet / Wide Landscape Dual-Pane Mode
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: List
                Box(
                    modifier = Modifier
                        .width(listPaneWidthDp)
                        .fillMaxHeight()
                ) {
                    listPane()
                }

                // Cybernetic Vertical Separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ParticleBorder)
                )

                // Right Pane: Detail
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    detailPane()
                }
            }
        } else {
            // Compact Phone Single-Pane: Pushes List to Detail with Back navigation
            BackHandler(enabled = isDetailVisible) {
                onBackFromDetail()
            }

            AnimatedContent(
                targetState = isDetailVisible,
                transitionSpec = {
                    if (targetState) {
                        // Push into Detail: slide in from right, list retreats slightly left
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width / 3 } + fadeOut()
                        )
                    } else {
                        // Pop back to List: list slides in from left, detail retreats right
                        (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "ListDetailPanePushTransition",
                modifier = Modifier.fillMaxSize()
            ) { showDetail ->
                if (showDetail) {
                    detailPane()
                } else {
                    listPane()
                }
            }
        }
    }
}

/**
 * Adaptive Home & Chat Layout implementing the Material 3 ListDetailPaneScaffold.
 *
 * - On phones: Pushes from the contact list to the active conversation.
 * - On foldables & tablets: Renders the Contact List on the left pane and the active Chat on the right pane.
 * - Incorporates [ChatListRow] with SDK-safe privacy blur (API >= 31) and API < 31 fallback.
 */
@Composable
fun HomeChatLayout(
    contacts: List<Contact>,
    activeContact: Contact?,
    messages: List<Message>,
    selfDestructDuration: Long?,
    onSelectContact: (Contact?) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onSendMessage: (String) -> Unit,
    onSetSelfDestruct: (Long?) -> Unit,
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
    isPrivacyBlurEnabled: Boolean = true
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDualPane = maxWidth >= 600.dp

        ListDetailPaneScaffold(
            isDetailVisible = activeContact != null,
            dualPaneBreakpointDp = 600.dp,
            listPaneWidthDp = 360.dp,
            onBackFromDetail = { onSelectContact(null) },
            modifier = Modifier.fillMaxSize(),
            listPane = {
                ContactsScreen(
                    contacts = contacts,
                    selectedContactUid = activeContact?.uid,
                    onSelectContact = { onSelectContact(it) },
                    onDeleteContact = onDeleteContact,
                    onAcceptContact = onAcceptContact,
                    onHandshake = onHandshake,
                    onOpenSettings = onOpenSettings,
                    onScanQr = onScanQr,
                    onShowMyQr = onShowMyQr,
                    onOpenDashboard = onOpenDashboard,
                    localUsername = localUsername,
                    localProfileColor = localProfileColor,
                    onUpdateProfile = onUpdateProfile
                )
            },
            detailPane = {
                if (activeContact != null) {
                    ChatScreen(
                        contact = activeContact,
                        messages = messages,
                        selfDestructDuration = selfDestructDuration,
                        onSendMessage = onSendMessage,
                        onSetSelfDestruct = onSetSelfDestruct,
                        onBack = { onSelectContact(null) },
                        showBackButton = !isDualPane,
                        onShowMyQr = onShowMyQr,
                        onAcceptContact = { onAcceptContact(activeContact) },
                        onDeclineContact = {
                            onDeleteContact(activeContact)
                            onSelectContact(null)
                        }
                    )
                } else {
                    // Mission Control Standby Pane for Tablet / Foldable right pane
                    StandbyPane(
                        onHandshake = onHandshake,
                        onScanQr = onScanQr,
                        onShowMyQr = onShowMyQr
                    )
                }
            }
        )
    }
}

/**
 * Standby display for foldables/tablets when no active conversation is selected in the detail pane.
 */
@Composable
private fun StandbyPane(
    onHandshake: () -> Unit = {},
    onScanQr: () -> Unit = onHandshake,
    onShowMyQr: () -> Unit = onHandshake
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
                .fillMaxWidth(0.80f)
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
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                color = QuantumCyan,
                textAlign = TextAlign.Center
            )

            Text(
                text = "No active conversation selected.\nSelect a peer from the left pane or establish a zero-knowledge handshake.",
                fontFamily = QuantumMonospace,
                fontSize = 12.sp,
                color = SubatomicGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onScanQr,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuantumCyan,
                        contentColor = CyberDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SCAN QR",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onShowMyQr,
                    border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = QuantumCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MY QR",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

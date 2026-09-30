package `in`.grayscales.entangl.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.ui.home.HomeChatLayout

/**
 * QuantumTwoPaneLayout delegates to [HomeChatLayout], implementing Material 3's
 * adaptive [ListDetailPaneScaffold] with foldable/tablet split-pane rendering
 * and phone push list-to-detail navigation.
 */
@Composable
fun QuantumTwoPaneLayout(
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
    isPrivacyBlurEnabled: Boolean = true,
    onClearChat: (Contact) -> Unit = {},
    onBlockToggle: (Contact) -> Unit = {},
    onUnblockContact: () -> Unit = {}
) {
    HomeChatLayout(
        contacts = contacts,
        activeContact = activeContact,
        messages = messages,
        selfDestructDuration = selfDestructDuration,
        onSelectContact = onSelectContact,
        onDeleteContact = onDeleteContact,
        onSendMessage = onSendMessage,
        onSetSelfDestruct = onSetSelfDestruct,
        onHandshake = onHandshake,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        onScanQr = onScanQr,
        onShowMyQr = onShowMyQr,
        onOpenDashboard = onOpenDashboard,
        onAcceptContact = onAcceptContact,
        localUsername = localUsername,
        localProfileColor = localProfileColor,
        onUpdateProfile = onUpdateProfile,
        isPrivacyBlurEnabled = isPrivacyBlurEnabled,
        onClearChat = onClearChat,
        onBlockToggle = onBlockToggle,
        onUnblockContact = onUnblockContact
    )
}

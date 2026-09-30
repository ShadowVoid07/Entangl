package `in`.grayscales.entangl.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.grayscales.entangl.core.crypto.CryptoManager
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.data.notification.EntanglNotificationManager
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.model.Direction
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus
import `in`.grayscales.entangl.domain.repository.ContactRepository
import `in`.grayscales.entangl.domain.repository.MessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val cryptoManager: CryptoManager,
    val handshakeManager: HandshakeManager,
    private val notificationManager: EntanglNotificationManager,
    val nodeIdentityManager: NodeIdentityManager,
    private val networkTransport: `in`.grayscales.entangl.data.network.NetworkTransport,
    val localTransferManager: `in`.grayscales.entangl.data.network.LocalTransferManager
) : ViewModel() {

    // Persistent local node UID and .onion address
    val localUid: String get() = nodeIdentityManager.localUid
    val localOnion: String get() = nodeIdentityManager.localOnion

    // Local user codename state (max 25 chars)
    private val _username = MutableStateFlow(nodeIdentityManager.username ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _profileColor = MutableStateFlow(nodeIdentityManager.profileColor)
    val profileColor: StateFlow<String> = _profileColor.asStateFlow()

    private val _isUsernameSet = MutableStateFlow(nodeIdentityManager.isUsernameSet)
    val isUsernameSet: StateFlow<Boolean> = _isUsernameSet.asStateFlow()

    // Contact awaiting reciprocal QR scan prompt
    private val _promptReciprocalScanForContact = MutableStateFlow<Contact?>(null)
    val promptReciprocalScanForContact: StateFlow<Contact?> = _promptReciprocalScanForContact.asStateFlow()

    fun setUsername(newUsername: String) {
        val trimmed = newUsername.trim().take(NodeIdentityManager.MAX_USERNAME_LENGTH)
        if (trimmed.isNotEmpty()) {
            nodeIdentityManager.username = trimmed
            _username.value = trimmed
            _isUsernameSet.value = true
        }
    }

    fun setProfile(newUsername: String, newColorHex: String) {
        val trimmed = newUsername.trim().take(NodeIdentityManager.MAX_USERNAME_LENGTH)
        if (trimmed.isNotEmpty()) {
            nodeIdentityManager.username = trimmed
            _username.value = trimmed
            _isUsernameSet.value = true
        }
        val sanitizedColor = NodeIdentityManager.sanitizeHexColor(newColorHex)
        nodeIdentityManager.profileColor = sanitizedColor
        _profileColor.value = sanitizedColor
    }

    fun setProfileColor(newColorHex: String) {
        val sanitizedColor = NodeIdentityManager.sanitizeHexColor(newColorHex)
        nodeIdentityManager.profileColor = sanitizedColor
        _profileColor.value = sanitizedColor
    }

    fun dismissReciprocalScanPrompt() {
        _promptReciprocalScanForContact.value = null
    }

    // All entangled contacts
    val contacts: StateFlow<List<Contact>> = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected contact UID
    private val _selectedContactUid = MutableStateFlow<String?>(null)

    // Active contact reactively synchronized with Room database changes
    val activeContact: StateFlow<Contact?> = combine(_selectedContactUid, contacts) { uid, list ->
        if (uid == null) null else list.find { it.uid == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active conversation message stream
    val activeMessages: StateFlow<List<Message>> = activeContact.flatMapLatest { contact ->
        if (contact != null) {
            notificationManager.cancelForContact(contact.uid)
            messageRepository.observeForContact(contact.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Self-destruct timer duration in milliseconds (null = Off)
    private val _selfDestructDuration = MutableStateFlow<Long?>(null)
    val selfDestructDuration: StateFlow<Long?> = _selfDestructDuration.asStateFlow()

    fun selectContact(contact: Contact?) {
        _selectedContactUid.value = contact?.uid
        notificationManager.setForegroundContact(contact?.uid)
        if (contact != null) {
            notificationManager.cancelForContact(contact.uid)
        }
    }

    fun selectContactByUid(uid: String) {
        _selectedContactUid.value = uid
        notificationManager.setForegroundContact(uid)
        notificationManager.cancelForContact(uid)
    }

    fun setSelfDestructDuration(durationMillis: Long?) {
        _selfDestructDuration.value = durationMillis
    }

    fun sendMessage(plaintext: String) {
        val contact = activeContact.value ?: return
        if (plaintext.isBlank()) return

        viewModelScope.launch {
            try {
                val selfDestructAt = _selfDestructDuration.value?.let { System.currentTimeMillis() + it }
                messageRepository.send(contact.uid, plaintext, selfDestructAt)
                contactRepository.updateLastSeen(contact.uid, System.currentTimeMillis())
            } catch (e: IllegalStateException) {
                // Mutual gate: chat is locked until both scans + safety confirm.
                // Never crash from UI send; user stays on locked state with guidance.
                Log.w("ChatViewModel", "Send blocked (mutual incomplete): ${e.message}")
            }
        }
    }

    /** Mark all viewed incoming messages as read (drives unread jump + receipts). */
    fun markActiveChatRead() {
        val contact = activeContact.value ?: return
        viewModelScope.launch {
            for (message in activeMessages.value) {
                if (message.direction == Direction.INCOMING && message.status == MessageStatus.DELIVERED) {
                    messageRepository.markRead(message.id)
                }
            }
        }
    }

    /** Delete a single vaporized message (TTL countdown completion). */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    /**
     * Demo / test utility: Simulates an incoming end-to-end encrypted packet
     * from the active peer to demonstrate live ratchet decryption and zero-leak notifications.
     */
    fun simulateIncomingPacket(plaintext: String = "Quantum link established. Transmission secure.") {
        val contact = activeContact.value ?: return

        viewModelScope.launch {
            val messageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val selfDestructAt = _selfDestructDuration.value?.let { now + it }

            val message = Message(
                id = messageId,
                contactUid = contact.uid,
                plaintext = plaintext,
                direction = Direction.INCOMING,
                status = MessageStatus.DELIVERED,
                timestamp = now,
                selfDestructAt = selfDestructAt
            )

            messageRepository.receiveAndStore(message)
            notificationManager.showIncomingMessageNotification(contact.uid)
        }
    }

    /**
     * Establishes entanglement from a scanned and verified peer QR handshake.
     * This call proves OUTBOUND (we scanned peer): sets hasScannedPeer=true,
     * preserves hasBeenScanned from prior SCAN_PING. If the peer already scanned
     * us (mutual complete), the session initializes and chat unlocks immediately —
     * no extra tap. Re-scans of an accepted peer refresh the session and keep chat open.
     * Crypto session is initialized ONLY on mutual completion or re-scan of accepted.
     */
    fun addContactFromHandshake(
        peerUid: String,
        peerPublicKey: ByteArray,
        peerOnion: String,
        safetyNumber: String,
        peerUsername: String = "",
        peerProfileColor: String = ""
    ) {
        viewModelScope.launch {
            // Self-scan guard
            if (peerUid == localUid) {
                Log.w("ChatViewModel", "Self-scan rejected for $peerUid")
                return@launch
            }
            val existingContact = contactRepository.getByUid(peerUid)
            // Key continuity: if peer rotated without succession, reject (require succession cert)
            if (existingContact != null && existingContact.publicKey.isNotEmpty() &&
                !existingContact.publicKey.contentEquals(peerPublicKey)
            ) {
                Log.e("ChatViewModel", "Key change without succession for $peerUid — rejected, require Device Succession")
                return@launch
            }
            val displayName = when {
                peerUsername.isNotBlank() -> peerUsername
                !existingContact?.displayName.isNullOrBlank() -> existingContact?.displayName!!
                else -> "Peer " + peerUid.take(6).uppercase()
            }
            val color = when {
                peerProfileColor.isNotBlank() -> peerProfileColor
                !existingContact?.profileColor.isNullOrBlank() -> existingContact?.profileColor
                else -> null
            }
            val hasBeenScanned = existingContact?.hasBeenScanned == true
            // Remember completed handshakes: re-scanning an accepted peer (same key,
            // verified above) must NOT lock a working chat back to pending.
            val alreadyAccepted = existingContact?.isAccepted == true
            val contact = Contact(
                uid = peerUid,
                publicKey = peerPublicKey,
                onionAddress = peerOnion,
                safetyNumber = safetyNumber,
                displayName = displayName,
                createdAt = existingContact?.createdAt ?: System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis(),
                // This scan completes mutual iff the peer already scanned us.
                // Otherwise stay pending until their SCAN_PING arrives.
                isAccepted = alreadyAccepted,
                profileColor = color,
                hasScannedPeer = true,
                hasBeenScanned = hasBeenScanned
            )

            contactRepository.save(contact)
            if (alreadyAccepted) {
                // Refresh session so a re-scan never breaks a live channel.
                try {
                    cryptoManager.initializeSession(peerUid, peerPublicKey, peerOnion)
                    messageRepository.unlockPendingMessages(peerUid)
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Session refresh on re-scan failed: ${e.message}")
                }
                _selectedContactUid.value = peerUid
            } else if (hasBeenScanned) {
                // Mutual just completed on our side (we scanned last): unlock immediately
                // so the user is not stuck tapping SCAN again. Safety matrix stays
                // available under the shield icon in chat for later visual audit.
                try {
                    cryptoManager.initializeSession(peerUid, peerPublicKey, peerOnion)
                    messageRepository.unlockPendingMessages(peerUid)
                    contactRepository.save(contact.copy(isAccepted = true))
                    _selectedContactUid.value = peerUid
                    Log.d("ChatViewModel", "Mutual complete — chat unlocked")
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Auto-unlock on mutual scan failed: ${e.message}")
                    _selectedContactUid.value = null
                }
            } else {
                // Do NOT select chat yet — stay on handshake profile for reciprocal scan.
                _selectedContactUid.value = null
            }

            // Send network scan ping to peer so they know their QR code was scanned
            // (this is the inbound proof for the peer side).
            val myUsername = nodeIdentityManager.username ?: ""
            val myColor = nodeIdentityManager.profileColor
            val myPub = cryptoManager.getLocalIdentityPublicKey() ?: handshakeManager.getOrGenerateIdentityKey()
            Log.d("ChatViewModel", "Dispatching SCAN_PING to peer $peerUid")
            var pingSent = false
            for (attempt in 1..3) {
                pingSent = networkTransport.sendScanPing(
                    recipientUid = peerUid,
                    localUid = localUid,
                    localUsername = myUsername,
                    localIdentityPub = myPub,
                    localOnion = localOnion,
                    localProfileColor = myColor
                )
                if (pingSent) {
                    Log.d("ChatViewModel", "SCAN_PING transmitted to $peerUid on attempt $attempt")
                    break
                }
                Log.w("ChatViewModel", "SCAN_PING attempt $attempt to $peerUid failed, retrying in 500ms...")
                delay(500L)
            }
            if (!pingSent) {
                Log.e("ChatViewModel", "All 3 attempts to send SCAN_PING to $peerUid failed")
            }
        }
    }

    /**
     * Final mutual confirmation gate. Must be called AFTER:
     * 1. hasScannedPeer==true (we scanned peer QR, signature+TTL+nonce verified), AND
     * 2. hasBeenScanned==true (peer SCAN_PING received, signature verified), AND
     * 3. user explicitly confirmed 60-digit safety numbers match out-of-band.
     * Only then isAccepted=true, session initialized, pending unlocked.
     * Suspends so callers can navigate only on real success (no false-positive return).
     */
    suspend fun confirmMutualHandshake(contactUid: String, safetyConfirmed: Boolean): Boolean {
        if (!safetyConfirmed) {
            Log.w("ChatViewModel", "Safety not confirmed for $contactUid — mutual blocked")
            return false
        }
        val contact = contactRepository.getByUid(contactUid) ?: return false
        if (!contact.hasScannedPeer || !contact.hasBeenScanned) {
            Log.w(
                "ChatViewModel",
                "Mutual incomplete for $contactUid " +
                    "(scanned=${contact.hasScannedPeer}, beenScanned=${contact.hasBeenScanned})"
            )
            return false
        }
        if (contact.publicKey.isEmpty()) return false
        try {
            cryptoManager.initializeSession(contact.uid, contact.publicKey, contact.onionAddress)
            messageRepository.unlockPendingMessages(contact.uid)
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Crypto init on mutual confirm failed: ${e.message}")
            return false
        }
        contactRepository.save(contact.copy(isAccepted = true, lastSeenAt = System.currentTimeMillis()))
        _selectedContactUid.value = contact.uid
        // Notify peer of acceptance (does NOT grant them acceptance — their side still requires mutual)
        val myUsername = nodeIdentityManager.username ?: ""
        val myColor = nodeIdentityManager.profileColor
        val myPub = try {
            cryptoManager.getLocalIdentityPublicKey() ?: handshakeManager.getOrGenerateIdentityKey()
        } catch (_: Exception) {
            ByteArray(0)
        }
        for (attempt in 1..3) {
            val sent = networkTransport.sendScanAccept(
                recipientUid = contact.uid,
                localUid = localUid,
                localUsername = myUsername,
                localIdentityPub = myPub,
                localOnion = localOnion,
                localProfileColor = myColor
            )
            if (sent) break
            delay(500L)
        }
        return true
    }

    /**
     * Legacy entry point kept for API compatibility with contact/chat screens.
     * MILITARY RULE: UI buttons must NEVER mutate scan state. hasBeenScanned is set
     * ONLY by MessageRepositoryImpl upon signature-verified SCAN_PING receipt.
     * Previously this method set hasBeenScanned=true on any tap, which faked inbound
     * proof for outbound-only contacts and showed THEY SCANNED ✓ without a real scan.
     * It also sent SCAN_ACCEPT receipts and raised prompts that triple-fired with the
     * handshake dialog + ledger. Now it is a pure no-op (navigation is handled by
     * callers via onScanQr); the handshake ledger is the single source of truth.
     */
    fun acceptContact(contact: Contact) {
        Log.d("ChatViewModel", "Scan-continue tapped; state unchanged, awaiting optical proof")
        _promptReciprocalScanForContact.value = null
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            if (_selectedContactUid.value == contact.uid) {
                _selectedContactUid.value = null
            }
            contactRepository.delete(contact.uid)
            cryptoManager.destroySession(contact.uid)
        }
    }

    /** Clear all stored message history for a contact (contact + keys kept). */
    fun clearChatHistory(contact: Contact) {
        viewModelScope.launch {
            messageRepository.clearChat(contact.uid)
        }
    }

    /** Block or unblock a peer. Blocked peers cannot send or receive until unblocked. */
    fun setBlocked(contact: Contact, blocked: Boolean) {
        viewModelScope.launch {
            contactRepository.getByUid(contact.uid)?.let { fresh ->
                contactRepository.save(fresh.copy(isBlocked = blocked))
            }
        }
    }
}

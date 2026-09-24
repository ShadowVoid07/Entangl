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
        if (contact != null) {
            notificationManager.cancelForContact(contact.uid)
        }
    }

    fun selectContactByUid(uid: String) {
        _selectedContactUid.value = uid
        notificationManager.cancelForContact(uid)
    }

    fun setSelfDestructDuration(durationMillis: Long?) {
        _selfDestructDuration.value = durationMillis
    }

    fun sendMessage(plaintext: String) {
        val contact = activeContact.value ?: return
        if (plaintext.isBlank()) return

        viewModelScope.launch {
            messageRepository.send(contact.uid, plaintext)
            contactRepository.updateLastSeen(contact.uid, System.currentTimeMillis())
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
     */
    fun addContactFromHandshake(
        peerUid: String,
        peerPublicKey: ByteArray,
        peerOnion: String,
        safetyNumber: String,
        peerUsername: String = ""
    ) {
        viewModelScope.launch {
            val displayName = peerUsername.ifBlank { "Peer " + peerUid.take(6).uppercase() }
            val contact = Contact(
                uid = peerUid,
                publicKey = peerPublicKey,
                onionAddress = peerOnion,
                safetyNumber = safetyNumber,
                displayName = displayName,
                createdAt = System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis(),
                isAccepted = true
            )

            contactRepository.save(contact)
            cryptoManager.initializeSession(peerUid, peerPublicKey, peerOnion)
            messageRepository.unlockPendingMessages(peerUid)
            _selectedContactUid.value = peerUid

            // Send network scan ping to peer so they know their QR code was scanned
            val myUsername = nodeIdentityManager.username ?: ""
            val myPub = cryptoManager.getLocalIdentityPublicKey() ?: handshakeManager.getOrGenerateIdentityKey()
            Log.i("ChatViewModel", "Dispatching SCAN_PING to peer $peerUid (localUsername: '$myUsername')")
            var pingSent = false
            for (attempt in 1..3) {
                pingSent = networkTransport.sendScanPing(
                    recipientUid = peerUid,
                    localUid = localUid,
                    localUsername = myUsername,
                    localIdentityPub = myPub,
                    localOnion = localOnion
                )
                if (pingSent) {
                    Log.i("ChatViewModel", "SCAN_PING successfully transmitted to $peerUid on attempt $attempt")
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
     * Accepts a connection from a peer who scanned our QR code.
     */
    fun acceptContact(contact: Contact) {
        viewModelScope.launch {
            // 1. If peer's public key was received in SCAN_PING, initialize crypto session immediately
            if (contact.publicKey.isNotEmpty()) {
                try {
                    cryptoManager.initializeSession(contact.uid, contact.publicKey, contact.onionAddress)
                    messageRepository.unlockPendingMessages(contact.uid)
                    Log.i("ChatViewModel", "Initialized crypto session on accept for ${contact.uid}")
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Crypto session init on accept failed: ${e.message}")
                }
            }

            val updatedContact = contact.copy(isAccepted = true)
            contactRepository.save(updatedContact)

            // 2. Record local in-chat status notice
            messageRepository.receiveAndStore(
                Message(
                    id = "accept-local-${System.currentTimeMillis()}",
                    contactUid = contact.uid,
                    plaintext = "You accepted the connection request from ${contact.displayName}",
                    direction = Direction.OUTGOING,
                    status = MessageStatus.DELIVERED,
                    timestamp = System.currentTimeMillis(),
                    selfDestructAt = null
                )
            )

            // 3. Send acceptance ping back to peer with identity public key
            val myUsername = nodeIdentityManager.username ?: ""
            val myPub = try {
                cryptoManager.getLocalIdentityPublicKey() ?: handshakeManager.getOrGenerateIdentityKey()
            } catch (_: Exception) {
                ByteArray(0)
            }

            Log.i("ChatViewModel", "Dispatching SCAN_ACCEPT to peer ${contact.uid} (localUsername: '$myUsername')")
            var acceptSent = false
            for (attempt in 1..3) {
                acceptSent = networkTransport.sendScanAccept(
                    recipientUid = contact.uid,
                    localUid = localUid,
                    localUsername = myUsername,
                    localIdentityPub = myPub,
                    localOnion = localOnion
                )
                if (acceptSent) {
                    Log.i("ChatViewModel", "SCAN_ACCEPT successfully transmitted to ${contact.uid} on attempt $attempt")
                    break
                }
                Log.w("ChatViewModel", "SCAN_ACCEPT attempt $attempt to ${contact.uid} failed, retrying in 500ms...")
                delay(500L)
            }
            if (!acceptSent) {
                Log.e("ChatViewModel", "All 3 attempts to send SCAN_ACCEPT to ${contact.uid} failed")
            }

            // 4. Trigger prompt to scan peer's QR code
            _promptReciprocalScanForContact.value = updatedContact
        }
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
}

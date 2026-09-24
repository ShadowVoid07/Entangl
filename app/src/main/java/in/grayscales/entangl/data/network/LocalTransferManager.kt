package `in`.grayscales.entangl.data.network

import android.content.Context
import android.util.Log
import `in`.grayscales.entangl.core.crypto.AeadCipher
import `in`.grayscales.entangl.core.crypto.ContactMigrationItem
import `in`.grayscales.entangl.core.crypto.DeviceMigrationPayload
import `in`.grayscales.entangl.core.crypto.Hkdf
import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import `in`.grayscales.entangl.core.crypto.MessageMigrationItem
import `in`.grayscales.entangl.core.crypto.SessionKeyMigrationItem
import `in`.grayscales.entangl.core.crypto.SuccessionCertificate
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.core.identity.SessionKeyStore
import `in`.grayscales.entangl.core.security.KeyDestructionService
import `in`.grayscales.entangl.core.util.SecureRandom
import `in`.grayscales.entangl.data.local.dao.ContactDao
import `in`.grayscales.entangl.data.local.dao.MessageDao
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import `in`.grayscales.entangl.data.local.entity.MessageEntity
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class TransferProgress {
    data object Idle : TransferProgress()
    data class WaitingForPeer(val qrPayload: TransferQrPayload) : TransferProgress()
    data class Transferring(val message: String) : TransferProgress()
    data class Completed(val message: String, val isOldDeviceWiped: Boolean) : TransferProgress()
    data class Failed(val error: String) : TransferProgress()
}

/**
 * Manages the direct P2P local encrypted transfer of all account data
 * between Old Device and New Device with automated cryptographic succession and self-destruct.
 */
@OptIn(ExperimentalEncodingApi::class)
class LocalTransferManager(
    private val context: Context,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val sessionKeyStore: SessionKeyStore,
    private val keyPairGenerator: KeyPairGenerator,
    private val nodeIdentityManager: NodeIdentityManager,
    private val networkTransport: NetworkTransport,
    private val keyDestructionService: KeyDestructionService
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var activeServerSocket: ServerSocket? = null

    private val _progress = MutableStateFlow<TransferProgress>(TransferProgress.Idle)
    val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    /**
     * Start the export server on Device A (Old Device).
     * Generates an ephemeral key, opens a local socket, and returns the QR connection payload.
     */
    fun startExportServer() {
        cancelActiveTransfer()

        serverJob = scope.launch {
            try {
                val localIp = getLocalIpAddress() ?: run {
                    _progress.value = TransferProgress.Failed("No active local Wi-Fi or hotspot connection found.")
                    return@launch
                }

                val serverSocket = ServerSocket(0).also { activeServerSocket = it }
                val port = serverSocket.localPort

                val (ephPub, ephPriv) = keyPairGenerator.generateEphemeralX25519()
                val authTokenBytes = SecureRandom.nextBytes(16)
                val authToken = Base64.encode(authTokenBytes)

                val qrPayload = TransferQrPayload(
                    ip = localIp,
                    port = port,
                    ephPub = Base64.encode(ephPub),
                    authToken = authToken,
                    senderUid = nodeIdentityManager.localUid
                )

                _progress.value = TransferProgress.WaitingForPeer(qrPayload)

                // Await incoming connection from New Device
                val socket = serverSocket.accept()
                _progress.value = TransferProgress.Transferring("Peer connected! Performing cryptographic handshake...")

                socket.soTimeout = 30000
                DataInputStream(socket.getInputStream()).use { dis ->
                    DataOutputStream(socket.getOutputStream()).use { dos ->
                        // 1. Verify authToken
                        val clientAuthToken = dis.readUTF()
                        if (clientAuthToken != authToken) {
                            throw SecurityException("Unauthorized local connection attempt: Auth token mismatch")
                        }

                        // 2. Receive Client Ephemeral Public Key and Client Identity Public Key
                        val clientEphPubLen = dis.readInt()
                        val clientEphPub = ByteArray(clientEphPubLen).also { dis.readFully(it) }

                        val clientIdentityPubLen = dis.readInt()
                        val clientIdentityPub = ByteArray(clientIdentityPubLen).also { dis.readFully(it) }

                        // 3. Derive AES-256-GCM tunnel key via ECDH + HKDF
                        val sharedSecret = keyPairGenerator.computeX25519KeyAgreement(ephPriv, clientEphPub)
                        val tunnelKey = Hkdf.deriveKey(
                            ikm = sharedSecret,
                            salt = "Entangl-Local-Transfer-v1".encodeToByteArray(),
                            info = "P2P-Tunnel-Key".encodeToByteArray(),
                            length = 32
                        )

                        _progress.value = TransferProgress.Transferring("Generating cryptographic delegation certificate...")

                        // 4. Create SuccessionCertificate
                        val myIdentityPub = keyPairGenerator.getStoredIdentityPublicKey() ?: keyPairGenerator.generateIdentityKeyPair()
                        val certificate = SuccessionCertificate.create(
                            oldIdentityPubKey = myIdentityPub,
                            newIdentityPubKey = clientIdentityPub,
                            keyPairGenerator = keyPairGenerator
                        )

                        _progress.value = TransferProgress.Transferring("Exporting and encrypting database records...")

                        // 5. Package contacts, messages, and session keys
                        val contacts = contactDao.getAll().map {
                            ContactMigrationItem(
                                uid = it.uid,
                                publicKey = it.publicKey,
                                onionAddress = it.onionAddress,
                                safetyNumber = it.safetyNumber,
                                displayName = it.displayName,
                                createdAt = it.createdAt,
                                lastSeenAt = it.lastSeenAt,
                                isAccepted = it.isAccepted
                            )
                        }

                        val messages = messageDao.getAllMessages().map {
                            MessageMigrationItem(
                                id = it.id,
                                contactUid = it.contactUid,
                                ciphertext = it.ciphertext,
                                direction = it.direction,
                                status = it.status,
                                timestamp = it.timestamp,
                                selfDestructAt = it.selfDestructAt
                            )
                        }

                        val sessionKeys = sessionKeyStore.loadAll().map { (uid, key) ->
                            SessionKeyMigrationItem(uid, key)
                        }

                        val migrationPayload = DeviceMigrationPayload(
                            senderUid = nodeIdentityManager.localUid,
                            contacts = contacts,
                            messages = messages,
                            sessionKeys = sessionKeys,
                            certificate = certificate
                        )

                        // 6. Encrypt payload with AES-256-GCM
                        val cborBytes = migrationPayload.toCbor()
                        val encryptedBytes = AeadCipher.encrypt(
                            key = tunnelKey,
                            plaintext = cborBytes,
                            aad = "entangl-device-migration-v1".encodeToByteArray()
                        )

                        _progress.value = TransferProgress.Transferring("Sending encrypted archive to new device...")

                        // 7. Transmit payload
                        dos.writeInt(encryptedBytes.size)
                        dos.write(encryptedBytes)
                        dos.flush()

                        // 8. Await Client ACK
                        val ack = dis.readUTF()
                        if (ack != "ACK_SUCCESS") {
                            throw IllegalStateException("Client returned error: $ack")
                        }

                        _progress.value = TransferProgress.Transferring("Broadcasting rotation notice to relays...")

                        // 9. Broadcast rotation notice across multi-relays
                        val rotationEnvelope = TransportEnvelope(
                            id = java.util.UUID.randomUUID().toString(),
                            type = TransportEnvelope.TYPE_IDENTITY_ROTATION,
                            senderUid = nodeIdentityManager.localUid,
                            senderIdentityPub = myIdentityPub,
                            senderOnion = nodeIdentityManager.localOnion,
                            recipientUid = "broadcast",
                            ciphertext = SuccessionCertificate.toByteArray(certificate)
                        )
                        networkTransport.sendEnvelope(rotationEnvelope)

                        _progress.value = TransferProgress.Transferring("Transfer verified! Executing cryptographic wipe...")

                        // 10. ATOMIC SELF-DESTRUCT
                        keyDestructionService.decommissionDevice()

                        _progress.value = TransferProgress.Completed(
                            message = "Account successfully transferred to new device! This device has been securely zeroized and decommissioned.",
                            isOldDeviceWiped = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalTransferManager", "Export failed: ${e.message}", e)
                _progress.value = TransferProgress.Failed("Transfer failed: ${e.message}")
            } finally {
                serverSocketClose()
            }
        }
    }

    /**
     * Connect to Device A as Device B (New Device) and import all data.
     */
    fun startImportClient(qrPayload: TransferQrPayload) {
        cancelActiveTransfer()

        scope.launch {
            _progress.value = TransferProgress.Transferring("Connecting to old device at ${qrPayload.ip}:${qrPayload.port}...")

            try {
                val socket = Socket(qrPayload.ip, qrPayload.port)
                socket.soTimeout = 45000

                DataOutputStream(socket.getOutputStream()).use { dos ->
                    DataInputStream(socket.getInputStream()).use { dis ->
                        // 1. Send Auth Token
                        dos.writeUTF(qrPayload.authToken)

                        // 2. Generate local Ephemeral X25519 and retrieve local Identity Public Key
                        val (myEphPub, myEphPriv) = keyPairGenerator.generateEphemeralX25519()
                        val myIdentityPub = keyPairGenerator.getStoredIdentityPublicKey()
                            ?: keyPairGenerator.generateIdentityKeyPair()

                        dos.writeInt(myEphPub.size)
                        dos.write(myEphPub)

                        dos.writeInt(myIdentityPub.size)
                        dos.write(myIdentityPub)
                        dos.flush()

                        // 3. Derive AES-256-GCM tunnel key via ECDH + HKDF
                        val remoteEphPub = Base64.decode(qrPayload.ephPub)
                        val sharedSecret = keyPairGenerator.computeX25519KeyAgreement(myEphPriv, remoteEphPub)
                        val tunnelKey = Hkdf.deriveKey(
                            ikm = sharedSecret,
                            salt = "Entangl-Local-Transfer-v1".encodeToByteArray(),
                            info = "P2P-Tunnel-Key".encodeToByteArray(),
                            length = 32
                        )

                        _progress.value = TransferProgress.Transferring("Downloading encrypted database...")

                        // 4. Read length-prefixed encrypted payload
                        val encryptedLen = dis.readInt()
                        val encryptedBytes = ByteArray(encryptedLen).also { dis.readFully(it) }

                        _progress.value = TransferProgress.Transferring("Decrypting and verifying payload integrity...")

                        // 5. Decrypt with AES-256-GCM
                        val decryptedCbor = AeadCipher.decrypt(
                            key = tunnelKey,
                            payload = encryptedBytes,
                            aad = "entangl-device-migration-v1".encodeToByteArray()
                        )

                        val payload = DeviceMigrationPayload.fromCbor(decryptedCbor)
                            ?: throw IllegalStateException("Corrupted migration payload format")

                        _progress.value = TransferProgress.Transferring("Importing ${payload.contacts.size} contacts and ${payload.messages.size} messages...")

                        // 6. Insert contacts
                        for (c in payload.contacts) {
                            contactDao.insertOrUpdate(
                                ContactEntity(
                                    uid = c.uid,
                                    publicKey = c.publicKey,
                                    onionAddress = c.onionAddress,
                                    safetyNumber = c.safetyNumber,
                                    displayName = c.displayName,
                                    createdAt = c.createdAt,
                                    lastSeenAt = c.lastSeenAt,
                                    isAccepted = c.isAccepted
                                )
                            )
                        }

                        // 7. Insert messages
                        for (m in payload.messages) {
                            messageDao.insertOrUpdate(
                                MessageEntity(
                                    id = m.id,
                                    contactUid = m.contactUid,
                                    ciphertext = m.ciphertext,
                                    direction = m.direction,
                                    status = m.status,
                                    timestamp = m.timestamp,
                                    selfDestructAt = m.selfDestructAt
                                )
                            )
                        }

                        // 8. Insert session keys
                        for (sk in payload.sessionKeys) {
                            sessionKeyStore.storeKey(sk.contactUid, sk.sessionKey)
                        }

                        // 9. Send success ACK
                        dos.writeUTF("ACK_SUCCESS")
                        dos.flush()

                        _progress.value = TransferProgress.Completed(
                            message = "Data imported successfully! ${payload.contacts.size} contacts and ${payload.messages.size} messages restored.",
                            isOldDeviceWiped = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalTransferManager", "Import failed: ${e.message}", e)
                _progress.value = TransferProgress.Failed("Import failed: ${e.message}")
            }
        }
    }

    fun cancelActiveTransfer() {
        serverJob?.cancel()
        serverJob = null
        serverSocketClose()
        _progress.value = TransferProgress.Idle
    }

    private fun serverSocketClose() {
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
        activeServerSocket = null
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}

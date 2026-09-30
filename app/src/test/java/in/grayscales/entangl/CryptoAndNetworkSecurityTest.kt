package `in`.grayscales.entangl

import `in`.grayscales.entangl.core.crypto.AeadCipher
import `in`.grayscales.entangl.core.crypto.DefaultCryptoManager
import `in`.grayscales.entangl.core.crypto.Hkdf
import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import `in`.grayscales.entangl.core.crypto.RatchetStateVerifier
import `in`.grayscales.entangl.core.crypto.SuccessionCertificate
import `in`.grayscales.entangl.data.network.NetworkTransport
import `in`.grayscales.entangl.data.network.TransportEnvelope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException

class CryptoAndNetworkSecurityTest {

    private val dummyRatchetVerifier = object : RatchetStateVerifier {
        override fun computeAndStoreHmac(contactUid: String, ratchetState: ByteArray) {}
        override fun verify(contactUid: String, ratchetState: ByteArray): Boolean = true
        override fun deleteHmac(contactUid: String) {}
    }

    // =========================================================================
    // 1. AEAD (AES-256-GCM) Tests
    // =========================================================================

    @Test
    fun testAes256GcmRoundTrip() {
        val key = ByteArray(32) { (it + 1).toByte() }
        val plaintext = "Quantum secure top secret message payload".encodeToByteArray()
        val aad = "contact-peer-42".encodeToByteArray()

        val ciphertext = AeadCipher.encrypt(key = key, plaintext = plaintext, aad = aad)
        assertNotNull(ciphertext)
        // Format is: 12-byte IV + ciphertext + 16-byte GCM tag
        assertEquals(plaintext.size + 12 + 16, ciphertext.size)

        val decrypted = AeadCipher.decrypt(key = key, payload = ciphertext, aad = aad)
        assertArrayEquals(plaintext, decrypted)
        assertEquals("Quantum secure top secret message payload", decrypted.decodeToString())
    }

    @Test
    fun testAes256GcmTamperedCiphertextRejection() {
        val key = ByteArray(32) { (it * 7).toByte() }
        val plaintext = "Uncompromised transaction: transfer $100".encodeToByteArray()
        val aad = "alice-to-bob".encodeToByteArray()

        val ciphertext = AeadCipher.encrypt(key = key, plaintext = plaintext, aad = aad)
        
        // Active attacker bit-flip attack on byte 15
        val tampered = ciphertext.copyOf()
        tampered[15] = (tampered[15].toInt() xor 0x01).toByte()

        try {
            AeadCipher.decrypt(key = key, payload = tampered, aad = aad)
            fail("Decryption MUST fail with AEADBadTagException when ciphertext is tampered!")
        } catch (e: Exception) {
            assertTrue(
                "Expected AEADBadTagException or Tag mismatch, got: ${e.javaClass.name}",
                e is AEADBadTagException || e.cause is AEADBadTagException || e.message?.contains("tag", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun testAes256GcmMismatchedAadRejection() {
        val key = ByteArray(32) { 0x5A }
        val plaintext = "Confidential chat".encodeToByteArray()
        val aliceAad = "recipient-alice".encodeToByteArray()
        val eveAad = "recipient-eve".encodeToByteArray()

        val ciphertext = AeadCipher.encrypt(key = key, plaintext = plaintext, aad = aliceAad)

        try {
            // Decrypt with mismatched AAD
            AeadCipher.decrypt(key = key, payload = ciphertext, aad = eveAad)
            fail("Decryption MUST fail when AAD (recipient binding) does not match!")
        } catch (e: Exception) {
            assertTrue(e is AEADBadTagException || e.cause is AEADBadTagException || e.message?.contains("tag", ignoreCase = true) == true)
        }
    }

    // =========================================================================
    // 2. HKDF-SHA256 Key Derivation Tests
    // =========================================================================

    @Test
    fun testHkdfDeterministicAndLength() {
        val ikm = "diffie-hellman-shared-secret-bytes".encodeToByteArray()
        val salt = "Entangl-Salt-2026".encodeToByteArray()
        val info = "AES-GCM-Key-Derivation".encodeToByteArray()

        val key32_a = Hkdf.deriveKey(ikm = ikm, salt = salt, info = info, length = 32)
        val key32_b = Hkdf.deriveKey(ikm = ikm, salt = salt, info = info, length = 32)
        val key64 = Hkdf.deriveKey(ikm = ikm, salt = salt, info = info, length = 64)

        assertEquals(32, key32_a.size)
        assertEquals(64, key64.size)
        assertArrayEquals(key32_a, key32_b)
        // First 32 bytes of 64-byte derivation should match 32-byte derivation
        assertArrayEquals(key32_a, key64.copyOfRange(0, 32))
    }

    @Test
    fun testHkdfContextSeparation() {
        val ikm = "common-shared-secret".encodeToByteArray()
        val keyChat = Hkdf.deriveKey(ikm = ikm, info = "chat-messages".encodeToByteArray(), length = 32)
        val keyTopics = Hkdf.deriveKey(ikm = ikm, info = "blinded-topics".encodeToByteArray(), length = 32)

        assertFalse(keyChat.contentEquals(keyTopics))
    }

    // =========================================================================
    // 3. DefaultCryptoManager Integration Tests
    // =========================================================================

    @Test
    fun testCryptoManagerEncryptDecryptRoundTrip() = runBlocking {
        val kpg = KeyPairGenerator()
        val manager = DefaultCryptoManager(kpg, dummyRatchetVerifier)

        val peerPublicKey = ByteArray(65) { 0x04 } // Mock EC uncompressed pub key
        manager.initializeSession("peer-bob", peerPublicKey, "bob-mesh-endpoint")

        val messageText = "Meet at the rendezvous point at 22:00"
        val encrypted = manager.encryptMessage("peer-bob", messageText.encodeToByteArray())

        assertNotNull(encrypted)
        assertFalse(encrypted.contentEquals(messageText.encodeToByteArray()))

        val decrypted = manager.decryptMessage("peer-bob", encrypted)
        assertEquals(messageText, decrypted.decodeToString())
    }

    @Test
    fun testDoubleRatchetForwardSecrecyAndEpochIncrement() = runBlocking {
        val kpg = KeyPairGenerator()
        val manager = DefaultCryptoManager(kpg, dummyRatchetVerifier)
        val peerPublicKey = ByteArray(65) { 0x05 }
        manager.initializeSession("peer-charlie", peerPublicKey, "charlie-mesh-endpoint")

        assertEquals(0, manager.getSessionRatchetEpoch("peer-charlie"))

        val msg1 = "Payload 1"
        val enc1 = manager.encryptMessage("peer-charlie", msg1.encodeToByteArray())
        assertEquals(1, manager.getSessionRatchetEpoch("peer-charlie"))

        val msg2 = "Payload 2"
        val enc2 = manager.encryptMessage("peer-charlie", msg2.encodeToByteArray())
        assertEquals(2, manager.getSessionRatchetEpoch("peer-charlie"))

        // Wire format header check: version = 0x01
        assertEquals(0x01.toByte(), enc1[0])
        assertEquals(0x01.toByte(), enc2[0])

        // Verify both can be decrypted
        assertEquals(msg1, manager.decryptMessage("peer-charlie", enc1).decodeToString())
        assertEquals(msg2, manager.decryptMessage("peer-charlie", enc2).decodeToString())
    }

    @Test
    fun testFixed2KbPayloadPaddingAndUnpadding() {
        val rawMessage = "Quantum secure zero-knowledge payload".encodeToByteArray()
        val padded = TransportEnvelope.padPayload(rawMessage)

        assertEquals("Padded payload must be exactly 2048 bytes for relay transport", 2048, padded.size)

        val unpadded = TransportEnvelope.unpadPayload(padded)
        assertArrayEquals("Unpadded payload must match original bytes", rawMessage, unpadded)

        // Legacy / unpadded payload fallback test
        val unpaddedDirect = TransportEnvelope.unpadPayload(rawMessage)
        assertArrayEquals("Unpadded direct should return original data", rawMessage, unpaddedDirect)
    }

    // =========================================================================
    // 4. TransportEnvelope Canonical & Signature Tests
    // =========================================================================

    @Test
    fun testEnvelopeCanonicalDataConsistency() {
        val envelope1 = TransportEnvelope(
            id = "msg-001",
            type = TransportEnvelope.TYPE_MESSAGE,
            senderUid = "alice",
            senderIdentityPub = ByteArray(32) { 0x01 },
            senderOnion = "alice.endpoint",
            recipientUid = "bob",
            ciphertext = "encrypted-payload".encodeToByteArray(),
            timestamp = 1720000000000L
        )

        val canonical1 = envelope1.getCanonicalData()
        val canonical2 = envelope1.getCanonicalData()
        assertArrayEquals(canonical1, canonical2)

        // Altering recipient MUST alter canonical bytes
        val envelopeTampered = envelope1.copy(recipientUid = "eve")
        assertFalse(canonical1.contentEquals(envelopeTampered.getCanonicalData()))
    }

    @Test
    fun testEnvelopeSigningAndVerificationRoundTrip() {
        val kpg = KeyPairGenerator()
        val transport = NetworkTransport(kpg)

        val envelope = TransportEnvelope(
            id = "msg-1234",
            type = TransportEnvelope.TYPE_MESSAGE,
            senderUid = "node-alice",
            senderIdentityPub = ByteArray(0),
            senderOnion = "alice.mesh",
            recipientUid = "node-bob",
            ciphertext = "secure-data".encodeToByteArray()
        )

        val signedEnvelope = transport.signEnvelope(envelope)
        assertTrue("Signature must not be empty", signedEnvelope.signature.isNotEmpty())
        assertTrue("Sender identity pub must not be empty", signedEnvelope.senderIdentityPub.isNotEmpty())

        val isValid = kpg.verify(
            publicKey = signedEnvelope.senderIdentityPub,
            data = signedEnvelope.getCanonicalData(),
            signature = signedEnvelope.signature
        )
        assertTrue("Envelope signature must verify successfully", isValid)

        // Tamper with ciphertext
        val tamperedCanonical = signedEnvelope.copy(ciphertext = "corrupted".encodeToByteArray()).getCanonicalData()
        val isTamperedValid = kpg.verify(
            publicKey = signedEnvelope.senderIdentityPub,
            data = tamperedCanonical,
            signature = signedEnvelope.signature
        )
        assertFalse("Tampered envelope signature must be rejected", isTamperedValid)
    }

    // =========================================================================
    // 5. SuccessionCertificate Tests
    // =========================================================================

    @Test
    fun testSuccessionCertificateValid() {
        val kpg = KeyPairGenerator()
        val oldPub = kpg.getStoredIdentityPublicKey() ?: kpg.generateIdentityKeyPair()
        val newPub = ByteArray(32) { 0x77 }

        val cert = SuccessionCertificate.create(
            oldIdentityPubKey = oldPub,
            newIdentityPubKey = newPub,
            keyPairGenerator = kpg
        )

        assertTrue(SuccessionCertificate.verify(cert, kpg))

        val serialized = SuccessionCertificate.toEncodedString(cert)
        val deserialized = SuccessionCertificate.fromEncodedString(serialized)
        assertNotNull(deserialized)
        assertTrue(SuccessionCertificate.verify(deserialized!!, kpg))

        // Tamper with new public key
        val tamperedCert = cert.copy(newIdentityPubKey = "fakeKeyBase64")
        assertFalse(SuccessionCertificate.verify(tamperedCert, kpg))
    }

    // =========================================================================
    // 6. Device Migration & Succession Tests
    // =========================================================================

    @Test
    fun testTransferQrPayloadSerialization() {
        val payload = `in`.grayscales.entangl.domain.model.TransferQrPayload(
            ip = "192.168.1.50",
            port = 45678,
            ephPub = "base64EphPublicKeyDataHere==",
            authToken = "authSecretToken12345",
            senderUid = "node-alpha-12345"
        )

        val qrString = payload.toQrString()
        assertTrue("QR string must have entangl-transfer URI scheme", qrString.startsWith("entangl-transfer://"))

        val parsed = `in`.grayscales.entangl.domain.model.TransferQrPayload.fromQrString(qrString)
        assertNotNull("Parsed payload must not be null", parsed)
        assertEquals(payload.ip, parsed!!.ip)
        assertEquals(payload.port, parsed.port)
        assertEquals(payload.ephPub, parsed.ephPub)
        assertEquals(payload.authToken, parsed.authToken)
        assertEquals(payload.senderUid, parsed.senderUid)

        // Invalid QR string rejection
        val invalidParsed = `in`.grayscales.entangl.domain.model.TransferQrPayload.fromQrString("invalid-qr-data")
        org.junit.Assert.assertNull(invalidParsed)
    }

    @Test
    fun testTransferTunnelEcdhHkdfDerivation() {
        val kpg = KeyPairGenerator()

        // Old Device generates ephemeral keypair
        val (ephPubA, ephPrivA) = kpg.generateEphemeralX25519()

        // New Device generates ephemeral keypair
        val (ephPubB, ephPrivB) = kpg.generateEphemeralX25519()

        // Both perform ECDH key agreement
        val secretA = kpg.computeX25519KeyAgreement(ephPrivA, ephPubB)
        val secretB = kpg.computeX25519KeyAgreement(ephPrivB, ephPubA)

        assertArrayEquals("ECDH shared secret must match symmetrically", secretA, secretB)

        // Derive 32-byte AES-256-GCM tunnel key via HKDF
        val salt = "Entangl-Local-Transfer-v1".encodeToByteArray()
        val info = "P2P-Tunnel-Key".encodeToByteArray()

        val tunnelKeyA = Hkdf.deriveKey(secretA, salt, info, 32)
        val tunnelKeyB = Hkdf.deriveKey(secretB, salt, info, 32)

        assertArrayEquals("Derived tunnel keys must match", tunnelKeyA, tunnelKeyB)
        assertEquals("Tunnel key length must be 32 bytes for AES-256", 32, tunnelKeyA.size)
    }

    @Test
    fun testDeviceMigrationPayloadCborEncryption() {
        val kpg = KeyPairGenerator()
        val oldPub = kpg.getStoredIdentityPublicKey() ?: kpg.generateIdentityKeyPair()
        val newPub = ByteArray(32) { 0x5A }

        val cert = SuccessionCertificate.create(
            oldIdentityPubKey = oldPub,
            newIdentityPubKey = newPub,
            keyPairGenerator = kpg
        )

        val contacts = listOf(
            `in`.grayscales.entangl.core.crypto.ContactMigrationItem(
                uid = "peer-alice",
                publicKey = ByteArray(32) { 0x11 },
                onionAddress = "alice123456789.onion",
                safetyNumber = "12345-67890",
                displayName = "Alice",
                createdAt = 1000000L,
                lastSeenAt = 2000000L,
                isAccepted = true,
                profileColor = "#00FF9D"
            )
        )

        val messages = listOf(
            `in`.grayscales.entangl.core.crypto.MessageMigrationItem(
                id = "msg-001",
                contactUid = "peer-alice",
                ciphertext = "encrypted-bytes".encodeToByteArray(),
                direction = 1,
                status = 2,
                timestamp = 1500000L,
                selfDestructAt = null
            )
        )

        val sessionKeys = listOf(
            `in`.grayscales.entangl.core.crypto.SessionKeyMigrationItem(
                contactUid = "peer-alice",
                sessionKey = ByteArray(32) { 0x99.toByte() }
            )
        )

        val migrationPayload = `in`.grayscales.entangl.core.crypto.DeviceMigrationPayload(
            senderUid = "my-local-uid",
            contacts = contacts,
            messages = messages,
            sessionKeys = sessionKeys,
            certificate = cert,
            localUsername = "QuantumGhost",
            localProfileColor = "#FF007F"
        )

        // 1. CBOR Serialization
        val cborBytes = migrationPayload.toCbor()
        assertTrue("CBOR payload must not be empty", cborBytes.isNotEmpty())

        // 2. AES-256-GCM AEAD Tunnel Encryption
        val tunnelKey = ByteArray(32) { 0x42 }
        val aad = "entangl-device-migration-v1".encodeToByteArray()
        val encrypted = AeadCipher.encrypt(tunnelKey, cborBytes, aad)

        assertTrue("Encrypted size must include IV + ciphertext + GCM tag", encrypted.size > cborBytes.size)

        // 3. AES-256-GCM AEAD Tunnel Decryption
        val decryptedCbor = AeadCipher.decrypt(tunnelKey, encrypted, aad)
        assertArrayEquals(cborBytes, decryptedCbor)

        // 4. CBOR Deserialization
        val restored = `in`.grayscales.entangl.core.crypto.DeviceMigrationPayload.fromCbor(decryptedCbor)
        assertNotNull("Restored payload must not be null", restored)
        assertEquals("my-local-uid", restored!!.senderUid)
        assertEquals("QuantumGhost", restored.localUsername)
        assertEquals("#FF007F", restored.localProfileColor)
        assertEquals(1, restored.contacts.size)
        assertEquals("peer-alice", restored.contacts[0].uid)
        assertEquals("Alice", restored.contacts[0].displayName)
        assertEquals("#00FF9D", restored.contacts[0].profileColor)
        assertEquals(1, restored.messages.size)
        assertEquals("msg-001", restored.messages[0].id)
        assertEquals(1, restored.sessionKeys.size)
        assertEquals("peer-alice", restored.sessionKeys[0].contactUid)
        assertNotNull(restored.certificate)
        assertTrue(SuccessionCertificate.verify(restored.certificate!!, kpg))
    }

    @Test
    fun testCryptoManagerIdentityPublicKeyMatchesKeystoreAndVerifiesEnvelope() = runBlocking {
        val kpg = KeyPairGenerator()
        val expectedIdentityPub = kpg.getStoredIdentityPublicKey() ?: kpg.generateIdentityKeyPair()

        val cryptoManager = DefaultCryptoManager(
            keyPairGenerator = kpg,
            ratchetStateVerifier = dummyRatchetVerifier
        )

        // Initialize session with a peer (which sets up local DH keys)
        val peerPub = ByteArray(32) { 0x33 }
        cryptoManager.initializeSession("peer-alice", peerPub, "alice.onion")

        // getLocalIdentityPublicKey must return the Keystore Identity Key, NOT the ephemeral DH key
        val identityPub = cryptoManager.getLocalIdentityPublicKey()
        assertNotNull(identityPub)
        assertArrayEquals("Identity pub key must match Keystore identity key", expectedIdentityPub, identityPub)

        // Test envelope signing with this identity pub key
        val transport = NetworkTransport(kpg)
        val envelope = TransportEnvelope(
            id = "msg-auth-1",
            type = TransportEnvelope.TYPE_MESSAGE,
            senderUid = "node-local",
            senderIdentityPub = identityPub!!,
            senderOnion = "local.onion",
            recipientUid = "peer-alice",
            ciphertext = "hello world".encodeToByteArray()
        )

        val signed = transport.signEnvelope(envelope)
        assertTrue(signed.signature.isNotEmpty())
        assertArrayEquals(expectedIdentityPub, signed.senderIdentityPub)

        val isValid = kpg.verify(
            publicKey = signed.senderIdentityPub,
            data = signed.getCanonicalData(),
            signature = signed.signature
        )
        assertTrue("Signature verification must succeed when using identity pub from cryptoManager", isValid)
    }

    @Test
    fun testDirectMessageEnvelopeSizeWithinNtfyCeiling() {
        val kpg = KeyPairGenerator()
        val transport = NetworkTransport(kpg)

        val plaintext = "Hello! Direct message without physical scanning.".encodeToByteArray()
        val paddedCiphertext = TransportEnvelope.padPayload(plaintext, 2048)

        val envelope = TransportEnvelope(
            id = "msg-direct-100",
            type = TransportEnvelope.TYPE_MESSAGE,
            senderUid = "node-alice-direct",
            senderUsername = "Alice",
            senderProfileColor = "#00F0FF",
            senderIdentityPub = kpg.generateIdentityKeyPair(),
            senderOnion = "alice777777777777777777777777777777777777777777777777777.onion",
            recipientUid = "node-bob-direct",
            ciphertext = paddedCiphertext,
            timestamp = System.currentTimeMillis()
        )

        val signed = transport.signEnvelope(envelope)
        val json = signed.toJson()

        // Ntfy's hard limit is 4095 characters. The envelope must fit with margin to spare.
        assertTrue("Envelope JSON length (${json.length}) must be <= 4000 characters for ntfy relay", json.length <= 4000)

        // Deserialization must successfully recreate the envelope and unpad the payload
        val deserialized = TransportEnvelope.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("msg-direct-100", deserialized!!.id)
        assertEquals(TransportEnvelope.TYPE_MESSAGE, deserialized.type)
        assertEquals("node-bob-direct", deserialized.recipientUid)

        val unpadded = TransportEnvelope.unpadPayload(deserialized.ciphertext)
        assertArrayEquals(plaintext, unpadded)

        // Signature verification must pass
        val sigValid = kpg.verify(
            publicKey = deserialized.senderIdentityPub,
            data = deserialized.getCanonicalData(),
            signature = deserialized.signature
        )
        assertTrue("Signature must be valid on deserialized envelope", sigValid)
    }

    @Test
    fun testTwoEndedMutualSessionEncryptionAndDecryption() = runBlocking {
        val aliceKpg = KeyPairGenerator()
        val bobKpg = KeyPairGenerator()

        val aliceCrypto = DefaultCryptoManager(aliceKpg, dummyRatchetVerifier)
        val bobCrypto = DefaultCryptoManager(bobKpg, dummyRatchetVerifier)

        val alicePub = aliceCrypto.getLocalIdentityPublicKey()!!
        val bobPub = bobCrypto.getLocalIdentityPublicKey()!!

        val aliceUid = "alice-node-uid"
        val bobUid = "bob-node-uid"

        // 1. Alice scans Bob's QR code first
        aliceCrypto.initializeSession(bobUid, bobPub, "bob.onion")

        // 2. Alice sends an encrypted message to Bob
        val secretMessage = "Hello Bob! Transmitted after QR scan."
        val ciphertextFromAlice = aliceCrypto.encryptMessage(bobUid, secretMessage.encodeToByteArray())
        assertNotNull(ciphertextFromAlice)

        // 3. Before Bob has scanned Alice's QR code, Bob cannot decrypt the message
        try {
            bobCrypto.decryptMessage(aliceUid, ciphertextFromAlice)
            fail("Bob should not be able to decrypt before scanning Alice's QR code / initializing session")
        } catch (_: Exception) {
            // Expected: shows as "Encrypted message" in the UI
        }

        // 4. Bob scans Alice's QR code (from Bob's end)
        bobCrypto.initializeSession(aliceUid, alicePub, "alice.onion")

        // 5. Now Bob decrypts the pending message
        val decryptedByBob = bobCrypto.decryptMessage(aliceUid, ciphertextFromAlice)
        assertEquals(secretMessage, decryptedByBob.decodeToString())

        // 6. Bob replies to Alice
        val replyMessage = "Hello Alice! Scanned your QR and decrypted successfully."
        val ciphertextFromBob = bobCrypto.encryptMessage(aliceUid, replyMessage.encodeToByteArray())

        // 7. Alice decrypts Bob's reply
        val decryptedByAlice = aliceCrypto.decryptMessage(bobUid, ciphertextFromBob)
        assertEquals(replyMessage, decryptedByAlice.decodeToString())
    }
}

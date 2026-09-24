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
import org.junit.Assert.assertNotEquals
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
}

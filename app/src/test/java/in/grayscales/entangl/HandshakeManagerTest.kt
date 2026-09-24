package `in`.grayscales.entangl

import `in`.grayscales.entangl.core.crypto.DefaultCryptoManager
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.core.crypto.HandshakeVerificationResult
import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import `in`.grayscales.entangl.core.crypto.RatchetStateVerifier
import `in`.grayscales.entangl.core.util.currentTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HandshakeManagerTest {

    private lateinit var kpgA: KeyPairGenerator
    private lateinit var kpgB: KeyPairGenerator
    private lateinit var handshakeManagerA: HandshakeManager
    private lateinit var handshakeManagerB: HandshakeManager

    private val dummyVerifier = object : RatchetStateVerifier {
        override fun computeAndStoreHmac(contactUid: String, ratchetState: ByteArray) {}
        override fun verify(contactUid: String, ratchetState: ByteArray): Boolean = true
        override fun deleteHmac(contactUid: String) {}
    }

    @Before
    fun setUp() {
        kpgA = KeyPairGenerator()
        kpgB = KeyPairGenerator()

        val cryptoManagerA = DefaultCryptoManager(kpgA, dummyVerifier)
        val cryptoManagerB = DefaultCryptoManager(kpgB, dummyVerifier)

        handshakeManagerA = HandshakeManager(kpgA, cryptoManagerA)
        handshakeManagerB = HandshakeManager(kpgB, cryptoManagerB)
    }

    @Test
    fun testGenerateInitiatorPayload() {
        val payload = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00F0FF"
        )

        assertEquals("INITIATE", payload.action)
        assertEquals("node-alpha-1", payload.uid)
        assertEquals("AliceNode", payload.username)
        assertEquals("#00F0FF", payload.profileColor)
        assertEquals("alpha123456789.onion", payload.onion)
        assertEquals(32, payload.nonce.size)
        assertTrue(payload.ephPub.size >= 32)
        assertTrue(payload.signature.isNotEmpty())

        val now = currentTimeMillis()
        assertTrue(payload.timestamp in (now - 5000)..now)
    }

    @Test
    fun testGenerateConfirmPayload() {
        val initiatorNonce = ByteArray(32) { (it * 2).toByte() }
        val payload = handshakeManagerB.generateConfirmPayload(
            localUid = "node-beta-2",
            localOnion = "beta987654321.onion",
            peerNonce = initiatorNonce,
            username = "BobNode",
            profileColor = "#FF007F"
        )

        assertEquals("CONFIRM", payload.action)
        assertEquals("node-beta-2", payload.uid)
        assertEquals("BobNode", payload.username)
        assertEquals("#FF007F", payload.profileColor)
        assertEquals("beta987654321.onion", payload.onion)
        assertEquals(32, payload.nonce.size)
        assertTrue(payload.nonce.contentEquals(initiatorNonce))
        assertTrue(payload.ephPub.size >= 32)
        assertTrue(payload.signature.isNotEmpty())
    }

    @Test
    fun testVerifyPeerPayloadSuccess() {
        val payloadA = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00FF9D"
        )

        val result = handshakeManagerB.verifyPeerPayload(payloadA)
        assertTrue("Verification must succeed", result is HandshakeVerificationResult.Success)

        val success = result as HandshakeVerificationResult.Success
        assertEquals("node-alpha-1", success.peerUid)
        assertEquals("AliceNode", success.peerUsername)
        assertEquals("#00FF9D", success.peerProfileColor)
        assertEquals("alpha123456789.onion", success.peerOnion)
        assertNotNull(success.safetyNumber)
        assertEquals(60, success.safetyNumber.replace(" ", "").length)
    }

    @Test
    fun testVerifyPeerPayloadTamperedProfileColorFails() {
        val payloadA = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00F0FF"
        )

        // Attacker intercepts and modifies profileColor
        val tamperedPayload = payloadA.copy(profileColor = "#FF003C")
        val result = handshakeManagerB.verifyPeerPayload(tamperedPayload)

        assertEquals(
            "Tampered profileColor must fail cryptographic signature verification",
            HandshakeVerificationResult.InvalidSignature,
            result
        )
    }

    @Test
    fun testVerifyPeerPayloadTamperedUsernameFails() {
        val payloadA = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00F0FF"
        )

        val tamperedPayload = payloadA.copy(username = "ImposterAlice")
        val result = handshakeManagerB.verifyPeerPayload(tamperedPayload)

        assertEquals(
            "Tampered username must fail cryptographic signature verification",
            HandshakeVerificationResult.InvalidSignature,
            result
        )
    }

    @Test
    fun testVerifyPeerPayloadExpiredFails() {
        val oldPayload = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00F0FF"
        ).copy(timestamp = currentTimeMillis() - 70_000L) // 70 seconds old (TTL is 60s)

        val result = handshakeManagerB.verifyPeerPayload(oldPayload)
        assertTrue("Expired payload must return Expired result", result is HandshakeVerificationResult.Expired)
    }

    @Test
    fun testVerifyPeerPayloadFutureTimestampFails() {
        val futurePayload = handshakeManagerA.generateInitiatorPayload(
            localUid = "node-alpha-1",
            localOnion = "alpha123456789.onion",
            username = "AliceNode",
            profileColor = "#00F0FF"
        ).copy(timestamp = currentTimeMillis() + 30_000L) // 30 seconds into future

        val result = handshakeManagerB.verifyPeerPayload(futurePayload)
        assertTrue("Future timestamp payload must return Expired result", result is HandshakeVerificationResult.Expired)
    }
}

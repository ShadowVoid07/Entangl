package `in`.grayscales.entangl

import `in`.grayscales.entangl.data.network.NetworkTransport
import `in`.grayscales.entangl.data.network.TransportEnvelope
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTransportTest {

    @Test
    fun testEnvelopeJsonSerializationRoundTrip() {
        val original = TransportEnvelope(
            id = "msg-uuid-12345",
            type = TransportEnvelope.TYPE_MESSAGE,
            senderUid = "node-alice-001",
            senderIdentityPub = ByteArray(32) { (it * 3).toByte() },
            senderOnion = "aliceonionaddress1234567890abcdef.onion",
            recipientUid = "node-bob-002",
            ciphertext = "Secret encrypted payload with ChaCha20-Poly1305".encodeToByteArray(),
            timestamp = 1711200000000L
        )

        val json = original.toJson()
        println("Generated JSON envelope: $json")
        assertTrue(json.contains("msg-uuid-12345"))
        assertTrue(json.contains("node-alice-001"))
        assertTrue(json.contains("node-bob-002"))

        val deserialized = TransportEnvelope.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(original.id, deserialized!!.id)
        assertEquals(original.type, deserialized.type)
        assertEquals(original.senderUid, deserialized.senderUid)
        assertArrayEquals(original.senderIdentityPub, deserialized.senderIdentityPub)
        assertEquals(original.senderOnion, deserialized.senderOnion)
        assertEquals(original.recipientUid, deserialized.recipientUid)
        assertArrayEquals(original.ciphertext, deserialized.ciphertext)
        assertEquals(original.timestamp, deserialized.timestamp)
    }

    @Test
    fun testDeliveryAckEnvelopeRoundTrip() {
        val ack = TransportEnvelope(
            id = "msg-uuid-99999",
            type = TransportEnvelope.TYPE_DELIVERY_ACK,
            senderUid = "node-bob-002",
            senderIdentityPub = ByteArray(0),
            senderOnion = "",
            recipientUid = "node-alice-001",
            ciphertext = ByteArray(0),
            timestamp = System.currentTimeMillis()
        )

        val json = ack.toJson()
        val deserialized = TransportEnvelope.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(TransportEnvelope.TYPE_DELIVERY_ACK, deserialized!!.type)
        assertEquals("msg-uuid-99999", deserialized.id)
        assertEquals("node-bob-002", deserialized.senderUid)
        assertEquals("node-alice-001", deserialized.recipientUid)
    }

    @Test
    fun testTopicDerivationConsistency() {
        val transport = NetworkTransport()
        val topic1 = transport.getTopicForUid("node-alice-001")
        val topic2 = transport.getTopicForUid("node-alice-001")
        val topicBob = transport.getTopicForUid("node-bob-002")

        assertEquals(topic1, topic2)
        assertTrue(topic1.startsWith("entangl-v2-"))
        assertTrue(topicBob.startsWith("entangl-v2-"))
        org.junit.Assert.assertNotEquals(topic1, topicBob)
    }

    @Test
    fun testScanPingEnvelopeRoundTrip() {
        val ping = TransportEnvelope(
            id = "ping-12345",
            type = TransportEnvelope.TYPE_SCAN_PING,
            senderUid = "node-alice-001",
            senderUsername = "CipherAlice",
            senderIdentityPub = ByteArray(32) { 0x01 },
            senderOnion = "alice.onion",
            recipientUid = "node-bob-002",
            ciphertext = ByteArray(0),
            timestamp = 1711200000000L
        )

        val json = ping.toJson()
        val deserialized = TransportEnvelope.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(TransportEnvelope.TYPE_SCAN_PING, deserialized!!.type)
        assertEquals("CipherAlice", deserialized.senderUsername)
        assertEquals("node-alice-001", deserialized.senderUid)
        assertEquals("node-bob-002", deserialized.recipientUid)
        assertArrayEquals(ByteArray(32) { 0x01 }, deserialized.senderIdentityPub)
    }

    @Test
    fun testScanAcceptEnvelopeRoundTrip() {
        val accept = TransportEnvelope(
            id = "accept-12345",
            type = TransportEnvelope.TYPE_SCAN_ACCEPT,
            senderUid = "node-bob-002",
            senderUsername = "AgentBob",
            senderIdentityPub = ByteArray(0),
            senderOnion = "",
            recipientUid = "node-alice-001",
            ciphertext = ByteArray(0),
            timestamp = 1711200010000L
        )

        val json = accept.toJson()
        val deserialized = TransportEnvelope.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(TransportEnvelope.TYPE_SCAN_ACCEPT, deserialized!!.type)
        assertEquals("AgentBob", deserialized.senderUsername)
        assertEquals("node-bob-002", deserialized.senderUid)
        assertEquals("node-alice-001", deserialized.recipientUid)
    }

    @Test
    fun testMultiRelayPoolConfiguration() {
        val transport = NetworkTransport()
        assertEquals("https://ntfy.tedomum.fr", transport.activeRelay)
        assertTrue(NetworkTransport.RELAY_SERVERS.contains("https://ntfy.adminforge.de"))
        assertTrue(NetworkTransport.RELAY_SERVERS.contains("https://ntfy.tedomum.fr"))
        assertTrue(NetworkTransport.RELAY_SERVERS.contains("https://ntfy.envs.net"))
    }
}

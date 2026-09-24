package `in`.grayscales.entangl.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandshakePayloadTest {

    @Test
    fun testCborSerializationRoundTrip() {
        val payload = HandshakePayload(
            v = 1,
            action = "INITIATE",
            uid = "alice-uid-1234",
            username = "QuantumAlice",
            ephPub = ByteArray(32) { it.toByte() },
            identityPub = ByteArray(32) { (it + 10).toByte() },
            onion = "v3onionaddress56characterslongexampletestsampleaddress1.onion",
            nonce = ByteArray(32) { (it * 3).toByte() },
            timestamp = 1711200000000L,
            signature = ByteArray(64) { 0x5A }
        )

        val cborBytes = payload.toCbor()
        assertTrue(cborBytes.isNotEmpty())

        val deserialized = HandshakePayload.fromCbor(cborBytes)
        assertEquals(payload, deserialized)
        assertEquals("QuantumAlice", deserialized.username)
    }

    @Test
    fun testQrStringRoundTrip() {
        val payload = HandshakePayload(
            v = 1,
            action = "CONFIRM",
            uid = "bob-uid-5678",
            username = "AgentBob",
            ephPub = ByteArray(32) { (it + 5).toByte() },
            identityPub = ByteArray(32) { (it + 20).toByte() },
            onion = "bobonionaddress56characterslongexampletestsampleaddr2.onion",
            nonce = ByteArray(32) { 0x7F },
            timestamp = 1711200060000L,
            signature = ByteArray(64) { 0x3C }
        )

        val qrString = payload.toQrString()
        assertTrue(qrString.isNotEmpty())
        assertFalse(qrString.contains(" "))

        val deserialized = HandshakePayload.fromQrString(qrString)
        assertEquals(payload, deserialized)
        assertEquals("AgentBob", deserialized.username)
    }

    @Test
    fun testCanonicalDataChangesWithFields() {
        val payload1 = HandshakePayload(
            v = 1,
            action = "INITIATE",
            uid = "uid-1",
            username = "Alice",
            ephPub = ByteArray(32) { 1 },
            identityPub = ByteArray(32) { 2 },
            onion = "test.onion",
            nonce = ByteArray(32) { 3 },
            timestamp = 1000L,
            signature = ByteArray(0)
        )

        val payload2 = payload1.copy(timestamp = 2000L)
        assertFalse(payload1.getCanonicalData().contentEquals(payload2.getCanonicalData()))

        val payload3 = payload1.copy(username = "Bob")
        assertFalse(payload1.getCanonicalData().contentEquals(payload3.getCanonicalData()))
    }
}

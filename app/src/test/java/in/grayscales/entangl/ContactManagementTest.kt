package `in`.grayscales.entangl

import `in`.grayscales.entangl.data.local.entity.ContactEntity
import `in`.grayscales.entangl.domain.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactManagementTest {

    private fun sampleContact() = Contact(
        uid = "node-test-01",
        publicKey = ByteArray(32) { it.toByte() },
        onionAddress = "mesh-test.entangl.net",
        safetyNumber = "00000 00000",
        displayName = "Tester",
        createdAt = 1000L,
        lastSeenAt = 2000L,
        isAccepted = true,
        profileColor = "#00F0FF",
        hasScannedPeer = true,
        hasBeenScanned = true
    )

    @Test
    fun testBlockDefaultsFalse() {
        assertFalse(sampleContact().isBlocked)
    }

    @Test
    fun testBlockTogglePreservesHandshakeState() {
        val blocked = sampleContact().copy(isBlocked = true)
        assertTrue(blocked.isBlocked)
        assertTrue(blocked.isAccepted)
        assertTrue(blocked.hasScannedPeer && blocked.hasBeenScanned)
        assertFalse(blocked.copy(isBlocked = false).isBlocked)
    }

    @Test
    fun testEntityRoundTripPreservesBlockFlag() {
        val blocked = sampleContact().copy(isBlocked = true)
        val restored = ContactEntity.fromDomain(blocked).toDomain()
        assertEquals(blocked, restored)
        assertTrue(restored.isBlocked)
    }
}

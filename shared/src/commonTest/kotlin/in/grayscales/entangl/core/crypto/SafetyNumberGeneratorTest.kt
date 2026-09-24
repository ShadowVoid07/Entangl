package `in`.grayscales.entangl.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SafetyNumberGeneratorTest {

    @Test
    fun testSafetyNumberIsDeterministicAndCommutative() {
        val alicePubKey = ByteArray(32) { (it * 3).toByte() }
        val bobPubKey = ByteArray(32) { (it * 7).toByte() }

        // Alice computes safety number with Bob
        val safetyNumberAlice = SafetyNumberGenerator.generate(alicePubKey, bobPubKey)

        // Bob computes safety number with Alice
        val safetyNumberBob = SafetyNumberGenerator.generate(bobPubKey, alicePubKey)

        // Both peers must derive the EXACT SAME safety number
        assertEquals(safetyNumberAlice, safetyNumberBob)

        // Safety number must have 12 groups of 5 digits separated by spaces (71 chars total)
        assertEquals(71, safetyNumberAlice.length)
        val groups = safetyNumberAlice.split(" ")
        assertEquals(12, groups.size)
        groups.forEach { group ->
            assertEquals(5, group.length)
            assertTrue(group.all { it.isDigit() })
        }
    }

    @Test
    fun testMitmAttackChangesSafetyNumber() {
        val alicePubKey = ByteArray(32) { 1 }
        val bobPubKey = ByteArray(32) { 2 }
        val evePubKey = ByteArray(32) { 9 }

        val legitSafetyNumber = SafetyNumberGenerator.generate(alicePubKey, bobPubKey)
        val mitmSafetyNumber = SafetyNumberGenerator.generate(alicePubKey, evePubKey)

        assertNotEquals(legitSafetyNumber, mitmSafetyNumber)
    }
}

package `in`.grayscales.entangl.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ByteArrayExtTest {

    @Test
    fun testZeroizeWipesMemory() {
        val sensitiveKey = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        sensitiveKey.zeroize()
        assertTrue(sensitiveKey.all { it == 0.toByte() })
    }

    @Test
    fun testConstantTimeEquals() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(1, 2, 3, 4)
        val c = byteArrayOf(1, 2, 3, 5)
        val d = byteArrayOf(1, 2, 3)

        assertTrue(a.constantTimeEquals(b))
        assertFalse(a.constantTimeEquals(c))
        assertFalse(a.constantTimeEquals(d))
    }

    @Test
    fun testHexConversion() {
        val bytes = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)
        val hex = bytes.toHexString()
        assertEquals("0a0b0c0d", hex)

        val restored = hex.hexToByteArray()
        assertTrue(bytes.constantTimeEquals(restored))
    }
}

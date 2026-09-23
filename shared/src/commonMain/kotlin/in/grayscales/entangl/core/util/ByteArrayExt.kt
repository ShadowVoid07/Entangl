package `in`.grayscales.entangl.core.util

/**
 * Secure extension functions for ByteArray.
 * All operations are designed with key material safety in mind.
 */

/**
 * Zeroize the contents of this ByteArray in-place.
 * Use this to wipe any ByteArray that held key material.
 *
 * NOTE: This is a best-effort JVM wipe. For guaranteed zeroization,
 * use [NativeKeyBuffer] which operates on native memory.
 */
fun ByteArray.zeroize() {
    this.fill(0)
}

/**
 * Convert to lowercase hex string.
 * Safe for logging PUBLIC keys only. NEVER use on private key material.
 */
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

/**
 * Decode a hex string back to ByteArray.
 */
fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/**
 * Constant-time comparison to prevent timing side-channel attacks.
 * Returns true if both arrays have the same length and identical contents.
 */
fun ByteArray.constantTimeEquals(other: ByteArray): Boolean {
    if (this.size != other.size) return false
    var result = 0
    for (i in indices) {
        result = result or (this[i].toInt() xor other[i].toInt())
    }
    return result == 0
}

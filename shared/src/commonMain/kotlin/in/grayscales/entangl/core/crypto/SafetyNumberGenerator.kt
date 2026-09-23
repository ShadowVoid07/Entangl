package `in`.grayscales.entangl.core.crypto

/**
 * Generates a Safety Number for post-connection MITM verification.
 *
 * The Safety Number is a 60-digit numeric fingerprint derived from both
 * peers' identity public keys. If an attacker performed a MITM during
 * the QR handshake (physically near-impossible, but defense-in-depth),
 * the Safety Numbers displayed on each device will NOT match.
 *
 * Algorithm:
 *   1. Sort both public keys lexicographically.
 *   2. Concatenate: sorted_key_1 || sorted_key_2
 *   3. SHA-256 hash the concatenation (5 iterations for stretching).
 *   4. Truncate the hash to 30 bytes.
 *   5. Convert each byte to a 2-digit decimal: byte mod 100 → "00"-"99".
 *   6. Format as 12 groups of 5 digits separated by spaces.
 *
 * This is a pure Kotlin implementation — no platform dependencies.
 */
object SafetyNumberGenerator {

    private const val HASH_ITERATIONS = 5
    private const val FINGERPRINT_BYTES = 30
    private const val DIGITS_PER_GROUP = 5
    private const val TOTAL_GROUPS = 12

    /**
     * Generate a Safety Number string from two identity public keys.
     *
     * @param localPublicKey The local device's Ed25519 identity public key.
     * @param remotePublicKey The peer's Ed25519 identity public key.
     * @return A 60-digit numeric string formatted as "XXXXX XXXXX XXXXX ..." (12 groups).
     */
    fun generate(localPublicKey: ByteArray, remotePublicKey: ByteArray): String {
        // 1. Sort keys lexicographically for deterministic ordering
        val (first, second) = if (localPublicKey.compareLexicographically(remotePublicKey) <= 0) {
            localPublicKey to remotePublicKey
        } else {
            remotePublicKey to localPublicKey
        }

        // 2. Concatenate sorted keys
        val combined = first + second

        // 3. Iterative SHA-256 hash
        var hash = sha256(combined)
        repeat(HASH_ITERATIONS - 1) {
            hash = sha256(hash)
        }

        // 4. Truncate to FINGERPRINT_BYTES
        val truncated = hash.copyOf(FINGERPRINT_BYTES)

        // 5. Convert to decimal digits
        val digits = buildString {
            for (byte in truncated) {
                val value = (byte.toInt() and 0xFF) % 100
                append(value.toString().padStart(2, '0'))
            }
        }

        // 6. Format as groups of 5
        return digits.chunked(DIGITS_PER_GROUP)
            .take(TOTAL_GROUPS)
            .joinToString(" ")
    }

    /**
     * Minimal platform-independent SHA-256 implementation.
     * Uses java.security.MessageDigest on JVM/Android targets.
     * For iOS, this would need an expect/actual or a KMP crypto lib.
     *
     * NOTE: This is acceptable because SafetyNumberGenerator only hashes
     * public keys (non-secret data). No key material passes through here.
     */
    private fun sha256(input: ByteArray): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }

    /**
     * Lexicographic comparison of two byte arrays.
     */
    private fun ByteArray.compareLexicographically(other: ByteArray): Int {
        val minLen = minOf(this.size, other.size)
        for (i in 0 until minLen) {
            val cmp = (this[i].toInt() and 0xFF) - (other[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return this.size - other.size
    }
}

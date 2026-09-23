package `in`.grayscales.entangl.core.crypto

/**
 * Verifies the integrity of the Double Ratchet session state using
 * a platform-specific HMAC stored in secure hardware.
 *
 * Android: HMAC key in Android Keystore (hardware-backed).
 * iOS: HMAC key in Keychain with Secure Enclave protection.
 *
 * This prevents ratchet state rollback attacks where an attacker replaces
 * the serialized ratchet state with an older copy to force key reuse.
 */
interface RatchetStateVerifier {

    /**
     * Compute HMAC-SHA256 of the serialized ratchet state and store it
     * in the platform's secure storage, keyed by contact UID.
     *
     * @param contactUid Unique identifier for the peer session.
     * @param ratchetState The serialized ratchet state bytes.
     */
    fun computeAndStoreHmac(contactUid: String, ratchetState: ByteArray)

    /**
     * Verify the current ratchet state against the stored HMAC.
     *
     * @param contactUid Unique identifier for the peer session.
     * @param ratchetState The serialized ratchet state bytes to verify.
     * @return true if HMAC matches (state is intact), false if tampered.
     */
    fun verify(contactUid: String, ratchetState: ByteArray): Boolean

    /**
     * Delete the stored HMAC for a contact (used when session is destroyed).
     */
    fun deleteHmac(contactUid: String)
}

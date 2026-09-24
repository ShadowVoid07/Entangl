package `in`.grayscales.entangl.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Canonical payload transmitted via dynamic QR code during the Mutual Handshake.
 * Serialized with compact CBOR encoding to ensure fast optical scan acquisition.
 *
 * @param v Protocol version (default 1).
 * @param action Handshake stage: "INITIATE" (QR-1) or "CONFIRM" (QR-2).
 * @param uid Unique ephemeral or identity UID for the initiator.
 * @param ephPub Ephemeral X25519 public key (32 bytes) for PQXDH establishment.
 * @param identityPub Ed25519 identity public key (32 bytes) for trust verification.
 * @param onion Peer Tor v3 .onion address (56 chars + .onion).
 * @param nonce 32-byte cryptographically secure rolling nonce.
 * @param timestamp Epoch millis at generation for TTL validation (60-second validity).
 * @param signature Ed25519 signature over canonical payload bytes using identity key.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
@Serializable
data class HandshakePayload(
    val v: Int = 1,
    val action: String,
    val uid: String,
    val username: String = "",
    val profileColor: String = "",
    val ephPub: ByteArray,
    val identityPub: ByteArray,
    val onion: String,
    val nonce: ByteArray,
    val timestamp: Long,
    val signature: ByteArray
) {
    /**
     * Derives canonical byte representation of all signed fields.
     * The signature itself is excluded from this canonical sequence.
     */
    fun getCanonicalData(): ByteArray {
        val timestampBytes = ByteArray(8) { i ->
            (timestamp ushr (56 - i * 8)).toByte()
        }
        return action.encodeToByteArray() +
            uid.encodeToByteArray() +
            username.encodeToByteArray() +
            profileColor.encodeToByteArray() +
            ephPub +
            identityPub +
            onion.encodeToByteArray() +
            nonce +
            timestampBytes
    }

    /**
     * Serializes this payload into a compact CBOR byte array.
     */
    fun toCbor(): ByteArray = Cbor.encodeToByteArray(serializer(), this)

    /**
     * Encodes this payload to a URL-safe Base64 string for QR code generation.
     */
    fun toQrString(): String = Base64.UrlSafe.encode(toCbor())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandshakePayload) return false

        if (v != other.v) return false
        if (action != other.action) return false
        if (uid != other.uid) return false
        if (username != other.username) return false
        if (profileColor != other.profileColor) return false
        if (!ephPub.contentEquals(other.ephPub)) return false
        if (!identityPub.contentEquals(other.identityPub)) return false
        if (onion != other.onion) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (timestamp != other.timestamp) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = v
        result = 31 * result + action.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + profileColor.hashCode()
        result = 31 * result + ephPub.contentHashCode()
        result = 31 * result + identityPub.contentHashCode()
        result = 31 * result + onion.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }

    companion object {
        /**
         * Deserializes a CBOR byte array into a [HandshakePayload].
         */
        fun fromCbor(bytes: ByteArray): HandshakePayload =
            Cbor.decodeFromByteArray(serializer(), bytes)

        /**
         * Deserializes a URL-safe or standard Base64 QR code string into a [HandshakePayload].
         */
        fun fromQrString(qrText: String): HandshakePayload {
            val trimmed = qrText.trim()
            val bytes = try {
                Base64.UrlSafe.decode(trimmed)
            } catch (_: Exception) {
                Base64.decode(trimmed)
            }
            return fromCbor(bytes)
        }
    }
}

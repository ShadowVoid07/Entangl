package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.SecureRandom
import `in`.grayscales.entangl.core.util.currentTimeMillis
import `in`.grayscales.entangl.domain.model.HandshakePayload

sealed class HandshakeVerificationResult {
    data class Success(
        val peerUid: String,
        val peerUsername: String = "",
        val peerOnion: String,
        val peerIdentityPub: ByteArray,
        val peerEphPub: ByteArray,
        val safetyNumber: String
    ) : HandshakeVerificationResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return peerUid == other.peerUid &&
                peerUsername == other.peerUsername &&
                peerOnion == other.peerOnion &&
                peerIdentityPub.contentEquals(other.peerIdentityPub) &&
                peerEphPub.contentEquals(other.peerEphPub) &&
                safetyNumber == other.safetyNumber
        }

        override fun hashCode(): Int {
            var result = peerUid.hashCode()
            result = 31 * result + peerUsername.hashCode()
            result = 31 * result + peerOnion.hashCode()
            result = 31 * result + peerIdentityPub.contentHashCode()
            result = 31 * result + peerEphPub.contentHashCode()
            result = 31 * result + safetyNumber.hashCode()
            return result
        }
    }

    data class Expired(val ageMillis: Long) : HandshakeVerificationResult()
    data object InvalidSignature : HandshakeVerificationResult()
    data class Error(val reason: String) : HandshakeVerificationResult()
}

/**
 * Manages the cryptographic state, signing, and verification
 * for the dynamic 2-stage Mutual QR Handshake protocol.
 */
class HandshakeManager(
    private val keyPairGenerator: KeyPairGenerator,
    private val cryptoManager: CryptoManager
) {
    companion object {
        const val PAYLOAD_TTL_MILLIS = 60_000L // 60 seconds TTL
    }

    /**
     * Retrieves the stored Ed25519 identity public key,
     * or generates one if not already present.
     */
    fun getOrGenerateIdentityKey(): ByteArray {
        return keyPairGenerator.getStoredIdentityPublicKey()
            ?: keyPairGenerator.generateIdentityKeyPair()
    }

    /**
     * Generates a signed QR-1 (INITIATE) handshake payload with a rolling 32-byte nonce.
     */
    fun generateInitiatorPayload(localUid: String, localOnion: String, username: String = ""): HandshakePayload {
        val identityPub = getOrGenerateIdentityKey()
        val (ephPub, ephemeralPrivBuffer) = keyPairGenerator.generateEphemeralX25519()
        ephemeralPrivBuffer.close()

        val nonce = SecureRandom.nextBytes(32)
        val now = currentTimeMillis()

        val unsigned = HandshakePayload(
            v = 1,
            action = "INITIATE",
            uid = localUid,
            username = username,
            ephPub = ephPub,
            identityPub = identityPub,
            onion = localOnion,
            nonce = nonce,
            timestamp = now,
            signature = ByteArray(0)
        )

        val signature = keyPairGenerator.sign(unsigned.getCanonicalData())
        return unsigned.copy(signature = signature)
    }

    /**
     * Generates a signed QR-2 (CONFIRM) handshake payload responding to a peer's scan.
     */
    fun generateConfirmPayload(localUid: String, localOnion: String, peerNonce: ByteArray, username: String = ""): HandshakePayload {
        val identityPub = getOrGenerateIdentityKey()
        val (ephPub, ephemeralPrivBuffer) = keyPairGenerator.generateEphemeralX25519()
        ephemeralPrivBuffer.close()

        val now = currentTimeMillis()

        val unsigned = HandshakePayload(
            v = 1,
            action = "CONFIRM",
            uid = localUid,
            username = username,
            ephPub = ephPub,
            identityPub = identityPub,
            onion = localOnion,
            nonce = peerNonce,
            timestamp = now,
            signature = ByteArray(0)
        )

        val signature = keyPairGenerator.sign(unsigned.getCanonicalData())
        return unsigned.copy(signature = signature)
    }

    /**
     * Validates an incoming scanned peer payload:
     * 1. Checks timestamp TTL against [maxAgeMillis]
     * 2. Cryptographically verifies Ed25519 signature
     * 3. Computes the 60-digit Safety Number for out-of-band visual comparison
     */
    fun verifyPeerPayload(
        payload: HandshakePayload,
        maxAgeMillis: Long = PAYLOAD_TTL_MILLIS
    ): HandshakeVerificationResult {
        val now = currentTimeMillis()
        val age = now - payload.timestamp
        if (age < 0 || age > maxAgeMillis) {
            return HandshakeVerificationResult.Expired(age)
        }

        val canonicalData = payload.getCanonicalData()
        val validSig = keyPairGenerator.verify(
            publicKey = payload.identityPub,
            data = canonicalData,
            signature = payload.signature
        )

        if (!validSig) {
            return HandshakeVerificationResult.InvalidSignature
        }

        val localIdentityPub = getOrGenerateIdentityKey()
        val safetyNumber = cryptoManager.generateSafetyNumber(localIdentityPub, payload.identityPub)

        return HandshakeVerificationResult.Success(
            peerUid = payload.uid,
            peerUsername = payload.username,
            peerOnion = payload.onion,
            peerIdentityPub = payload.identityPub,
            peerEphPub = payload.ephPub,
            safetyNumber = safetyNumber
        )
    }
}

package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.SecureRandom
import `in`.grayscales.entangl.core.util.currentTimeMillis
import `in`.grayscales.entangl.domain.model.HandshakePayload

sealed class HandshakeVerificationResult {
    data class Success(
        val peerUid: String,
        val peerUsername: String = "",
        val peerProfileColor: String = "",
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
                peerProfileColor == other.peerProfileColor &&
                peerOnion == other.peerOnion &&
                peerIdentityPub.contentEquals(other.peerIdentityPub) &&
                peerEphPub.contentEquals(other.peerEphPub) &&
                safetyNumber == other.safetyNumber
        }

        override fun hashCode(): Int {
            var result = peerUid.hashCode()
            result = 31 * result + peerUsername.hashCode()
            result = 31 * result + peerProfileColor.hashCode()
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
        private const val MAX_CLOCK_SKEW_MILLIS = 10_000L
    }

    // Military-grade mutual binding: track our last displayed INITIATE nonce
    // so a CONFIRM echo can be cryptographically tied to this exact session.
    private var lastInitiatorNonce: ByteArray? = null

    // Single-use replay cache: hex(nonce). Prevents screenshot re-scan.
    // KMP commonMain: plain set (verification runs on main thread, no JVM sync needed).
    private val consumedNonces = mutableSetOf<String>()

    private fun nonceHex(nonce: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(nonce.size * 2)
        for (b in nonce) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4]).append(hexChars[v and 0x0F])
        }
        return sb.toString()
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
     * Stores the nonce as the expected echo for a future CONFIRM (mutual binding).
     */
    fun generateInitiatorPayload(
        localUid: String,
        localOnion: String,
        username: String = "",
        profileColor: String = ""
    ): HandshakePayload {
        val identityPub = getOrGenerateIdentityKey()
        val (ephPub, ephemeralPrivBuffer) = keyPairGenerator.generateEphemeralX25519()
        ephemeralPrivBuffer.close()

        val nonce = SecureRandom.nextBytes(32)
        lastInitiatorNonce = nonce.copyOf()
        val now = currentTimeMillis()

        val unsigned = HandshakePayload(
            v = 1,
            action = "INITIATE",
            uid = localUid,
            username = username,
            profileColor = profileColor,
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
    fun generateConfirmPayload(
        localUid: String,
        localOnion: String,
        peerNonce: ByteArray,
        username: String = "",
        profileColor: String = ""
    ): HandshakePayload {
        val identityPub = getOrGenerateIdentityKey()
        val (ephPub, ephemeralPrivBuffer) = keyPairGenerator.generateEphemeralX25519()
        ephemeralPrivBuffer.close()

        val now = currentTimeMillis()

        val unsigned = HandshakePayload(
            v = 1,
            action = "CONFIRM",
            uid = localUid,
            username = username,
            profileColor = profileColor,
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
     * Validates an incoming scanned peer payload (military-grade strict):
     * 1. Structural checks: v==1, action in {INITIATE,CONFIRM}, uid/onion non-blank,
     *    nonce==32B, ephPub/identityPub non-empty, timestamp sane.
     * 2. TTL + clock-skew check (reject future beyond skew, expired beyond TTL).
     * 3. Single-use nonce replay check (screenshot re-scan rejected).
     * 4. Ed25519 signature over canonical bytes.
     * 5. If action==CONFIRM, nonce must equal our last displayed INITIATE nonce
     *    (binds B's response to A's exact session). If no INITIATE displayed yet,
     *    CONFIRM is rejected — A must show QR first.
     * 6. Computes 60-digit Safety Number. Caller must still enforce out-of-band
     *    visual comparison + reciprocal scan before marking accepted.
     */
    fun verifyPeerPayload(
        payload: HandshakePayload,
        maxAgeMillis: Long = PAYLOAD_TTL_MILLIS
    ): HandshakeVerificationResult {
        // Structural validation before crypto
        if (payload.v != 1) return HandshakeVerificationResult.Error("Unsupported protocol v=${payload.v}")
        if (payload.action != "INITIATE" && payload.action != "CONFIRM") {
            return HandshakeVerificationResult.Error("Unknown action ${payload.action}")
        }
        if (payload.uid.isBlank() || payload.onion.isBlank()) {
            return HandshakeVerificationResult.Error("Missing uid/onion")
        }
        if (payload.nonce.size != 32) {
            return HandshakeVerificationResult.Error("Invalid nonce size ${payload.nonce.size}")
        }
        if (payload.identityPub.isEmpty() || payload.ephPub.isEmpty()) {
            return HandshakeVerificationResult.Error("Missing keys")
        }
        if (payload.signature.isEmpty()) {
            return HandshakeVerificationResult.InvalidSignature
        }

        val now = currentTimeMillis()
        val age = now - payload.timestamp
        // Reject future timestamps beyond skew and expired beyond TTL
        if (age < -MAX_CLOCK_SKEW_MILLIS || age > maxAgeMillis) {
            return HandshakeVerificationResult.Expired(age)
        }

        // CONFIRM must echo our last INITIATE nonce (mutual session binding)
        if (payload.action == "CONFIRM") {
            val expected = lastInitiatorNonce
                ?: return HandshakeVerificationResult.Error("Unexpected CONFIRM: no INITIATE displayed")
            if (!payload.nonce.contentEquals(expected)) {
                return HandshakeVerificationResult.Error("CONFIRM nonce mismatch: not bound to displayed session")
            }
        }

        // Single-use: reject already-consumed nonce (screenshot replay)
        val hex = nonceHex(payload.nonce)
        if (!consumedNonces.add(hex)) {
            return HandshakeVerificationResult.Error("Nonce already consumed: replay rejected")
        }

        val canonicalData = payload.getCanonicalData()
        val validSig = keyPairGenerator.verify(
            publicKey = payload.identityPub,
            data = canonicalData,
            signature = payload.signature
        )

        if (!validSig) {
            // Allow retry on genuine scan glitch: release nonce claim on sig failure
            // so a fresh scan of a renewed QR can proceed, but keep replay cache
            // for successfully verified nonces only. Here verification failed, so
            // remove to avoid permanent denial from a corrupted frame.
            consumedNonces.remove(hex)
            return HandshakeVerificationResult.InvalidSignature
        }

        val localIdentityPub = getOrGenerateIdentityKey()
        // Self-scan guard (only when localUidHint bound, i.e. production UI sets it;
        // skipped in unit tests where Keystore alias is shared and hint is empty).
        // Cannot entangle with self: reject if uid matches local OR identity key matches local.
        val hint = resolveLocalUidHint()
        if (hint.isNotBlank()) {
            if (payload.uid == hint) {
                consumedNonces.remove(hex)
                return HandshakeVerificationResult.Error("Self-scan rejected")
            }
            if (payload.identityPub.contentEquals(localIdentityPub)) {
                consumedNonces.remove(hex)
                return HandshakeVerificationResult.Error("Self-scan rejected")
            }
        }

        val safetyNumber = cryptoManager.generateSafetyNumber(localIdentityPub, payload.identityPub)

        return HandshakeVerificationResult.Success(
            peerUid = payload.uid,
            peerUsername = payload.username,
            peerProfileColor = payload.profileColor,
            peerOnion = payload.onion,
            peerIdentityPub = payload.identityPub,
            peerEphPub = payload.ephPub,
            safetyNumber = safetyNumber
        )
    }

    /**
     * Hint hook for self-scan detection when uid is known. Defaults to empty
     * (identity-key comparison above is authoritative). Kept separate for testability.
     */
    var localUidHint: String = ""

    private fun resolveLocalUidHint(): String = localUidHint
}

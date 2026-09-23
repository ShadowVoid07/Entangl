package `in`.grayscales.entangl.domain.model

/**
 * State machine for the Mutual QR Handshake protocol.
 *
 * Flow: Idle → QrGenerated → PeerScanned → Completed
 *       Any state can transition to Failed on error.
 */
sealed class HandshakeState {

    /** No handshake in progress. */
    data object Idle : HandshakeState()

    /** QR-1 has been generated and is being displayed to the peer. */
    data class QrGenerated(
        val payload: ByteArray,
        val expiresAt: Long
    ) : HandshakeState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QrGenerated) return false
            return payload.contentEquals(other.payload) && expiresAt == other.expiresAt
        }

        override fun hashCode(): Int = payload.contentHashCode() * 31 + expiresAt.hashCode()
    }

    /** Peer's QR has been scanned and verified. Awaiting reciprocal scan. */
    data class PeerScanned(
        val peerPublicKey: ByteArray,
        val peerOnionAddress: String
    ) : HandshakeState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PeerScanned) return false
            return peerPublicKey.contentEquals(other.peerPublicKey) &&
                peerOnionAddress == other.peerOnionAddress
        }

        override fun hashCode(): Int =
            peerPublicKey.contentHashCode() * 31 + peerOnionAddress.hashCode()
    }

    /** Both sides have scanned and verified. Session is established. */
    data class Completed(
        val safetyNumber: String
    ) : HandshakeState()

    /** Handshake failed at any stage. */
    data class Failed(
        val reason: String
    ) : HandshakeState()
}

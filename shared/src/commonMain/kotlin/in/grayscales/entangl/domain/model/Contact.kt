package `in`.grayscales.entangl.domain.model

/**
 * Represents a trusted peer contact established via the Mutual QR Handshake.
 * This is a pure domain model — no framework annotations.
 */
data class Contact(
    val uid: String,
    val publicKey: ByteArray,
    val onionAddress: String,
    val safetyNumber: String,
    val displayName: String?,
    val createdAt: Long,
    val lastSeenAt: Long?,
    val isAccepted: Boolean = false,
    val profileColor: String? = null,
    // Military-grade mutual handshake state: both directions must be optically verified.
    // hasScannedPeer = local user scanned peer QR (outbound). hasBeenScanned = peer
    // sent SCAN_PING proving they scanned our QR (inbound). isAccepted may only become
    // true when both are true AND safety number explicitly confirmed out-of-band.
    val hasScannedPeer: Boolean = false,
    val hasBeenScanned: Boolean = false,
    // Local-only mute: blocked peers cannot send or receive until unblocked.
    // Never transmitted; enforced in MessageRepositoryImpl.
    val isBlocked: Boolean = false
) {
    companion object {
        /**
         * Presence window: a peer counts as active now only with signature-verified
         * traffic inside this window. `lastSeenAt` advances solely on fresh,
         * verified inbound envelopes — never on our own sends, never on stale
         * backlog replays — so the dot reflects real current activity.
         */
        const val PRESENCE_ACTIVE_WINDOW_MS = 120_000L
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Contact) return false
        if (uid != other.uid) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (onionAddress != other.onionAddress) return false
        if (safetyNumber != other.safetyNumber) return false
        if (displayName != other.displayName) return false
        if (createdAt != other.createdAt) return false
        if (lastSeenAt != other.lastSeenAt) return false
        if (isAccepted != other.isAccepted) return false
        if (profileColor != other.profileColor) return false
        if (hasScannedPeer != other.hasScannedPeer) return false
        if (hasBeenScanned != other.hasBeenScanned) return false
        if (isBlocked != other.isBlocked) return false
        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + onionAddress.hashCode()
        result = 31 * result + safetyNumber.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (lastSeenAt?.hashCode() ?: 0)
        result = 31 * result + isAccepted.hashCode()
        result = 31 * result + (profileColor?.hashCode() ?: 0)
        result = 31 * result + hasScannedPeer.hashCode()
        result = 31 * result + hasBeenScanned.hashCode()
        result = 31 * result + isBlocked.hashCode()
        return result
    }
}

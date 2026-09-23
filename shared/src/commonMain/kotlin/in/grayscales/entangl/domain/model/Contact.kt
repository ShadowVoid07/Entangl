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
    val isAccepted: Boolean = false
) {
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
        return result
    }
}

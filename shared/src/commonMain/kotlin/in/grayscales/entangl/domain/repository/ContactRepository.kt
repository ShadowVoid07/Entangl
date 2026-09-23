package `in`.grayscales.entangl.domain.repository

import `in`.grayscales.entangl.domain.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing trusted peer contacts.
 * Defined in domain (commonMain), implemented in data layer (androidMain).
 */
interface ContactRepository {

    /** Persist a new contact after a successful QR handshake. */
    suspend fun save(contact: Contact)

    /** Retrieve a contact by their unique handshake ID. */
    suspend fun getByUid(uid: String): Contact?

    /** Observe all contacts as a reactive stream. */
    fun observeAll(): Flow<List<Contact>>

    /** Remove a contact and all associated session data. */
    suspend fun delete(uid: String)

    /** Update the last-seen timestamp for a contact. */
    suspend fun updateLastSeen(uid: String, timestamp: Long)

    /** Update connection acceptance status for a contact. */
    suspend fun updateAcceptance(uid: String, isAccepted: Boolean)
}

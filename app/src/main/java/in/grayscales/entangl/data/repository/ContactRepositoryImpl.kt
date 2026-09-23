package `in`.grayscales.entangl.data.repository

import `in`.grayscales.entangl.data.local.dao.ContactDao
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val contactDao: ContactDao
) : ContactRepository {

    override suspend fun save(contact: Contact) {
        contactDao.insertOrUpdate(ContactEntity.fromDomain(contact))
    }

    override suspend fun getByUid(uid: String): Contact? {
        return contactDao.getByUid(uid)?.toDomain()
    }

    override fun observeAll(): Flow<List<Contact>> {
        return contactDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun delete(uid: String) {
        contactDao.deleteByUid(uid)
    }

    override suspend fun updateLastSeen(uid: String, timestamp: Long) {
        contactDao.updateLastSeen(uid, timestamp)
    }

    override suspend fun updateAcceptance(uid: String, isAccepted: Boolean) {
        val existing = contactDao.getByUid(uid)
        if (existing != null) {
            contactDao.insertOrUpdate(existing.copy(isAccepted = isAccepted))
        } else {
            contactDao.updateAcceptance(uid, isAccepted)
        }
    }
}

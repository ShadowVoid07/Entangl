package `in`.grayscales.entangl.di

import `in`.grayscales.entangl.core.crypto.DefaultCryptoManager
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.core.identity.SessionKeyStore
import `in`.grayscales.entangl.data.local.EntanglDatabase
import `in`.grayscales.entangl.data.network.NetworkTransport
import `in`.grayscales.entangl.data.notification.EntanglNotificationManager
import `in`.grayscales.entangl.data.repository.ContactRepositoryImpl
import `in`.grayscales.entangl.data.repository.MessageRepositoryImpl
import `in`.grayscales.entangl.domain.repository.ContactRepository
import `in`.grayscales.entangl.domain.repository.MessageRepository
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for Android app dependencies (Room DB, DAOs, Repositories, Services, Identity, and ViewModels).
 */
val appModule = module {
    single { NodeIdentityManager(get()) }
    single { SessionKeyStore(get()) }
    single<DefaultCryptoManager.SessionKeyPersistence> { get<SessionKeyStore>() }
    single { NetworkTransport() }
    single { EntanglDatabase.getInstance(get()) }
    single { get<EntanglDatabase>().contactDao() }
    single { get<EntanglDatabase>().messageDao() }
    single<ContactRepository> { ContactRepositoryImpl(get()) }
    single<MessageRepository> { MessageRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single { EntanglNotificationManager(get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get()) }
}

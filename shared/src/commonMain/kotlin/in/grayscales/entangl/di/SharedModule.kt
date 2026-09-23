package `in`.grayscales.entangl.di

import `in`.grayscales.entangl.core.crypto.HandshakeManager
import org.koin.dsl.module

/**
 * Koin module for platform-agnostic dependencies defined in commonMain.
 */
val sharedModule = module {
    single { HandshakeManager(get(), get()) }
}

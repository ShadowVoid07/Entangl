package `in`.grayscales.entangl.di

import `in`.grayscales.entangl.core.crypto.AndroidRatchetStateVerifier
import `in`.grayscales.entangl.core.crypto.CryptoManager
import `in`.grayscales.entangl.core.crypto.DefaultCryptoManager
import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import `in`.grayscales.entangl.core.crypto.RatchetStateVerifier
import `in`.grayscales.entangl.core.security.AndroidPlatformSecurity
import `in`.grayscales.entangl.core.security.PlatformSecurity
import org.koin.dsl.module

/**
 * Koin module for Android-specific bindings of shared contracts.
 * Receives [DefaultCryptoManager.SessionKeyPersistence] from the app module's
 * SessionKeyStore binding (if available) to persist session keys across restarts.
 */
val androidSharedModule = module {
    single<PlatformSecurity> { AndroidPlatformSecurity(get()) }
    single<RatchetStateVerifier> { AndroidRatchetStateVerifier(get()) }
    single { KeyPairGenerator() }
    single<CryptoManager> {
        DefaultCryptoManager(
            keyPairGenerator = get(),
            ratchetStateVerifier = get(),
            sessionKeyPersistence = getOrNull()
        )
    }
}

package `in`.grayscales.entangl

import android.app.Application
import `in`.grayscales.entangl.core.security.PlatformSecurity
import `in`.grayscales.entangl.di.androidSharedModule
import `in`.grayscales.entangl.di.appModule
import `in`.grayscales.entangl.di.sharedModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application entry point for Entangl.
 * Initializes Koin dependency injection and performs early platform security initialization.
 */
class EntanglApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidContext(this@EntanglApplication)
            modules(sharedModule, androidSharedModule, appModule)
        }

        // Run early hardware/platform security checks
        val platformSecurity: PlatformSecurity = get()
        platformSecurity.initialize()
    }
}

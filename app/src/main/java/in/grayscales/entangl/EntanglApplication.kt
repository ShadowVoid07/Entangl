package `in`.grayscales.entangl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import `in`.grayscales.entangl.core.security.PlatformSecurity
import `in`.grayscales.entangl.data.network.NetworkTransport
import `in`.grayscales.entangl.di.androidSharedModule
import `in`.grayscales.entangl.di.appModule
import `in`.grayscales.entangl.di.sharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // Run early hardware/platform security checks and warm up database & crypto asynchronously to prevent blocking app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val platformSecurity: PlatformSecurity = get()
                platformSecurity.initialize()
                get<`in`.grayscales.entangl.core.identity.NodeIdentityManager>()
                get<`in`.grayscales.entangl.data.local.EntanglDatabase>()
            } catch (_: Exception) {}
        }

        // Schedule periodic WorkManager task to purge expired ephemeral messages
        `in`.grayscales.entangl.core.security.EphemeralMessageCleanupWorker.schedule(this)

        // Track foreground/background lifecycle to switch NetworkTransport between
        // persistent SSE streaming and low-power burst polling, conserving cellular radio power.
        val networkTransport: NetworkTransport = get()
        var runningActivities = 0
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (runningActivities++ == 0) {
                    networkTransport.setForeground(true)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (--runningActivities <= 0) {
                    runningActivities = 0
                    networkTransport.setForeground(false)
                    // An open chat is no longer visible: re-arm its notifications so
                    // background arrivals buzz again instead of staying silent.
                    try {
                        get<`in`.grayscales.entangl.data.notification.EntanglNotificationManager>()
                            .setForegroundContact(null)
                    } catch (_: Exception) {}
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

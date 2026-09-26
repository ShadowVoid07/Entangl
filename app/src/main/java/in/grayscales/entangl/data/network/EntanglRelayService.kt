package `in`.grayscales.entangl.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import `in`.grayscales.entangl.R
import org.koin.android.ext.android.inject

class EntanglRelayService : Service() {

    private val networkMonitor: NetworkMonitor by inject()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        networkMonitor.startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entangl")
            .setContentText("Encrypted connection is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // The NetworkMonitor and NetworkTransport are actually managed via DI and MainActivity lifecycle,
        // but this service keeps the process elevated.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }

    private fun createNotificationChannel() {
        val name = "Entangl Relay Service"
        val descriptionText = "Keeps encrypted connections alive in the background"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "entangl_relay_channel"
        const val NOTIFICATION_ID = 101
    }
}

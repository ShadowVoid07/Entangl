package `in`.grayscales.entangl.data.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import `in`.grayscales.entangl.R
import `in`.grayscales.entangl.data.network.NetworkMonitor
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
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback to launcher icon if ic_stat_entangl not present
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Entangl Relay Service"
            val descriptionText = "Keeps encrypted connections alive in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "entangl_relay_channel"
        const val NOTIFICATION_ID = 101
    }
}

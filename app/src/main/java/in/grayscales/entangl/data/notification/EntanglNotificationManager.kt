package `in`.grayscales.entangl.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import `in`.grayscales.entangl.MainActivity

/**
 * Zero-Leak Notification Manager for Entangl.
 *
 * Guarantees zero lockscreen metadata leakage by enforcing [NotificationCompat.VISIBILITY_SECRET].
 * Incoming packet notifications display generic cryptographic status without leaking
 * sender identities, Tor onion addresses, or message plaintext.
 */
class EntanglNotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "entangl_secure_transmissions"
        const val CHANNEL_NAME = "Encrypted Transmissions"
        const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming end-to-end encrypted quantum messages"
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                enableLights(true)
                lightColor = 0xFF00F0FF.toInt() // Quantum Cyan LED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Post a secure notification for an incoming encrypted packet.
     * Enforces [NotificationCompat.VISIBILITY_SECRET] and immutable PendingIntent.
     */
    fun showIncomingMessageNotification(contactUid: String) {
        // Android 13+ runtime permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("EntanglNotification", "Cannot post notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        // Explicit Intent to MainActivity with contact UID
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONTACT_UID, contactUid)
        }

        // FLAG_IMMUTABLE prevents malicious modification of intent parameters
        val pendingIntent = PendingIntent.getActivity(
            context,
            contactUid.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Encrypted Quantum Packet")
            .setContentText("1 encrypted transmission received")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Zero lockscreen leak
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00F0FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = NOTIFICATION_ID_BASE + (contactUid.hashCode() and 0x7FFF)
        Log.i("EntanglNotification", "Posting incoming message notification (ID: $notifId) for contact $contactUid")
        notificationManager.notify(notifId, notification)
    }

    /**
     * Dismiss notifications for a specific contact once user enters conversation.
     */
    fun cancelForContact(contactUid: String) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + (contactUid.hashCode() and 0x7FFF))
    }

    /**
     * Post a secure notification when a peer scans the local QR code.
     */
    fun showScanPingNotification(contactUid: String, peerName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("EntanglNotification", "Cannot post scan ping notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONTACT_UID, contactUid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            contactUid.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val displayName = peerName.ifBlank { "A peer" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("New Peer Connection Request")
            .setContentText("$displayName scanned your QR code and can send messages")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00F0FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = NOTIFICATION_ID_BASE + (contactUid.hashCode() and 0x7FFF)
        Log.i("EntanglNotification", "Posting scan ping notification (ID: $notifId) for peer $displayName ($contactUid)")
        notificationManager.notify(notifId, notification)
    }

    /**
     * Post a secure notification when a peer accepts our connection request.
     */
    fun showScanAcceptNotification(contactUid: String, peerName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("EntanglNotification", "Cannot post scan accept notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONTACT_UID, contactUid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            contactUid.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val displayName = peerName.ifBlank { "Peer" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Connection Accepted")
            .setContentText("$displayName accepted your connection request")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00F0FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = NOTIFICATION_ID_BASE + (contactUid.hashCode() and 0x7FFF)
        Log.i("EntanglNotification", "Posting scan accept notification (ID: $notifId) for peer $displayName ($contactUid)")
        notificationManager.notify(notifId, notification)
    }
}

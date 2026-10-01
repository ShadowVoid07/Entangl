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

    /**
     * Stable per-contact notification IDs. The old `hashCode() and 0x7FFF` scheme
     * collided across peers (one peer's notification overwrote another's, and a
     * colliding PendingIntent could deep-link the wrong chat). Allocated once per
     * UID, capped to keep IDs in range.
     */
    private val notificationIds = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val nextNotificationId = java.util.concurrent.atomic.AtomicInteger(1)

    private fun notificationIdFor(contactUid: String): Int =
        notificationIds.getOrPut(contactUid) {
            NOTIFICATION_ID_BASE + (nextNotificationId.getAndIncrement() and 0x7FFF)
        }

    /**
     * UID of the conversation currently open on screen, if any. While set,
     * incoming-message notifications for that peer are suppressed (no buzz/shade
     * spam for a chat the user is already reading) — delivery, storage and ACKs
     * are unaffected. Wired from ChatViewModel selection state.
     */
    @Volatile
    private var foregroundContactUid: String? = null

    fun setForegroundContact(contactUid: String?) {
        foregroundContactUid = contactUid?.ifBlank { null }
        if (contactUid != null) {
            cancelForContact(contactUid)
        }
    }

    fun isForegroundContact(contactUid: String): Boolean =
        foregroundContactUid != null && foregroundContactUid == contactUid

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
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

        // FLAG_IMMUTABLE prevents malicious modification of intent parameters.
        // Stable per-contact requestCode: colliding codes could route taps to the
        // wrong conversation.
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(contactUid),
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

        val notifId = notificationIdFor(contactUid)
        Log.d("EntanglNotification", "Posting incoming message notification (ID: $notifId)")
        notificationManager.notify(notifId, notification)
    }

    /**
     * Dismiss notifications for a specific contact once user enters conversation.
     */
    fun cancelForContact(contactUid: String) {
        notificationManager.cancel(notificationIdFor(contactUid))
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
            notificationIdFor(contactUid),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Display names originate from peer envelopes: bound length defensively even
        // though the repository sanitizes first (defense in depth at the shade).
        val displayName = peerName.filterNot { it.isISOControl() }.trim().take(25).ifBlank { "A peer" }
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

        val notifId = notificationIdFor(contactUid)
        Log.d("EntanglNotification", "Posting scan ping notification (ID: $notifId)")
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
            notificationIdFor(contactUid),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val displayName = peerName.filterNot { it.isISOControl() }.trim().take(25).ifBlank { "Peer" }
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

        val notifId = notificationIdFor(contactUid)
        Log.d("EntanglNotification", "Posting scan accept notification (ID: $notifId)")
        notificationManager.notify(notifId, notification)
    }
}

package `in`.grayscales.entangl.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Guard for the system clipboard to prevent leaks of keys, onions, or decrypted messages.
 * Automatically clears clipboard contents after a configured timeout (default: 30 seconds).
 */
class ClipboardGuard(
    private val context: Context,
    private val autoClearTimeoutMs: Long = 30_000L
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingClearRunnable: Runnable? = null

    /**
     * Copy sensitive text to the clipboard with auto-clear scheduled.
     */
    fun copySensitive(label: String, text: String) {
        val manager = clipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        // On Android 13+ (API 33), mark clip as sensitive so system UI masks preview
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        manager.setPrimaryClip(clip)

        scheduleAutoClear()
    }

    /**
     * Clear the clipboard immediately.
     */
    fun clearClipboard() {
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClearRunnable = null

        val manager = clipboardManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            manager.clearPrimaryClip()
        } else {
            val emptyClip = ClipData.newPlainText("", "")
            manager.setPrimaryClip(emptyClip)
        }
    }

    private fun scheduleAutoClear() {
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable { clearClipboard() }
        pendingClearRunnable = runnable
        mainHandler.postDelayed(runnable, autoClearTimeoutMs)
    }
}

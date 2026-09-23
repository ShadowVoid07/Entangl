package `in`.grayscales.entangl.core.security

/**
 * Security events detected by the platform hardening layer.
 * Emitted by [PlatformSecurity] and consumed by the UI layer
 * to display appropriate warnings or take defensive action.
 */
sealed class SecurityEvent {
    /** A debugger is attached to the process. */
    data object DebuggerDetected : SecurityEvent()

    /** The device appears to be rooted or has an unlocked bootloader. */
    data object RootDetected : SecurityEvent()

    /** A third-party accessibility service is active (potential keylogger). */
    data class AccessibilityServiceActive(
        val serviceName: String
    ) : SecurityEvent()

    /** The ratchet state HMAC verification failed (tampering or rollback). */
    data class RatchetTampered(
        val contactUid: String
    ) : SecurityEvent()

    /** Clipboard was automatically cleared after timeout. */
    data object ClipboardCleared : SecurityEvent()
}

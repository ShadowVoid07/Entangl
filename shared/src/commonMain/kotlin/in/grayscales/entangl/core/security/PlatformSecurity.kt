package `in`.grayscales.entangl.core.security

/**
 * Platform-specific security hardening operations.
 *
 * Android: Anti-debug, root detection, accessibility monitoring,
 *          clipboard guard, FLAG_SECURE, filterTouchesWhenObscured.
 * iOS: Jailbreak detection, screen capture prevention, pasteboard guard.
 */
interface PlatformSecurity {

    /** Initialize all platform security checks. Call once at app startup. */
    fun initialize()

    /** Run all threat detection checks and return any active threats. */
    fun checkThreats(): List<SecurityEvent>

    /**
     * Apply window-level security protections.
     * Android: FLAG_SECURE + filterTouchesWhenObscured on the Window.
     * iOS: Screen capture notification + overlay.
     *
     * @param windowHandle Platform-specific window reference (Android: Window, iOS: UIWindow).
     */
    fun applyWindowProtection(windowHandle: Any)

    /** Clear the system clipboard of any data originating from this app. */
    fun clearClipboard()

    /** Start continuous monitoring for accessibility service changes. */
    fun startContinuousMonitoring()

    /** Stop continuous monitoring (call in onDestroy/cleanup). */
    fun stopContinuousMonitoring()
}

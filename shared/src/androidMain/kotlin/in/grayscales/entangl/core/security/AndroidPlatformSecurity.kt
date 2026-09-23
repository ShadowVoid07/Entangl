package `in`.grayscales.entangl.core.security

import android.content.Context
import android.view.Window
import android.view.WindowManager

/**
 * Android implementation of [PlatformSecurity].
 * Coordinates debugger detection, root detection, accessibility monitoring,
 * clipboard zeroization, FLAG_SECURE window protection, and touch obfuscation filtering.
 */
class AndroidPlatformSecurity(
    private val context: Context
) : PlatformSecurity {

    private val debugDetector = DebugDetector(context)
    private val rootDetector = RootDetector()
    private val clipboardGuard = ClipboardGuard(context)
    private var accessibilityMonitor: AccessibilityMonitor? = null
    private val activeThreats = mutableListOf<SecurityEvent>()

    override fun initialize() {
        checkThreats()
        startContinuousMonitoring()
    }

    override fun checkThreats(): List<SecurityEvent> {
        val threats = mutableListOf<SecurityEvent>()

        if (debugDetector.isDebuggerActive()) {
            threats.add(SecurityEvent.DebuggerDetected)
        }

        if (rootDetector.isDeviceRooted()) {
            threats.add(SecurityEvent.RootDetected)
        }

        accessibilityMonitor?.getActiveServices()?.forEach { serviceName ->
            threats.add(SecurityEvent.AccessibilityServiceActive(serviceName))
        }

        synchronized(activeThreats) {
            activeThreats.clear()
            activeThreats.addAll(threats)
        }
        return threats
    }

    override fun applyWindowProtection(windowHandle: Any) {
        if (windowHandle is Window) {
            // Prevent screenshots, screen recording, and OS recent app thumbnail leaks
            windowHandle.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            // Prevent tapjacking and overlay clickjacking
            windowHandle.decorView.filterTouchesWhenObscured = true
        }
    }

    override fun clearClipboard() {
        clipboardGuard.clearClipboard()
    }

    override fun startContinuousMonitoring() {
        if (accessibilityMonitor == null) {
            accessibilityMonitor = AccessibilityMonitor(context) { suspiciousService ->
                synchronized(activeThreats) {
                    activeThreats.add(SecurityEvent.AccessibilityServiceActive(suspiciousService))
                }
            }
        }
        accessibilityMonitor?.startMonitoring()
    }

    override fun stopContinuousMonitoring() {
        accessibilityMonitor?.stopMonitoring()
    }
}

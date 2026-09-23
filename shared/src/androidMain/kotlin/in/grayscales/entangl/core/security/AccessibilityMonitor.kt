package `in`.grayscales.entangl.core.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * Monitors active Accessibility Services to detect potential overlay or keylogging attacks.
 */
class AccessibilityMonitor(
    private val context: Context,
    private val onSuspiciousServiceDetected: (String) -> Unit = {}
) {
    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private val listener = AccessibilityManager.AccessibilityStateChangeListener { _ ->
        checkActiveServices()
    }

    /**
     * Start observing changes in accessibility services.
     */
    fun startMonitoring() {
        accessibilityManager?.addAccessibilityStateChangeListener(listener)
        checkActiveServices()
    }

    /**
     * Stop observing accessibility state changes.
     */
    fun stopMonitoring() {
        accessibilityManager?.removeAccessibilityStateChangeListener(listener)
    }

    /**
     * Returns a list of active accessibility service names.
     */
    fun getActiveServices(): List<String> {
        val manager = accessibilityManager ?: return emptyList()
        val services = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return services.mapNotNull { it.resolveInfo?.serviceInfo?.name }
    }

    private fun checkActiveServices() {
        val services = getActiveServices()
        for (service in services) {
            // Flag any non-system or unknown accessibility services
            if (!isKnownSafeService(service)) {
                onSuspiciousServiceDetected(service)
            }
        }
    }

    private fun isKnownSafeService(serviceName: String): Boolean {
        // System accessibility services usually belong to com.google.android or com.android
        return serviceName.startsWith("com.google.android.accessibility") ||
                serviceName.startsWith("com.android.server.accessibility")
    }
}

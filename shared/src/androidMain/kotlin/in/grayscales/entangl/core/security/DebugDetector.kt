package `in`.grayscales.entangl.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Detects attached debuggers and ptrace-based instrumentation (Frida, GDB, LLDB).
 */
class DebugDetector(private val context: Context) {

    /**
     * Returns true if a debugger is attached or the app is being traced.
     */
    fun isDebuggerActive(): Boolean {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return true
        }

        // Check if debuggable flag is enabled in production
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable && !isRunningInEmulator()) {
            // Can be treated as an anomaly if release build has debuggable flag
        }

        // Check /proc/self/status for TracerPid != 0
        return isTracerPidAttached()
    }

    private fun isTracerPidAttached(): Boolean {
        val statusFile = File("/proc/self/status")
        if (!statusFile.exists() || !statusFile.canRead()) return false

        try {
            BufferedReader(FileReader(statusFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("TracerPid:")) {
                        val tracerPid = line!!.substring("TracerPid:".length).trim().toIntOrNull() ?: 0
                        return tracerPid > 0
                    }
                }
            }
        } catch (_: Exception) {
            // Discard exception to avoid crashing
        }
        return false
    }

    private fun isRunningInEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu")
    }
}

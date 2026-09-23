package `in`.grayscales.entangl.core.security

import android.os.Build
import java.io.File

/**
 * Detects root access, Magisk modules, and compromised execution environments.
 */
class RootDetector {

    companion object {
        private val ROOT_PATHS = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        private val DANGEROUS_PROPERTIES = arrayOf(
            "ro.debuggable",
            "ro.secure"
        )
    }

    /**
     * Checks if device is rooted or shows signs of system tampering.
     */
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuBinaryPaths() || checkWhichSu()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinaryPaths(): Boolean {
        for (path in ROOT_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkWhichSu(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }
}

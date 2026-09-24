package `in`.grayscales.entangl.ui.theme

import androidx.compose.ui.graphics.Color
import `in`.grayscales.entangl.ui.theme.ColorUtils.DEFAULT_PROFILE_HEX
import kotlin.math.abs

object ColorUtils {

    const val DEFAULT_PROFILE_HEX = "#00F0FF"
    val DEFAULT_PROFILE_COLOR = Color(0xFF00F0FF)

    private val HEX_REGEX = Regex("^[0-9A-Fa-f]{6}$")

    /**
     * Checks whether the provided string is a valid 6-digit hex color with or without leading '#'.
     */
    fun isValidHexColor(hex: String?): Boolean {
        if (hex.isNullOrBlank()) return false
        val cleaned = hex.trim().removePrefix("#")
        return HEX_REGEX.matches(cleaned)
    }

    /**
     * Normalizes a hex string to uppercase `#RRGGBB` format, falling back to [DEFAULT_PROFILE_HEX].
     */
    fun formatHexColor(hex: String?): String {
        if (hex.isNullOrBlank()) return DEFAULT_PROFILE_HEX
        val cleaned = hex.trim().removePrefix("#")
        return if (HEX_REGEX.matches(cleaned)) {
            "#${cleaned.uppercase()}"
        } else {
            DEFAULT_PROFILE_HEX
        }
    }

    /**
     * Converts a hex string (`#RRGGBB` or `RRGGBB`) to a Compose [Color], or null if invalid.
     */
    fun parseColorOrNull(hex: String?): Color? {
        if (!isValidHexColor(hex)) return null
        val cleaned = hex!!.trim().removePrefix("#")
        val colorInt = cleaned.toLongOrNull(16) ?: return null
        return Color(0xFF000000 or colorInt)
    }

    /**
     * Converts a hex string to a Compose [Color], falling back to [defaultColor].
     */
    fun parseColorOrDefault(hex: String?, defaultColor: Color = DEFAULT_PROFILE_COLOR): Color {
        return parseColorOrNull(hex) ?: defaultColor
    }

    /**
     * Converts a Compose [Color] to uppercase `#RRGGBB` string.
     */
    fun colorToHex(color: Color): String {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    /**
     * Deconstructs a Compose [Color] into Hue (0..360), Saturation (0..1), and Value (0..1).
     */
    fun colorToHsv(color: Color): Triple<Float, Float, Float> {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val rawHue = when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) % 6f) * 60f
            max == g -> (((b - r) / delta) + 2f) * 60f
            else -> (((r - g) / delta) + 4f) * 60f
        }
        val hue = if (rawHue < 0f) rawHue + 360f else rawHue
        val saturation = if (max == 0f) 0f else delta / max
        val value = max

        return Triple(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    }

    /**
     * Reconstructs a Compose [Color] from Hue (0..360), Saturation (0..1), and Value (0..1).
     */
    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val h = (hue % 360f + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val c = v * s
        val hPrime = h / 60f
        val x = c * (1f - abs((hPrime % 2f) - 1f))

        val (r1, g1, b1) = when {
            hPrime < 1f -> Triple(c, x, 0f)
            hPrime < 2f -> Triple(x, c, 0f)
            hPrime < 3f -> Triple(0f, c, x)
            hPrime < 4f -> Triple(0f, x, c)
            hPrime < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val m = v - c
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    data class Preset(
        val name: String,
        val hex: String,
        val color: Color
    )

    val PRESETS = listOf(
        Preset("Quantum Cyan", "#00F0FF", Color(0xFF00F0FF)),
        Preset("Matrix Emerald", "#00FF9D", Color(0xFF00FF9D)),
        Preset("Laser Crimson", "#FF003C", Color(0xFFFF003C)),
        Preset("Singularity Purple", "#9D00FF", Color(0xFF9D00FF)),
        Preset("Electric Amber", "#FFE600", Color(0xFFFFE600)),
        Preset("Cyber Rose", "#FF007F", Color(0xFFFF007F)),
        Preset("Deep Photon", "#00A3FF", Color(0xFF00A3FF)),
        Preset("Plasma Violet", "#7000FF", Color(0xFF7000FF))
    )
}

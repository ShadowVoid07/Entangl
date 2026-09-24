package `in`.grayscales.entangl

import androidx.compose.ui.graphics.Color
import `in`.grayscales.entangl.ui.theme.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun testIsValidHexColor() {
        assertTrue(ColorUtils.isValidHexColor("#00F0FF"))
        assertTrue(ColorUtils.isValidHexColor("00F0FF"))
        assertTrue(ColorUtils.isValidHexColor("#ffffff"))
        assertTrue(ColorUtils.isValidHexColor("#000000"))
        assertTrue(ColorUtils.isValidHexColor("A1B2C3"))

        assertFalse(ColorUtils.isValidHexColor(null))
        assertFalse(ColorUtils.isValidHexColor(""))
        assertFalse(ColorUtils.isValidHexColor("   "))
        assertFalse(ColorUtils.isValidHexColor("#12345"))      // Too short
        assertFalse(ColorUtils.isValidHexColor("#1234567"))    // Too long
        assertFalse(ColorUtils.isValidHexColor("#GGFFFF"))    // Non-hex
        assertFalse(ColorUtils.isValidHexColor("red"))        // Word
    }

    @Test
    fun testFormatHexColor() {
        assertEquals("#00F0FF", ColorUtils.formatHexColor("#00f0ff"))
        assertEquals("#AABBCC", ColorUtils.formatHexColor("aabbcc"))
        assertEquals("#00F0FF", ColorUtils.formatHexColor(null))
        assertEquals("#00F0FF", ColorUtils.formatHexColor(""))
        assertEquals("#00F0FF", ColorUtils.formatHexColor("invalid"))
    }

    @Test
    fun testParseColorOrNull() {
        val cyan = ColorUtils.parseColorOrNull("#00F0FF")
        assertNotNull(cyan)
        assertEquals(Color(0xFF00F0FF), cyan)

        assertNull(ColorUtils.parseColorOrNull("not-a-color"))
        assertNull(ColorUtils.parseColorOrNull(null))
    }

    @Test
    fun testParseColorOrDefault() {
        val fallback = Color(0xFFFF0000)
        val valid = ColorUtils.parseColorOrDefault("#00FF00", fallback)
        assertEquals(Color(0xFF00FF00).value, valid.value)

        val invalid = ColorUtils.parseColorOrDefault("bad-hex", fallback)
        assertEquals(fallback.value, invalid.value)
    }

    @Test
    fun testColorToHex() {
        assertEquals("#00F0FF", ColorUtils.colorToHex(Color(0xFF00F0FF)))
        assertEquals("#FF003C", ColorUtils.colorToHex(Color(0xFFFF003C)))
        assertEquals("#000000", ColorUtils.colorToHex(Color(0xFF000000)))
        assertEquals("#FFFFFF", ColorUtils.colorToHex(Color(0xFFFFFFFF)))
    }

    @Test
    fun testHsvConversions() {
        // Red: H=0, S=1, V=1
        val red = Color(0xFFFF0000)
        val (hR, sR, vR) = ColorUtils.colorToHsv(red)
        assertEquals(0f, hR, 1.0f)
        assertEquals(1f, sR, 0.01f)
        assertEquals(1f, vR, 0.01f)
        val reconRed = ColorUtils.hsvToColor(hR, sR, vR)
        assertEquals(ColorUtils.colorToHex(red), ColorUtils.colorToHex(reconRed))

        // Green: H=120, S=1, V=1
        val green = Color(0xFF00FF00)
        val (hG, sG, vG) = ColorUtils.colorToHsv(green)
        assertEquals(120f, hG, 1.0f)
        assertEquals(1f, sG, 0.01f)
        assertEquals(1f, vG, 0.01f)
        val reconGreen = ColorUtils.hsvToColor(hG, sG, vG)
        assertEquals(ColorUtils.colorToHex(green), ColorUtils.colorToHex(reconGreen))

        // Blue: H=240, S=1, V=1
        val blue = Color(0xFF0000FF)
        val (hB, sB, vB) = ColorUtils.colorToHsv(blue)
        assertEquals(240f, hB, 1.0f)
        assertEquals(1f, sB, 0.01f)
        assertEquals(1f, vB, 0.01f)
        val reconBlue = ColorUtils.hsvToColor(hB, sB, vB)
        assertEquals(ColorUtils.colorToHex(blue), ColorUtils.colorToHex(reconBlue))
    }

    @Test
    fun testPresetsIntegrity() {
        assertEquals(8, ColorUtils.PRESETS.size)
        for (preset in ColorUtils.PRESETS) {
            assertTrue("Preset ${preset.name} hex must be valid", ColorUtils.isValidHexColor(preset.hex))
            assertEquals("Preset hex must be formatted", preset.hex.uppercase(), ColorUtils.formatHexColor(preset.hex))
        }
    }
}

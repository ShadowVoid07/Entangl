package `in`.grayscales.entangl.ui.theme

import android.app.Activity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val QuantumDarkColorScheme = darkColorScheme(
    primary = QuantumCyan,
    onPrimary = VoidBackground,
    primaryContainer = DarkMatterVariant,
    onPrimaryContainer = QuantumCyan,
    secondary = QuantumCyanVariant,
    onSecondary = VoidBackground,
    secondaryContainer = DarkMatter,
    onSecondaryContainer = NeutronWhite,
    tertiary = QuantumGreen,
    onTertiary = VoidBackground,
    background = VoidBackground,
    onBackground = NeutronWhite,
    surface = DarkMatter,
    onSurface = NeutronWhite,
    surfaceVariant = DarkMatterVariant,
    onSurfaceVariant = SubatomicGray,
    error = IsotopeMagenta,
    onError = VoidBackground,
    outline = ParticleBorder
)

/**
 * Entangl Theme:
 * - Pure Dark OLED Void aesthetic
 * - Dynamic color disabled to prevent security UI color alteration
 * - Automatic FLAG_SECURE enforcement on the window
 */
@Composable
fun EntanglTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Hardware window protection against screen recording and recents thumbnail leaking
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
            window.decorView.filterTouchesWhenObscured = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = QuantumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
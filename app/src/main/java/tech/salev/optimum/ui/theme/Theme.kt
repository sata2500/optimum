package tech.salev.optimum.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import tech.salev.optimum.data.repository.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AccentGoldLight,
    tertiary = AccentGoldDark,
    background = PrimaryDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGoldDark,
    secondary = AccentGold,
    tertiary = AccentGoldLight,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

/**
 * Main app theme composable.
 *
 * @param themeMode User-selected theme preference (SYSTEM / LIGHT / DARK).
 *                  Defaults to SYSTEM for backwards compatibility.
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+).
 *                     Disabled when the user explicitly picks LIGHT or DARK.
 */
@Composable
fun OptimumTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    // Dynamic colors (Material You) only on Android 12+ and when no explicit theme is chosen
    val useDynamic = themeMode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

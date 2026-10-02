package com.occaecat.ztoeschedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

val LocalDisplayMode = staticCompositionLocalOf { DisplayMode.Comfortable }
val LocalLiquidGlass = staticCompositionLocalOf { false }

/*
 * Static palette used when dynamic colour is off (or unavailable).
 * Material "fidelity" scheme generated from seed #FFB300: amber light-bulb primary
 * with an electric-cyan tertiary; warm neutrals so surfaces stay calm.
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7E5700),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFB300),
    onPrimaryContainer = Color(0xFF6B4900),
    inversePrimary = Color(0xFFFFBA38),
    secondary = Color(0xFF795920),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD38F),
    onSecondaryContainer = Color(0xFF795921),
    tertiary = Color(0xFF00677E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF00D2FE),
    onTertiaryContainer = Color(0xFF00566A),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF211B11),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF211B11),
    surfaceVariant = Color(0xFFF3E0C7),
    onSurfaceVariant = Color(0xFF514532),
    surfaceTint = Color(0xFF7E5700),
    inverseSurface = Color(0xFF372F24),
    inverseOnSurface = Color(0xFFFDEFDE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF847560),
    outlineVariant = Color(0xFFD6C4AC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F3),
    surfaceContainer = Color(0xFFFAECDB),
    surfaceContainerHigh = Color(0xFFF4E6D6),
    surfaceContainerHighest = Color(0xFFEEE0D0),
    surfaceContainerLow = Color(0xFFFFF2E2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5D8C8)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD79B),
    onPrimary = Color(0xFF432C00),
    primaryContainer = Color(0xFFFFB300),
    onPrimaryContainer = Color(0xFF6B4900),
    inversePrimary = Color(0xFF7E5700),
    secondary = Color(0xFFEAC07D),
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Color(0xFF61440C),
    onSecondaryContainer = Color(0xFFDBB270),
    tertiary = Color(0xFFA4E7FF),
    onTertiary = Color(0xFF003543),
    tertiaryContainer = Color(0xFF00D2FE),
    onTertiaryContainer = Color(0xFF00566A),
    background = Color(0xFF181209),
    onBackground = Color(0xFFEEE0D0),
    surface = Color(0xFF181209),
    onSurface = Color(0xFFEEE0D0),
    surfaceVariant = Color(0xFF514532),
    onSurfaceVariant = Color(0xFFD6C4AC),
    surfaceTint = Color(0xFFFFBA38),
    inverseSurface = Color(0xFFEEE0D0),
    inverseOnSurface = Color(0xFF372F24),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9E8E78),
    outlineVariant = Color(0xFF514532),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF40382D),
    surfaceContainer = Color(0xFF251F15),
    surfaceContainerHigh = Color(0xFF30291E),
    surfaceContainerHighest = Color(0xFF3B3429),
    surfaceContainerLow = Color(0xFF211B11),
    surfaceContainerLowest = Color(0xFF130D05),
    surfaceDim = Color(0xFF181209)
)

/** True-black variant for OLED: base layers black, containers lifted just enough to stay distinct. */
fun androidx.compose.material3.ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212)
)

val AmoledColorScheme = DarkColorScheme.toAmoled()

val ContrastColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF333333),
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color.Black
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SvitloYeZhytomyrTheme(
    themePreference: ColorTheme = ColorTheme.System,
    displayMode: DisplayMode = DisplayMode.Comfortable,
    liquidGlass: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val systemDark = isSystemInDarkTheme()
    val isDynamicSupported = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val effectiveTheme = if (isAmoled && (themePreference == ColorTheme.Dark || (themePreference == ColorTheme.System && systemDark))) {
        ColorTheme.Amoled
    } else {
        themePreference
    }

    val colorScheme = when (effectiveTheme) {
        ColorTheme.System -> {
            if (isDynamicSupported) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) DarkColorScheme else LightColorScheme
            }
        }
        ColorTheme.Light -> {
            if (isDynamicSupported) dynamicLightColorScheme(context) else LightColorScheme
        }
        ColorTheme.Dark -> {
            if (isDynamicSupported) dynamicDarkColorScheme(context) else DarkColorScheme
        }
        ColorTheme.Amoled -> {
            if (isDynamicSupported) dynamicDarkColorScheme(context).toAmoled() else AmoledColorScheme
        }
        ColorTheme.Contrast -> ContrastColorScheme
    }

    CompositionLocalProvider(LocalDisplayMode provides displayMode, LocalLiquidGlass provides liquidGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Use standard typography
            shapes = ExpressiveShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

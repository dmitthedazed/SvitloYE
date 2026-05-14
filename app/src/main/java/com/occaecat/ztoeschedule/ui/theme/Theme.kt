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

val LocalCornerRadius = staticCompositionLocalOf { 24 }
val LocalDisplayMode = staticCompositionLocalOf { DisplayMode.Comfortable }
val LocalLiquidGlass = staticCompositionLocalOf { false }
val LocalGlassBackdrop = staticCompositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),
    scrim = Color(0xFF000000),
    surfaceTint = PrimaryDark
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight
)

val AmoledColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color(0xFF6750A4),
    scrim = Color(0xFF000000),
    surfaceTint = PrimaryDark
)

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
    cornerRadius: Int = -1,
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

    // Detect system corner radius or fallback to 24
    val finalCornerRadius = remember(cornerRadius) {
        if (cornerRadius == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val activity = context as? android.app.Activity
                val radius = if (activity != null) {
                    val insets = activity.window.decorView.rootWindowInsets
                    // POSITION_TOP_LEFT is a safe bet for generic radius
                    insets?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.radius
                } else null
                
                radius?.let { 
                    with(density) { it.toDp().value.toInt() } 
                } ?: 24
            }
            else {
                24
            }
        } else {
            cornerRadius
        }
    }

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
            // Force disable dynamic color for background/surface in AMOLED mode, but maybe keep accents?
            // Current implementation ignores dynamic colors for Amoled background
            val base = if (isDynamicSupported) dynamicDarkColorScheme(context) else DarkColorScheme
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF121212),
                onBackground = Color.White,
                onSurface = Color.White,
                outline = Color(0xFF333333)
            )
        }
        ColorTheme.Contrast -> ContrastColorScheme
    }

    val customShapes = materialShapesForCornerRadius(finalCornerRadius)

    CompositionLocalProvider(LocalCornerRadius provides finalCornerRadius, LocalDisplayMode provides displayMode, LocalLiquidGlass provides liquidGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Use standard typography
            shapes = customShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

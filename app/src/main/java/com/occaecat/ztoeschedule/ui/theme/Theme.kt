package com.occaecat.ztoeschedule.ui.theme

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
import androidx.compose.material3.Typography
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.FontScale

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark
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
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF333333)
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

@Composable
fun SvitloYeZhytomyrTheme(
    themePreference: ColorTheme = ColorTheme.System,
    fontScalePreference: FontScale = FontScale.Normal,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDynamicSupported = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when (themePreference) {
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

    // Apply font scaling to all typography styles
    val scaledTypography = Typography(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * fontScalePreference.multiplier),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * fontScalePreference.multiplier),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * fontScalePreference.multiplier),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * fontScalePreference.multiplier),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * fontScalePreference.multiplier),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * fontScalePreference.multiplier),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * fontScalePreference.multiplier),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * fontScalePreference.multiplier),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * fontScalePreference.multiplier),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * fontScalePreference.multiplier),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * fontScalePreference.multiplier),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * fontScalePreference.multiplier),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * fontScalePreference.multiplier),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * fontScalePreference.multiplier),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * fontScalePreference.multiplier)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        shapes = Shapes,
        content = content
    )
}
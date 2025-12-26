package com.occaecat.ztoeschedule.widget.glance

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme

object SvitloYeGlanceTheme {
    val colors = ColorProviders(
        light = com.occaecat.ztoeschedule.ui.theme.LightColorScheme,
        dark = com.occaecat.ztoeschedule.ui.theme.DarkColorScheme
    )
}

@Composable
fun SvitloYeGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceTheme.colors
        } else {
            SvitloYeGlanceTheme.colors
        },
        content = content
    )
}
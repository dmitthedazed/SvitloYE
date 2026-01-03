package com.occaecat.ztoeschedule.widget.glance

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme

@Composable
fun SvitloYeGlanceTheme(content: @Composable () -> Unit) {
    // Use default Glance theme colors; project dependency version lacks ColorProviders helpers.
    GlanceTheme { content() }
}
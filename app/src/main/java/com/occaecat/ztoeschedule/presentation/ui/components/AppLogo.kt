package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.occaecat.ztoeschedule.R

/**
 * The app logo (hour ring, Є and bolt) drawn in two tinted layers so it follows
 * the current color scheme, including Material You dynamic colors.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    letterColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.ic_logo_letter),
            contentDescription = null,
            colorFilter = ColorFilter.tint(letterColor),
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.ic_logo_accent),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accentColor),
            modifier = Modifier.fillMaxSize()
        )
    }
}

package com.occaecat.ztoeschedule.presentation.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.settings.AppIdentityCard
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsAccent
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsPage
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsRow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTrailing
import com.occaecat.ztoeschedule.presentation.ui.settings.settingsSection

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    SettingsPage(
        title = "Про проєкт",
        onBack = onBack
    ) {
        item(key = "identity") { AppIdentityCard() }

        item(key = "mission") {
            Text(
                "Графіки відключень Житомиробленерго для вашої адреси — з нагадуваннями, віджетами та Android Auto. Безкоштовно й без реклами.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)
            )
        }

        settingsSection(
            "Автор",
            listOf(
                SettingsRow(
                    title = "Дмитрий",
                    subtitle = "Розробник",
                    icon = Icons.Default.Person,
                    accent = SettingsAccent.Primary,
                    trailing = SettingsTrailing.Custom {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BrandButton(R.drawable.ic_brand_github, "GitHub") { uriHandler.openUri("https://github.com/dmitthedazed") }
                            BrandButton(R.drawable.ic_brand_linkedin, "LinkedIn") { uriHandler.openUri("https://www.linkedin.com/in/dmitthedazed") }
                        }
                    }
                )
            )
        )

        settingsSection(
            "Підтримати розробку",
            listOf(
                SettingsRow(
                    title = "Monobank",
                    subtitle = "Банка на розвиток застосунку",
                    icon = Icons.Default.Favorite,
                    brandIcon = R.drawable.ic_brand_monobank,
                    trailing = SettingsTrailing.External,
                    onClick = { uriHandler.openUri("https://send.monobank.ua/2AMdpReyqQ") }
                ),
                SettingsRow(
                    title = "Privat24",
                    subtitle = "Переказ на картку",
                    icon = Icons.Default.Favorite,
                    brandIcon = R.drawable.ic_brand_privat24,
                    trailing = SettingsTrailing.External,
                    onClick = { uriHandler.openUri("https://www.privat24.ua/send/i3nk5") }
                )
            )
        )

        settingsSection(
            "Проєкт",
            listOf(
                SettingsRow(
                    title = "Вихідний код",
                    subtitle = "GitHub",
                    icon = Icons.Default.Code,
                    brandIcon = R.drawable.ic_brand_github,
                    trailing = SettingsTrailing.External,
                    onClick = { uriHandler.openUri("https://github.com/occaecat/ZTOESchedule") }
                ),
                SettingsRow(
                    title = "Ліцензія",
                    subtitle = "MIT",
                    icon = Icons.Default.Description,
                    accent = SettingsAccent.Neutral,
                    trailing = SettingsTrailing.External,
                    onClick = { uriHandler.openUri("https://opensource.org/licenses/MIT") }
                ),
                SettingsRow(
                    title = "Джерело даних",
                    subtitle = "ztoe.com.ua",
                    icon = Icons.Default.Language,
                    brandIcon = R.drawable.ic_brand_ztoe,
                    trailing = SettingsTrailing.External,
                    onClick = { uriHandler.openUri("https://www.ztoe.com.ua") }
                )
            )
        )

        item(key = "footer") {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Зроблено з ❤️ у Житомирі",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrandButton(icon: Int, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp)
        )
    }
}

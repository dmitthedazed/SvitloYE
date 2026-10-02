@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.occaecat.ztoeschedule.presentation.ui.info

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.occaecat.ztoeschedule.InfoActivity
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsAccent
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsPage
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsRow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTrailing
import com.occaecat.ztoeschedule.presentation.ui.settings.settingsSection

internal const val FEEDBACK_EMAIL = "olegkhasanovv@gmail.com"

internal fun openFeedbackEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:$FEEDBACK_EMAIL".toUri()
        putExtra(Intent.EXTRA_SUBJECT, "СвітлоЄ: зворотний зв'язок")
    }
    context.startActivity(Intent.createChooser(intent, "Написати нам"))
}

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onNavigateToGemini: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    SettingsPage(
        title = "Зв'язок",
        onBack = onBack
    ) {
        item(key = "hero") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepHeroIcon(
                    icon = Icons.Default.Forum,
                    shape = MaterialShapes.Clover4Leaf.toShape(),
                    containerColor = colorScheme.tertiaryContainer,
                    contentColor = colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.height(24.dp))
                Text("Ми на зв'язку", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Знайшли помилку в графіку чи маєте ідею? Напишіть — ми читаємо кожен лист.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        settingsSection(
            "Написати",
            listOf(
                SettingsRow(
                    title = "Електронна пошта",
                    subtitle = "Пропозиції та повідомлення про помилки",
                    icon = Icons.Default.Email,
                    accent = SettingsAccent.Primary,
                    trailing = SettingsTrailing.External,
                    onClick = { openFeedbackEmail(context) }
                ),
                SettingsRow(
                    title = "ШІ-підтримка",
                    subtitle = "Відповіді про графіки та енергосистему",
                    icon = Icons.Default.AutoAwesome,
                    accent = SettingsAccent.Tertiary,
                    trailing = SettingsTrailing.Custom {
                        Surface(color = colorScheme.tertiaryContainer, contentColor = colorScheme.onTertiaryContainer, shape = CircleShape) {
                            Text(
                                "BETA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    },
                    onClick = onNavigateToGemini
                )
            )
        )

        settingsSection(
            "Спершу подивіться",
            listOf(
                SettingsRow(
                    title = "Питання та відповіді",
                    subtitle = "Звідки дані, як працюють сповіщення і черги",
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = {
                        context.startActivity(Intent(context, InfoActivity::class.java).putExtra("type", "faq"))
                    }
                )
            )
        )

        item(key = "tip") {
            Surface(
                color = colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Якщо графік не збігається з реальністю — додайте до листа адресу й час. Так ми швидше знайдемо причину.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

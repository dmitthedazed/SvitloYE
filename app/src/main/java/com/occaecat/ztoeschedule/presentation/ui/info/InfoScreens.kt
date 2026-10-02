@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.occaecat.ztoeschedule.presentation.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsPage
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsSectionHeader

@Composable
fun GeminiChatScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    SettingsPage(title = "ШІ-підтримка", onBack = onBack) {
        item(key = "hero") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepHeroIcon(
                    icon = Icons.Default.AutoAwesome,
                    shape = MaterialShapes.Sunny.toShape(),
                    containerColor = colorScheme.tertiaryContainer,
                    contentColor = colorScheme.onTertiaryContainer,
                    rotate = true
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    "Помічник уже в дорозі",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ми працюємо над розумним помічником, який відповідатиме на питання про графіки та стан енергосистеми.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private data class Faq(val icon: ImageVector, val question: String, val answer: String)

private val faqs = listOf(
    Faq(
        Icons.Default.CloudDownload,
        "Звідки застосунок бере дані?",
        "Безпосередньо з офіційного сайту АТ «Житомиробленерго». Дані оновлюються автоматично кожні кілька хвилин."
    ),
    Faq(
        Icons.Default.SyncProblem,
        "Чому графік не збігається з реальністю?",
        "Графіки погодинних відключень — це план. Диспетчер може змінювати їх у реальному часі залежно від стану енергосистеми. Застосунок показує найсвіжішу доступну версію."
    ),
    Faq(
        Icons.Default.Tag,
        "Як дізнатися свою чергу?",
        "Під час першого запуску оберіть РЕМ, населений пункт, вулицю й будинок — застосунок сам визначить чергу та підчергу."
    ),
    Faq(
        Icons.Default.WifiOff,
        "Чи працює застосунок без інтернету?",
        "Так, останній завантажений графік зберігається на пристрої. Але пам'ятайте: офлайн дані можуть бути застарілими."
    ),
    Faq(
        Icons.Default.NotificationsActive,
        "Як працюють сповіщення?",
        "Застосунок надсилає нагадування до початку відключення або ввімкнення світла за вашою адресою. Для точного часу дозвольте точні будильники в налаштуваннях."
    ),
    Faq(
        Icons.Default.WarningAmber,
        "Що таке «можливе відключення»?",
        "Періоди, коли світло можуть вимкнути лише у разі критичного дефіциту потужності. Зазвичай у ці години світло є, але варто бути готовими."
    )
)

@Composable
fun FaqScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    SettingsPage(
        title = "Питання та відповіді",
        onBack = onBack
    ) {
        item(key = "faq_header") { SettingsSectionHeader("Часті запитання") }
        item(key = "faq_list") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                faqs.forEachIndexed { index, faq -> FaqItem(faq, index, faqs.size) }
            }
        }

        item(key = "more") {
            Surface(
                color = colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StepLeadingIcon(Icons.AutoMirrored.Filled.HelpOutline)
                    Spacer(Modifier.height(12.dp))
                    Text("Залишилися питання?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Напишіть нам — обов'язково допоможемо.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    StepPrimaryButton(text = "Написати нам", icon = Icons.Default.Email, onClick = { openFeedbackEmail(context) })
                }
            }
        }
    }
}

/** Accordion row in a grouped list: the question, and the answer revealed underneath. */
@Composable
private fun FaqItem(faq: Faq, index: Int, count: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val top by animateDpAsState(if (index == 0 || expanded) 24.dp else 4.dp, label = "faq_top")
    val bottom by animateDpAsState(if (index == count - 1 || expanded) 24.dp else 4.dp, label = "faq_bottom")
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "faq_chevron")

    Surface(
        onClick = { expanded = !expanded },
        color = if (expanded) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
        contentColor = if (expanded) colorScheme.onSecondaryContainer else colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) "Розгорнуто" else "Згорнуто"
            }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepLeadingIcon(faq.icon)
                Spacer(Modifier.width(16.dp))
                Text(
                    faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevron)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    faq.answer,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 56.dp, top = 12.dp, end = 8.dp)
                )
            }
        }
    }
}

package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Notice Card for displaying important schedule messages
 *
 * Features:
 * - Elevated card design
 * - Info icon indicator
 * - Selectable text for copying
 * - Highlights with red border when message contains "УВАГА" (Attention)
 * - Proper HTML entity handling
 *
 * @param message The formatted message text to display
 * @param modifier Optional modifier
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoticeCard(
    message: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Check if message contains attention keyword
    val isAttention = message.contains("УВАГА", ignoreCase = true) ||
                      message.contains("УВАГА!", ignoreCase = true) ||
                      message.contains("ATTENTION", ignoreCase = true)

    val cardModifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { },
            onLongClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString(message))
            },
            onLongClickLabel = "копіювати текст повідомлення"
        )

    // Use OutlinedCard for attention messages to show border
    if (isAttention) {
        OutlinedCard(
            modifier = cardModifier,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        ) {
            NoticeCardContent(message = message, isAttention = true)
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            NoticeCardContent(message = message, isAttention = false)
        }
    }
}

/**
 * Content for the notice card
 */
@Composable
private fun NoticeCardContent(
    message: String,
    isAttention: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Інформація",
                tint = if (isAttention) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = if (isAttention) "ВАЖЛИВА ІНФОРМАЦІЯ" else "Інформація",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isAttention) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Selectable message text
        if (message.isNotBlank()) {
            SelectionContainer {
                Text(
                    text = AnnotatedString.fromHtml(message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineBreak = LineBreak.Paragraph,
                        hyphens = Hyphens.Auto
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        } else {
            Text(
                text = "Немає повідомлень",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * Compact version of NoticeCard for smaller displays
 */
@Composable
fun NoticeCardCompact(
    message: String,
    modifier: Modifier = Modifier
) {
    val isAttention = message.contains("УВАГА", ignoreCase = true) ||
                      message.contains("ATTENTION", ignoreCase = true)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAttention) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (isAttention) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.size(20.dp)
            )

            if (message.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAttention) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            } else {
                Text(
                    text = "Немає повідомлень",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}


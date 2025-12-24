package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
@Composable
fun NoticeCard(
    message: String,
    modifier: Modifier = Modifier
) {
    // Check if message contains attention keyword
    val isAttention = message.contains("УВАГА", ignoreCase = true) ||
                      message.contains("УВАГА!", ignoreCase = true) ||
                      message.contains("ATTENTION", ignoreCase = true)

    // Use OutlinedCard for attention messages to show border
    if (isAttention) {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
        ) {
            NoticeCardContent(message = message, isAttention = true)
        }
    } else {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )

        // Selectable message text
        if (message.isNotBlank()) {
            SelectionContainer {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )
            }
        } else {
            Text(
                text = "Немає повідомлень",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}


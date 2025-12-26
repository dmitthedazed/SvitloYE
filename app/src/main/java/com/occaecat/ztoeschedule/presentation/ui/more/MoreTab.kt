package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R

@Composable
fun MoreTab(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Menu Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                MoreMenuItem(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.more_settings),
                    onClick = onNavigateToSettings
                )
            }
        }

        // Support & Info Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                MoreMenuItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.more_donate),
                    onClick = { /* TODO: Implement Donate */ }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                MoreMenuItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.more_about),
                    onClick = { /* TODO: Implement About */ }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                MoreMenuItem(
                    icon = Icons.Default.Help,
                    title = stringResource(R.string.more_faq),
                    onClick = { /* TODO: Implement FAQ */ }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                MoreMenuItem(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.more_contact),
                    onClick = { 
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@occaecat.com")
                            putExtra(Intent.EXTRA_SUBJECT, "СвітлоЄ? Житомир - Зворотній зв'язок")
                        }
                        context.startActivity(Intent.createChooser(intent, "Написати нам"))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Developer Signature
        val githubUrl = stringResource(R.string.github_url)
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.dev_signature),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}
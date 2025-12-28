package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Про додаток") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with Squircle
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(SquircleShape)
                        .background(colorScheme.primaryContainer)
                )
                
                Icon(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorScheme.onPrimaryContainer
                )
            }

            // Info Card Grouped in one block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AboutItem(
                        icon = Icons.Default.Info,
                        iconColor = colorScheme.primary,
                        containerColor = colorScheme.primaryContainer,
                        title = "СвітлоЄ? Житомир",
                        subtitle = "Назва додатку",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(start = 64.dp))
                    
                    AboutItem(
                        icon = Icons.Default.Verified,
                        iconColor = colorScheme.secondary,
                        containerColor = colorScheme.secondaryContainer,
                        title = "1.2.0",
                        subtitle = "Версія",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(start = 64.dp))

                    AboutItem(
                        icon = Icons.Default.Person,
                        iconColor = colorScheme.tertiary,
                        containerColor = colorScheme.tertiaryContainer,
                        title = "dmitthedazed",
                        subtitle = "Автор",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dmitthedazed"))
                            context.startActivity(intent)
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 64.dp))

                    AboutItem(
                        icon = Icons.Default.Description,
                        iconColor = colorScheme.outline,
                        containerColor = colorScheme.surfaceVariant,
                        title = "MIT License",
                        subtitle = "Ліцензія",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://opensource.org/licenses/MIT"))
                            context.startActivity(intent)
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 64.dp))

                    AboutItem(
                        icon = Icons.Default.Code,
                        iconColor = colorScheme.error,
                        containerColor = colorScheme.errorContainer,
                        title = "GitHub",
                        subtitle = "Вихідний код",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/occaecat/ZTOESchedule"))
                            context.startActivity(intent)
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 64.dp))

                    AboutItem(
                        icon = Icons.Default.Language,
                        iconColor = colorScheme.primary,
                        containerColor = colorScheme.primaryContainer,
                        title = "ztoe.com.ua",
                        subtitle = "Джерело даних",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ztoe.com.ua"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            Text(
                text = "Made with ❤️ in Zhytomyr",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = containerColor.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconColor
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

val SquircleShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width * 0.5f, 0f)
            cubicTo(width * 0.85f, 0f, width, height * 0.15f, width, height * 0.5f)
            cubicTo(width, height * 0.85f, width * 0.85f, height, width * 0.5f, height)
            cubicTo(width * 0.15f, height, 0f, height * 0.85f, 0f, height * 0.5f)
            cubicTo(0f, height * 0.15f, width * 0.15f, 0f, width * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
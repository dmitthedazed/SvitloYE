package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.occaecat.ztoeschedule.data.model.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Современный экран отображения графика отключений
 * Дизайн в стиле скриншота с большой статус-карточкой
 */
@Composable
fun ModernScheduleScreen(
    selectedAddress: String,
    currentStatus: Schedule?,
    schedules: List<Schedule>,
    formattedMessage: String,
    onRefresh: () -> Unit,
    onChangeAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок с адресом
        item {
            AddressHeader(
                address = selectedAddress,
                onChangeAddress = onChangeAddress
            )
        }

        // Большая статус-карточка
        item {
            currentStatus?.let { status ->
                LargeStatusCard(
                    status = status,
                    schedules = schedules
                )
            }
        }

        // Предупреждающее сообщение
        if (formattedMessage.isNotBlank()) {
            item {
                WarningMessageCard(message = formattedMessage)
            }
        }

        // Нижние кнопки
        item {
            BottomButtons(
                onScheduleClick = { /* Показать график */ },
                onGroupClick = { /* Показать группу */ }
            )
        }

        // Полный график (опционально)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Повний графік",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(schedules) { schedule ->
            CompactScheduleItem(schedule = schedule)
        }
    }
}

/**
 * Заголовок с адресом и кнопкой уведомлений
 */
@Composable
private fun AddressHeader(
    address: String,
    onChangeAddress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onChangeAddress() }
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "РЕМ 1 • Житомир • вул. Київська, 7",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        IconButton(onClick = { /* Notifications */ }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Сповіщення",
                tint = Color.White
            )
        }
    }
}

/**
 * Большая статус-карточка с анимацией
 */
@Composable
private fun LargeStatusCard(
    status: Schedule,
    schedules: List<Schedule>
) {
    val isLightOn = status.color.lowercase() in listOf("white", "green")

    // Цвета для градиента
    val backgroundColor = if (isLightOn) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E7D32),
                Color(0xFF1B5E20)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFD32F2F),
                Color(0xFFB71C1C)
            )
        )
    }

    // Вычисляем прогресс и время до следующего изменения
    val (progress, timeRemaining) = calculateProgress(status, schedules)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Иконка состояния
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLightOn) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                // Текст статуса
                Text(
                    text = if (isLightOn) "Світло є" else "Відключення",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 36.sp
                )

                // Время до следующего изменения
                if (timeRemaining.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Залишилось у поточному вікні:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = timeRemaining,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                // Прогресс-бар
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.span.split("-").firstOrNull() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = status.span.split("-").lastOrNull() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Предупреждающая карточка с сообщением
 */
@Composable
private fun WarningMessageCard(message: String) {
    val isAttention = message.contains("УВАГА", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAttention) Color(0xFFFF6F00) else Color(0xFF424242)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isAttention) "Увага! Важлива інформація" else "Інформація",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Нижние кнопки навигации
 */
@Composable
private fun BottomButtons(
    onScheduleClick: () -> Unit,
    onGroupClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Кнопка "График"
        Card(
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2C)
            ),
            onClick = onScheduleClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Графік",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = "на сьогодні",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        // Кнопка "Група"
        Card(
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2C)
            ),
            onClick = onGroupClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Група 3.2",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = "Ваша черга",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Компактный элемент графика
 */
@Composable
private fun CompactScheduleItem(schedule: Schedule) {
    val isActive = schedule.color.lowercase() in listOf("white", "green")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Индикатор
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Время
            Text(
                text = schedule.span,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Статус
            Text(
                text = schedule.displayText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Вычисляет прогресс текущего временного окна и оставшееся время
 */
private fun calculateProgress(currentStatus: Schedule, schedules: List<Schedule>): Pair<Float, String> {
    try {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val times = currentStatus.span.split("-")
        if (times.size != 2) return Pair(0f, "")

        val start = LocalTime.parse(times[0].trim(), formatter)
        val end = LocalTime.parse(times[1].trim(), formatter)

        val totalMinutes = java.time.Duration.between(start, end).toMinutes().toFloat()
        val elapsedMinutes = java.time.Duration.between(start, now).toMinutes().toFloat()

        val progress = (elapsedMinutes / totalMinutes).coerceIn(0f, 1f)

        val remainingMinutes = (totalMinutes - elapsedMinutes).toLong()
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60

        val timeRemaining = when {
            hours > 0 -> "$hours год $minutes хв"
            else -> "$minutes хв"
        }

        return Pair(progress, timeRemaining)
    } catch (e: Exception) {
        return Pair(0f, "")
    }
}


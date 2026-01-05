# Live Update Notifications - Руководство

## Что такое Live Update?

Live Update (Promoted Ongoing Notifications) - это новый тип уведомлений в Android 15+ (API 35+), который система продвигает и отображает более заметно:

- 📍 **Закреплено вверху** панели уведомлений
- 🔒 **Показывается на экране блокировки**
- 🎯 **Status chip** в строке состояния с критической информацией
- 📂 **Развернуто по умолчанию** и не сворачивается
- ⚡ **Высокий приоритет** для важных активностей

## Реализация в ZTOESchedule

### 1. Разрешение в манифесте

```xml
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
```

✅ Уже добавлено в `AndroidManifest.xml`

### 2. Стили уведомлений

Приложение поддерживает 3 стиля с автоматическим fallback:

```kotlin
enum class StatusNotificationStyle {
    SIMPLE,           // Все версии Android
    LIVE_ACTIVITY,    // Android 12+ (API 31+) - хронометр, цвет
    PROMOTED          // Android 15+ (API 35+) - Live Update
}
```

### 3. Автоматический выбор стиля

`NotificationFactory.getBestSupportedStyle()` выбирает лучший доступный стиль:

```kotlin
fun getBestSupportedStyle(): StatusNotificationStyle {
    return when {
        // Live Update если API 35+ и пользователь включил
        Build.VERSION.SDK_INT >= 35 && canPostLiveUpdates() -> PROMOTED
        
        // Богатое уведомление с хронометром на API 31+
        Build.VERSION.SDK_INT >= 31 -> LIVE_ACTIVITY
        
        // Простое уведомление на старых версиях
        else -> SIMPLE
    }
}
```

### 4. Особенности Live Update уведомления

#### Status Chip
Показывает критическую информацию в свернутом виде:

```kotlin
// Короткий текст (до 7 символов) - время до конца
builder.setShortCriticalText("1час 30хв")

// Автоматический обратный отсчет
builder.setWhen(current.endMs)
builder.setUsesChronometer(true)
builder.setChronometerCountDown(true)
```

#### Требования к Live Update

✅ **Обязательно:**
- Standard/BigText/Progress стиль (не RemoteViews)
- `setOngoing(true)` - постоянное уведомление
- `setRequestPromotedOngoing(true)` - запрос промоушена
- `contentTitle` установлен
- Канал НЕ `IMPORTANCE_MIN`

❌ **Запрещено:**
- `customContentView` (RemoteViews)
- `setGroupSummary` (summary группы)
- `setColorized(true)` (colorized уведомление)

## Использование

### PowerStatusService

Сервис автоматически использует лучший стиль:

```kotlin
private suspend fun updateNotificationForAddress(address: SavedAddress) {
    // Автоматический выбор PROMOTED/LIVE_ACTIVITY/SIMPLE
    val style = notificationFactory.getBestSupportedStyle()
    
    val notification = smartNotificationManager.createStatusNotification(
        currentStatus = currentStatus,
        allSchedules = groupedSchedules,
        address = address.name,
        style = style
    )
    
    smartNotificationManager.updateStatusNotification(notification)
}
```

### Проверка возможности показа Live Updates

```kotlin
// Проверить может ли приложение показывать Live Updates
if (notificationFactory.canPostLiveUpdates()) {
    // Пользователь включил Live Updates в настройках
    // И устройство на Android 15+
}
```

### Открытие настроек Live Updates

```kotlin
// Отправить пользователя в настройки для включения Live Updates
val intent = Intent(Settings.ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS)
startActivity(intent)
```

## Когда использовать Live Updates

### ✅ Подходящие случаи

- **Активные процессы**: навигация, звонки, доставка
- **Инициировано пользователем**: явный запуск мониторинга
- **Требует внимания**: пользователь должен регулярно проверять статус
- **Временно чувствительно**: есть четкое начало и конец

**Наш случай**: Мониторинг электроэнергии идеален для Live Update:
- ⚡ Активный процесс (текущий статус электричества)
- 👤 Пользователь выбрал адрес для мониторинга
- ⏰ Требует постоянного внимания (когда вернут свет?)
- 🔄 Есть начало и конец (до следующего изменения)

### ❌ Неподходящие случаи

- Реклама и промо
- Сообщения в чате
- Календарные события
- Быстрый доступ к функциям приложения
- Фоновая информация (погода, курсы валют)

## API методы для Live Updates

### Проверка промоушена

```kotlin
// Проверить было ли уведомление продвинуто системой
notification.flags and Notification.FLAG_PROMOTED_ONGOING

// Проверить может ли уведомление быть продвинуто
notification.hasPromotableCharacteristics()

// Проверить разрешение пользователя
notificationManager.canPostPromotedNotifications()
```

## Техническая архитектура

```
PowerStatusService (foreground service)
    ↓ getBestSupportedStyle()
NotificationFactory
    ↓ createPromotedNotification() [API 35+]
    ├─ setRequestPromotedOngoing(true)
    ├─ setShortCriticalText("1г 30хв")
    ├─ setWhen() + setUsesChronometer()
    └─ BigTextStyle (required)
    ↓
SmartNotificationManager
    ↓ updateStatusNotification()
System promotes notification
    ↓
🎯 Status chip in status bar
📍 Top of notification shade
🔒 Lock screen
```

## Логирование

Для отладки Live Updates добавлены логи:

```kotlin
// NotificationFactory
"Requested Live Update promotion"
"Can post Live Updates: true/false"
"Using PROMOTED style (Live Update)"

// PowerStatusService
"Notification updated successfully (style=PROMOTED)"
```

Фильтр logcat:
```
tag:NotificationFactory OR tag:PowerStatusService
```

## Fallback стратегия

Приложение gracefully degraded в зависимости от версии Android:

| Android Version | API | Стиль | Особенности |
|----------------|-----|-------|-------------|
| 15+ | 35+ | **PROMOTED** | Live Update, status chip, promoted |
| 12-14 | 31-34 | **LIVE_ACTIVITY** | Хронометр, цвет, богатое содержание |
| <12 | <31 | **SIMPLE** | Стандартное уведомление с прогрессом |

## Тестирование

### На Android 15+ (API 35+)

1. Запустить приложение
2. Добавить адрес с приоритетом 1
3. Включить постоянное уведомление
4. Проверить:
   - Status chip в строке состояния
   - Уведомление закреплено вверху панели
   - Обратный отсчет времени работает
   - Уведомление на экране блокировки

### Проверка разрешений

```kotlin
// В приложении
if (!notificationFactory.canPostLiveUpdates()) {
    // Пользователь отключил Live Updates
    // Предложить включить через настройки
}
```

### Проверка через adb

```bash
# Проверить уровень API
adb shell getprop ro.build.version.sdk

# Проверить разрешения приложения
adb shell dumpsys package com.occaecat.ztoeschedule | grep permission

# Логи уведомлений
adb logcat | grep -E "NotificationFactory|PowerStatusService"
```

## Ссылки

- [Android Developers: Live Update Notifications](https://developer.android.com/develop/ui/views/notifications/create-live-update)
- [NotificationCompat.Builder API](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder)
- [Notification Policy Guide](https://developer.android.com/training/notify-user/build-notification)

## Changelog

### 2026-01-05
- ✅ Добавлено разрешение `POST_PROMOTED_NOTIFICATIONS`
- ✅ Реализован `createPromotedNotification()` с Live Update API
- ✅ Добавлен `getBestSupportedStyle()` для автоматического выбора
- ✅ Добавлен `canPostLiveUpdates()` для проверки разрешений
- ✅ Обновлен `PowerStatusService` для использования лучшего стиля
- ✅ Добавлены status chip с обратным отсчетом
- ✅ Документация и руководство

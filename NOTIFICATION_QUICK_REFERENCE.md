# 🚀 QUICK REFERENCE: Система Уведомлень (ОБНОВЛЕНО)

> **Последнее обновление**: 3 января 2026  
> **Статус**: Очищена от legacy кода, Phase 1-2 завершена

## Основные компоненты

### NotificationCoordinator (главный orkestrator)
```kotlin
// Отправить alert о смене статуса
notificationCoordinator.notifyStatusChange(
    address = savedAddress,
    oldStatus = previousStatus,
    newStatus = newStatus
)

// Обновить STATUS notification и UI
notificationCoordinator.updateStatusNotification(
    address = savedAddress,
    currentStatus = currentStatus,
    allSchedules = allSchedules
)

// Получить историю алертов для дебага
val recentAlerts = notificationCoordinator.getRecentAlerts()
```

### SmartNotificationManager (создание и постинг)
```kotlin
// Создать STATUS notification
val notification = smartNotificationManager.createStatusNotification(
    currentStatus = status,
    allSchedules = schedules,
    address = addressName,
    style = StatusNotificationStyle.LIVE_ACTIVITY
)

// Обновить существующее notification
smartNotificationManager.updateStatusNotification(notification)

// Отправить alert
smartNotificationManager.sendAlert(title, message, isOutage = true)
```

### PowerStatusService (persistent STATUS notification)
- Автоматически запускается при `PowerStatusService.start(context)`
- Обновляет каждую минуту через StateFlow + ticker
- Реагирует на смену адреса с graceful restart
- Network timeout protection (10 сек)

```kotlin
PowerStatusService.start(context)      // Start
PowerStatusService.stop(context)       // Stop
```

### PowerMonitorWorker (periodic checks)
- Запускается каждые 15 минут
- Проверяет расписание на все адреса
- Отправляет alerts через NotificationCoordinator
- Работает с exponential backoff на ошибках

### NotificationPolicy (правила отправки)
```kotlin
// Проверить может ли быть отправлен alert
val canSend = notificationPolicy.canSendAlert()

// Получить причину блокирования (для дебага)
val reason = notificationPolicy.getBlockReason()
// Вернет: "Notifications are globally disabled" / "System DND is active" / null
```

**Правила:**
1. Глобальный switch notifications enabled
2. System DND mode (Android 6+)
3. Quiet Hours (customizable start/end)
4. Priority Mode (All/Important/Silent)
5. Workday Mode (Mon-Fri only)

---

## 📊 Data Models

### AlertInfo
```kotlin
data class AlertInfo(
    val addressName: String,
    val previousStatus: ScheduleStatus,
    val newStatus: ScheduleStatus,
    val sentAtMs: Long,
    val title: String,
    val message: String,
    val isOutage: Boolean
)

// Check if within debounce window
alert.isWithinDebounce(currentTimeMs, debounceMs = 60_000)

// Check if matches another alert's status change
alert.matchesStatusChange(otherAlert)
```

### NotificationUpdate (sealed class)
```kotlin
when (update) {
    is NotificationUpdate.AlertSent -> { /* handle alert */ }
    is NotificationUpdate.StatusUpdated -> { /* handle status */ }
    is NotificationUpdate.AddressChanged -> { /* handle change */ }
    is NotificationUpdate.NotificationsToggled -> { /* handle toggle */ }
    is NotificationUpdate.ServiceStopped -> { /* handle stop */ }
}

// Observe updates
notificationCoordinator.recentUpdates.collect { update ->
    // React to change
}
```

### NotificationConfig
```kotlin
val config = NotificationConfig(
    enableCoordinator = true,
    enableDedupAlerts = true,
    enablePromotedStyle = true,
    alertDebounceMs = 60_000L,
    statusUpdateIntervalMs = 60_000L,
    workerCheckIntervalMinutes = 15L,
    networkTimeoutMs = 10_000L,
    enableDetailedLogging = false
)
```

---

## 🔐 Permissions

**Required (AndroidManifest.xml):**
```xml
<!-- For notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- For boot receiver -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- For foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

**Runtime check (Android 13+):**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED) {
        // Can post notifications
    }
}
```

---

## 📱 Notification Channels

| Channel | Priority | Sound | Vibration | Use Case |
|---------|----------|-------|-----------|----------|
| `CHANNEL_ALERTS_ID` | HIGH | ✅ | ✅ | Power status changes (alerts) |
| `CHANNEL_STATUS_ID` | LOW | ❌ | ❌ | Persistent status notification |
| `CHANNEL_INFO_ID` | DEFAULT | ✅ | ❌ | General updates |
| `CHANNEL_LIVE_ID` | DEFAULT | ❌ | ❌ | Live activity (legacy) |

---

## 🐛 Debugging

### View Alert History
```kotlin
val alerts = notificationCoordinator.getRecentAlerts()
alerts.forEach { alert ->
    Log.d("DEBUG", """
        Address: ${alert.addressName}
        Change: ${alert.previousStatus} → ${alert.newStatus}
        Sent at: ${Date(alert.sentAtMs)}
        Title: ${alert.title}
    """)
}
```

### Check Block Reason
```kotlin
val reason = notificationPolicy.getBlockReason()
if (reason != null) {
    Log.w("NOTIFICATION", "Alert blocked: $reason")
}
```

### View Active Notifications
```kotlin
val active = notificationState.getActiveNotificationsInfo()
Log.d("STATE", "Active notifications: $active")
```

### Enable Detailed Logging
```kotlin
val config = NotificationConfig(enableDetailedLogging = true)
```

---

## 🚨 Common Issues & Solutions

### Issue: Alerts not being sent
1. Check if notifications globally enabled: `Settings → Notifications → СвітлоЄ?`
2. Check if in Quiet Hours: `Settings → Advanced → Quiet Hours`
3. Check System DND mode
4. Check Priority Mode setting

**Debug:**
```kotlin
Log.d("NOTIFICATION", "Can send alert: ${notificationPolicy.canSendAlert()}")
Log.d("NOTIFICATION", "Block reason: ${notificationPolicy.getBlockReason()}")
```

### Issue: Duplicate alerts
- Should be prevented by deduplication (60 sec debounce)
- Check if `NotificationConfig.enableDedupAlerts = true`
- View recent alerts to verify no actual duplicates

### Issue: Service not starting
- Check `POST_NOTIFICATIONS` permission granted
- Check if notifications globally enabled
- Check logcat: `adb logcat | grep PowerStatusService`

### Issue: Slow updates
- Increase network timeout if on slow network: `networkTimeoutMs`
- Check WiFi/cellular connection
- View logs for `PowerMonitorWorker` execution

---

## 📚 Key Classes Location

```
app/src/main/java/com/occaecat/ztoeschedule/domain/notification/
├── NotificationCoordinator.kt          ⭐ Main orkestrator
├── SmartNotificationManager.kt         Notification creation/posting
├── PowerStatusService.kt               Persistent notification service
├── PowerMonitorWorker.kt               Periodic background checks
├── NotificationFactory.kt              Build notifications
├── NotificationPolicy.kt               Enforce notification rules
├── NotificationState.kt                Track active notifications
├── NotificationScheduler.kt            Manage WorkManager
├── NotificationActionReceiver.kt       Handle notification actions
├── BootReceiver.kt                    Restart after device boot
├── PowerStatusTileService.kt           Quick Settings tile
├── NotificationHelper.kt               Notification channels
├── NotificationIds.kt                  Notification ID constants
├── NotificationTextHelper.kt           Text formatting utilities
└── model/
    ├── NotificationConfig.kt           Configuration & feature flags
    ├── AlertInfo.kt                    Alert history tracking
    └── NotificationUpdate.kt           State change events
```

---

## 🔄 Integration Examples

### From Activity/Fragment
```kotlin
// Inject Coordinator
@Inject
lateinit var notificationCoordinator: NotificationCoordinator

// Send manual refresh
NotificationScheduler.runImmediateCheck(context)

// Handle address change
val oldAddress = savedAddress
val newAddress = newSavedAddress
notificationCoordinator.notifyAddressChanged(oldAddress, newAddress)
```

### From ViewModel
```kotlin
viewModelScope.launch {
    val address = getSelectedAddress()
    val schedules = repository.getSchedules(address.cherga, address.pidcherga)
    val currentStatus = ScheduleMapper.getCurrentGroupedStatus(schedules, now)
    
    // Update through coordinator
    notificationCoordinator.updateStatusNotification(
        address = address,
        currentStatus = currentStatus,
        allSchedules = schedules
    )
}
```

---

## 📋 Testing Checklist

- [ ] Alert deduplication works (same alert within 60 sec blocked)
- [ ] Address change triggers graceful restart
- [ ] Network timeout shows placeholder notification
- [ ] Quiet Hours block alerts correctly
- [ ] DND mode respected
- [ ] Priority mode filtering works
- [ ] Workday mode filters weekends
- [ ] Worker retries with exponential backoff
- [ ] Service starts without POST_NOTIFICATIONS permission
- [ ] QS tile updates correctly
- [ ] Widget updates synchronized with notification
- [ ] No crashes on rapid setting changes

---

**Last Updated:** Jan 3, 2026  
**Version:** 1.0  
**Status:** Production Ready ✅

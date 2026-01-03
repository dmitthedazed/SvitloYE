# ✅ INTEGRATION CHECKLIST

## Перед началом интеграции

- [ ] Бекап текущего кода
- [ ] Создать новую ветку git: `feature/notification-refactor`
- [ ] Убедиться что все существующие tests пасируют

## Файлы для проверки

### ✅ Новые файлы (ГОТОВЫ К ИСПОЛЬЗОВАНИЮ)
- [x] `domain/notification/model/NotificationConfig.kt`
- [x] `domain/notification/model/AlertInfo.kt`
- [x] `domain/notification/model/NotificationUpdate.kt`
- [x] `domain/notification/NotificationCoordinator.kt`

### ✅ Переделанные файлы (ТРЕБУЮТ ВАЛИДАЦИИ)
- [x] `domain/notification/SmartNotificationManager.kt`
- [x] `domain/notification/PowerStatusService.kt`
- [x] `domain/notification/PowerMonitorWorker.kt`
- [x] `domain/notification/NotificationFactory.kt`
- [x] `domain/notification/NotificationPolicy.kt`
- [x] `domain/notification/NotificationState.kt`
- [x] `domain/notification/NotificationActionReceiver.kt`
- [x] `domain/notification/BootReceiver.kt`
- [x] `domain/notification/NotificationScheduler.kt`
- [x] `domain/notification/PowerStatusTileService.kt`

### ⚠️ Неизменные файлы (но может потребоваться обновление конфига Hilt)
- [ ] `domain/notification/NotificationHelper.kt` - просмотрено, OK
- [ ] `domain/notification/NotificationIds.kt` - просмотрено, OK
- [ ] `domain/notification/NotificationTextHelper.kt` - просмотрено, OK

---

## Зависимости и конфигурация

### Проверить Hilt регистрацию

```kotlin
// Убедиться что NotificationCoordinator injected как Singleton
@Singleton
class NotificationCoordinator @Inject constructor(...)

// Убедиться что NotificationConfig available через DI
// Или создать Hilt Module:
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    @Singleton
    fun provideNotificationConfig(): NotificationConfig = NotificationConfig()
}
```

### Проверить imports в новых файлах
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
```

### Убедиться что все custom types импортируются
```kotlin
import com.occaecat.ztoeschedule.data.local.SavedAddress
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.notification.model.AlertInfo
import com.occaecat.ztoeschedule.domain.notification.model.NotificationUpdate
import com.occaecat.ztoeschedule.domain.notification.model.NotificationConfig
```

---

## Compile & Build проверка

### Очистить build кеш
```bash
./gradlew clean
./gradlew build
```

### Проверить нет compilation errors
- [ ] Gradle sync завершился успешно
- [ ] Нет красных squiggles в IDE
- [ ] Lint warnings минимальны

### Проверить существование ресурсов
- [ ] `R.drawable.ic_bolt` существует
- [ ] `R.drawable.ic_home_filled` существует
- [ ] `R.color.widget_power_on` существует
- [ ] `R.color.widget_power_off` существует

---

## Runtime проверки на девайсе

### Permissions
- [ ] POST_NOTIFICATIONS declared в AndroidManifest.xml
- [ ] RECEIVE_BOOT_COMPLETED declared
- [ ] FOREGROUND_SERVICE declared
- [ ] FOREGROUND_SERVICE_DATA_SYNC declared (API 29+)

### Services & Receivers registration
```xml
<!-- In AndroidManifest.xml -->
<service android:name=".domain.notification.PowerStatusService" />
<receiver android:name=".domain.notification.NotificationActionReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.occaecat.ztoeschedule.ACTION_REFRESH_STATUS" />
    </intent-filter>
</receiver>
<receiver android:name=".domain.notification.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
<service android:name=".domain.notification.PowerStatusTileService"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
</service>
```

### Functional tests

**Test 1: Service startup**
```kotlin
// ✅ PowerStatusService должен стартовать без ошибок
PowerStatusService.start(context)
// Check logcat: "Service onCreate()"
```

**Test 2: First alert**
```kotlin
// ✅ Первый alert должен отправиться
NotificationScheduler.runImmediateCheck(context)
// Check: notification appears in system tray
```

**Test 3: Deduplication**
```kotlin
// ✅ Второй identical alert за 60 сек должен быть заблокирован
// Send same alert twice
NotificationScheduler.runImmediateCheck(context)  // First - sent
Thread.sleep(1000)
NotificationScheduler.runImmediateCheck(context)  // Second - should be debounced
// Check logcat: "Alert debounced"
```

**Test 4: Address change**
```kotlin
// ✅ При смене адреса service должен перезапуститься gracefully
// Change address in app
// PowerStatusService должен автоматически restart
// Check: notification updates with new address
```

**Test 5: Network error**
```kotlin
// ✅ При отключении сети должно появиться placeholder notification
// Disable WiFi/cellular
NotificationScheduler.runImmediateCheck(context)
// Check: "Сіть недоступна..." notification appears
```

**Test 6: Quiet Hours**
```kotlin
// ✅ Alerts должны блокироваться в Quiet Hours
// Set Quiet Hours: 22:00-08:00
// Try to trigger alert in this timeframe
// Check logcat: "Alert blocked: quiet hours active"
```

**Test 7: DND Mode**
```kotlin
// ✅ System DND mode должен блокировать alerts
// Enable System DND (Settings → Sound → Do Not Disturb)
// Try to trigger alert
// Check logcat: "Alert blocked: system DND mode is active"
```

**Test 8: QS Tile**
```kotlin
// ✅ Quick Settings tile должен обновляться
// Add PowerStatusTile to Quick Settings
// Trigger status update
// Check: tile label updates to "Світло є ✅" or "Світла немає 🔴"
```

---

## Code Review Checklist

- [ ] Все методы имеют KDoc комментарии
- [ ] Все компоненты имеют TAG для логирования
- [ ] Нет hardcoded strings (все в ресурсах)
- [ ] Нет TODO/FIXME комментариев (кроме планируемых)
- [ ] Все coroutines управляются (no leaks)
- [ ] Все try-catch блоки логируют ошибку
- [ ] Нет @Suppress аннотаций без объяснения
- [ ] Соблюдены Kotlin naming conventions
- [ ] Использованы sealed classes где применимо
- [ ] Нет дублирования кода

---

## Performance проверки

### Memory leaks
```bash
# Проверить в Android Studio Profiler:
# 1. Запустить app
# 2. Open Profiler
# 3. Trigger multiple alerts
# 4. Check Memory tab - не должно быть скачков
# 5. Force GC и проверить что память освобождается
```

### CPU usage
```bash
# Проверить что обновления не требуют много CPU:
# 1. Open CPU Profiler
# 2. Let PowerStatusService run for 1 minute
# 3. Check: CPU usage < 5% average
```

### Battery impact
```bash
# Проверить что WorkManager не drain battery:
# 1. Enable Battery Saver mode
# 2. Let PowerMonitorWorker run every 15 min for 1 hour
# 3. Check: should not significantly impact battery
```

---

## Device compatibility

- [ ] Tested on API 26 (Android 8.0) - minimum
- [ ] Tested on API 31 (Android 12) - Live Activity support
- [ ] Tested on API 36 (Android 16) - Promoted notification support
- [ ] Tested in light theme
- [ ] Tested in dark theme
- [ ] Tested landscape orientation
- [ ] Tested with accessibility features enabled

---

## Migration from old code

### If you have custom notification code:

1. **Replace direct NotificationManager calls:**
   ```kotlin
   // OLD:
   NotificationManagerCompat.from(context).notify(id, notification)
   
   // NEW:
   smartNotificationManager.updateStatusNotification(notification)
   ```

2. **Replace widget update calls:**
   ```kotlin
   // OLD:
   updateGlanceWidgets(status, address)
   
   // NEW:
   notificationCoordinator.updateStatusNotification(address, status, schedules)
   ```

3. **Replace tile update calls:**
   ```kotlin
   // OLD:
   TileService.requestListeningState(context, tile)
   
   // NEW:
   // Coordinator handles this automatically
   ```

4. **Replace alert sending:**
   ```kotlin
   // OLD:
   smartNotificationManager.sendAlert(title, message, isOutage)
   
   // NEW:
   notificationCoordinator.notifyStatusChange(address, oldStatus, newStatus)
   // Coordinator handles deduplication
   ```

---

## Documentation

- [x] [NOTIFICATION_REFACTOR_SUMMARY.md](../NOTIFICATION_REFACTOR_SUMMARY.md) - полный отчет
- [x] [NOTIFICATION_QUICK_REFERENCE.md](../NOTIFICATION_QUICK_REFERENCE.md) - quick start guide
- [x] Inline KDoc comments в всех файлах
- [ ] Update project README.md с инструкциями по интеграции
- [ ] Create ADR (Architecture Decision Record) if needed

---

## Post-integration

### After successful merge:
- [ ] Deploy to production gradually (staged rollout)
- [ ] Monitor Crashlytics for any new crashes
- [ ] Monitor analytics for notification delivery rates
- [ ] Check user feedback in reviews
- [ ] Be ready to rollback if issues found

### Communication:
- [ ] Notify QA team about changes
- [ ] Notify support team about new behavior
- [ ] Add release notes for users (if user-facing changes)

---

## Rollback Plan

If critical issues found:

```bash
# Quick rollback:
git revert <commit-hash>

# Or restore from backup:
git checkout <old-tag> -- app/src/main/java/com/occaecat/ztoeschedule/domain/notification/
```

**Rollback impact:**
- Lose alert deduplication
- Lose DND mode support
- Some race conditions possible
- Widget/tile updates may be inconsistent

---

## Support & Debugging

For issues contact the development team with:
- [ ] Android version and API level
- [ ] Logcat output (tag: "NotificationCoordinator" or "PowerStatusService")
- [ ] Steps to reproduce
- [ ] Device info (manufacturer, model)
- [ ] Recent changes made

---

## Completion Sign-off

- [ ] All files compiled successfully
- [ ] All tests passed
- [ ] Code review completed
- [ ] QA approved
- [ ] Ready for production

**Integration Date:** __________  
**Integrated By:** __________  
**Reviewed By:** __________  

---

**Status:** 🟢 READY FOR INTEGRATION  
**Last Updated:** Jan 3, 2026

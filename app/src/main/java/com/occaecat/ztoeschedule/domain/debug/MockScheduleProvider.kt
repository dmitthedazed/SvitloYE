package com.occaecat.ztoeschedule.domain.debug

import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mock schedule provider for debugging notifications.
 * 
 * This class generates a rotating schedule that changes status every minute,
 * simulating real-world power status transitions for testing alert notifications.
 * 
 * Usage:
 * - Use cherga=9999 and pidcherga=9999 for demo location
 * - Status rotates: Available → Outage → Probable → repeat
 * - Each status lasts exactly 1 minute
 */
object MockScheduleProvider {
    
    private const val TAG = "MockScheduleProvider"
    private const val DEMO_CHERGA = 9999
    private const val DEMO_PIDCHERGA = 9999
    @Volatile
    private var cachedDate: String? = null
    @Volatile
    private var cachedSchedules: List<Schedule> = emptyList()
    
    /**
     * Check if the given cherga/pidcherga combination is for demo location
     */
    fun isDemoLocation(cherga: Int, pidcherga: Int): Boolean {
        return cherga == DEMO_CHERGA && pidcherga == DEMO_PIDCHERGA
    }
    
    /**
     * Generate mock schedule that rotates through different statuses every minute
     * 
     * The schedule alternates between:
     * - 🟢 Available (1 minute)
     * - 🔴 Outage (1 minute)  
     * - 🟡 Probable (1 minute)
     * 
     * This ensures frequent status changes to test alert notifications
     */
    fun generateMockSchedule(): List<Schedule> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val today = dateFormat.format(calendar.time)
        val cached = cachedSchedules
        if (cachedDate == today && cached.isNotEmpty()) {
            return cached
        }
        val schedules = mutableListOf<Schedule>()
        
        // Create 3-minute rotating cycle starting from current hour
        val statuses = listOf(
            ScheduleStatus.Available,
            ScheduleStatus.Outage,
            ScheduleStatus.Probable
        )
        
        // Generate schedule for next 24 hours with 1-minute intervals
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var minuteCounter = 0
        
        for (hour in 0..23) {
            for (minute in 0..59) {
                val startCalendar = calendar.clone() as Calendar
                startCalendar.set(Calendar.HOUR_OF_DAY, hour)
                startCalendar.set(Calendar.MINUTE, minute)
                
                val endCalendar = startCalendar.clone() as Calendar
                endCalendar.add(Calendar.MINUTE, 1)
                
                val startTime = timeFormat.format(startCalendar.time)
                val endTime = timeFormat.format(endCalendar.time)
                
                // Rotate through statuses every minute
                val status = statuses[minuteCounter % 3]
                val color = when (status) {
                    ScheduleStatus.Available -> "green"
                    ScheduleStatus.Outage -> "red"
                    ScheduleStatus.Probable -> "yellow"
                    ScheduleStatus.Unknown -> "white"
                }
                minuteCounter++
                
                schedules.add(
                    Schedule(
                        date = today,
                        span = "$startTime-$endTime",
                        color = color,
                        text = null
                    )
                )
            }
        }
        
        cachedDate = today
        cachedSchedules = schedules.toList()

        android.util.Log.d(TAG, "Generated ${schedules.size} mock schedule entries")
        android.util.Log.d(TAG, "Current time: ${timeFormat.format(Calendar.getInstance().time)}")

        // Log next 5 status changes for debugging
        val now = Calendar.getInstance()
        val currentMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        android.util.Log.d(TAG, "Next 5 status changes:")
        for (i in 0..4) {
            val targetMinute = (currentMinuteOfDay + i) % (24 * 60)
            if (targetMinute < schedules.size) {
                val schedule = schedules[targetMinute]
                android.util.Log.d(TAG, "  ${schedule.span}: ${schedule.status}")
            }
        }

        return cachedSchedules
    }
    
    /**
     * Get demo address name for display
     */
    fun getDemoAddressName(): String = "🔧 ТЕСТ (зміна кожну хвилину)"
    
    /**
     * Get demo location description
     */
    fun getDemoLocationDescription(): String {
        return "Демо-локація для тестування уведомлень. " +
                "Статус змінюється кожну хвилину: Доступно → Відключення → Можливо → повтор."
    }
}

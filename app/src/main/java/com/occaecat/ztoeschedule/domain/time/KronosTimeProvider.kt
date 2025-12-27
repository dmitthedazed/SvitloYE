package com.occaecat.ztoeschedule.domain.time

import com.lyft.kronos.KronosClock
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Implementation of TimeProvider using Kronos for NTP synchronization.
 * Falls back to System.currentTimeMillis() if NTP is not available yet.
 */
class KronosTimeProvider @Inject constructor(
    private val kronosClock: KronosClock
) : TimeProvider {

    override fun now(): Long {
        return kronosClock.getCurrentTimeMs()
    }

    override fun nowCalendar(): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Kyiv"))
        calendar.timeInMillis = now()
        return calendar
    }
}

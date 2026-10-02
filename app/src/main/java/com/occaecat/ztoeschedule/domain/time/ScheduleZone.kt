package com.occaecat.ztoeschedule.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

/**
 * The schedule is published in Kyiv time: dates ("dd.MM.yyyy"), spans and server headers.
 * Everything that buckets the schedule into days must use this zone, not the device's.
 */
object ScheduleZone {
    val zoneId: ZoneId = ZoneId.of("Europe/Kyiv")
    val timeZone: TimeZone = TimeZone.getTimeZone(zoneId)

    fun today(nowMs: Long): LocalDate = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()

    /** Today's date in the API format "dd.MM.yyyy". */
    fun todayString(nowMs: Long): String =
        today(nowMs).let { "%02d.%02d.%04d".format(Locale.US, it.dayOfMonth, it.monthValue, it.year) }

    /** Start of the next schedule day, in epoch ms. */
    fun nextMidnight(nowMs: Long): Long =
        today(nowMs).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
}

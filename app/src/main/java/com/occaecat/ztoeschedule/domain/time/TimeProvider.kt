package com.occaecat.ztoeschedule.domain.time

import java.util.Calendar

/**
 * Interface for providing the current time.
 * Abstracts the source of time (System, NTP/Kronos, etc).
 */
interface TimeProvider {
    /**
     * Returns the current time in milliseconds.
     * Guaranteed to be monotonic and synced with NTP if available.
     */
    fun now(): Long

    /**
     * Returns a Calendar instance set to the current accurate time.
     * TimeZone is handled by the implementation (defaulting to system or specific zone).
     */
    fun nowCalendar(): Calendar
}

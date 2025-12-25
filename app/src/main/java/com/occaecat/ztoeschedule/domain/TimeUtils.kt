package com.occaecat.ztoeschedule.domain

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilities for time formatting respecting system settings
 */
object TimeUtils {

    /**
     * Formats an API time string (HH:mm) into the system preferred format (12h or 24h)
     */
    fun formatToSystemTime(context: Context, timeStr: String): String {
        return try {
            val is24Hour = DateFormat.is24HourFormat(context)
            val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
            
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        } catch (e: Exception) {
            timeStr
        }
    }

    /**
     * Formats a time span string (e.g., "08:00-12:00") into system format
     */
    fun formatSpanToSystem(context: Context, span: String): String {
        return try {
            val parts = span.split("-")
            if (parts.size != 2) return span
            
            val start = formatToSystemTime(context, parts[0].trim())
            val end = formatToSystemTime(context, parts[1].trim())
            
            "$start — $end"
        } catch (e: Exception) {
            span
        }
    }
}

package com.occaecat.ztoeschedule.domain

import android.content.Context
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ScheduleStatus

/**
 * Helper for localizing status texts.
 * 
 * Since data classes don't have Context access, use this helper
 * in UI layers to get localized status descriptions.
 */
object StatusLocalizationHelper {
    
    /**
     * Get localized display text for a ScheduleStatus.
     */
    fun getDisplayText(context: Context, status: ScheduleStatus): String {
        return when (status) {
            ScheduleStatus.Outage -> context.getString(R.string.status_display_outage)
            ScheduleStatus.Available -> context.getString(R.string.status_display_available)
            ScheduleStatus.Probable -> context.getString(R.string.status_display_probable)
            ScheduleStatus.Unknown -> context.getString(R.string.status_unknown)
        }
    }
    
    /**
     * Get localized display text for a GroupedSchedule.
     * Prefers the API-provided text if available, falls back to localized status.
     */
    fun getDisplayText(context: Context, schedule: GroupedSchedule): String {
        return schedule.text ?: getDisplayText(context, schedule.status)
    }
}

/**
 * Extension function to get localized display text for GroupedSchedule.
 */
fun GroupedSchedule.getLocalizedDisplayText(context: Context): String {
    return StatusLocalizationHelper.getDisplayText(context, this)
}

/**
 * Extension function to get localized display text for ScheduleStatus.
 */
fun ScheduleStatus.getLocalizedDisplayText(context: Context): String {
    return StatusLocalizationHelper.getDisplayText(context, this)
}

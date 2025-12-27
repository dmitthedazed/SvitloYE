package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a Schedule entry for power outages
 */
data class Schedule(
    @SerializedName("date")
    val date: String,

    @SerializedName("span")
    val span: String, // Format: "HH:mm-HH:mm"

    @SerializedName("color")
    val color: String,

    @SerializedName("text")
    val text: String? = null
) {
    val status: ScheduleStatus
        get() = ScheduleStatus.fromColor(color)

    /**
     * Get display text based on color if text is null
     */
    val displayText: String
        get() = text ?: when (status) {
            ScheduleStatus.OUTAGE -> "Відключення"
            ScheduleStatus.AVAILABLE -> "Світло є"
            ScheduleStatus.PROBABLE -> "Можливе відключення"
            else -> "Невідомо"
        }
}

enum class ScheduleStatus {
    AVAILABLE,   // Green/White
    OUTAGE,      // Red
    PROBABLE,    // Yellow
    UNKNOWN;

    companion object {
        fun fromColor(color: String): ScheduleStatus = when (color.lowercase()) {
            "white", "green" -> AVAILABLE
            "red" -> OUTAGE
            "yellow" -> PROBABLE
            else -> UNKNOWN
        }
    }
}



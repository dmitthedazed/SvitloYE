package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName
import androidx.compose.runtime.Immutable

/**
 * Represents a Schedule entry for power outages
 */
@Immutable
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
            ScheduleStatus.Outage -> "Відключення"
            ScheduleStatus.Available -> "Світло є"
            ScheduleStatus.Probable -> "Можливе відключення"
            else -> "Невідомо"
        }
}

@Immutable
enum class ScheduleStatus {
    Available,   // Green/White
    Outage,      // Red
    Probable,    // Yellow
    Unknown;

    companion object {
        fun fromColor(color: String): ScheduleStatus = when (color.lowercase()) {
            "white", "green" -> Available
            "red" -> Outage
            "yellow" -> Probable
            else -> Unknown
        }
    }
}



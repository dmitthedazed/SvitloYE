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
    /**
     * Get display text based on color if text is null
     */
    val displayText: String
        get() = text ?: when (color.lowercase()) {
            "red" -> "Відключення"
            "white" -> "Світло є"
            "yellow" -> "Можливе відключення"
            "green" -> "Світло є"
            else -> "Невідомо"
        }
}



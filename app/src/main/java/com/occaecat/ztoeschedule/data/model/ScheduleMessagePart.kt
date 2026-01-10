package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a message part related to schedule information
 */
data class ScheduleMessagePart(
    @SerializedName("id")
    val id: Int,

    @SerializedName("text")
    val text: String
)


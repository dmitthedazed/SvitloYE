package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a City entity
 */
data class City(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("rem_id")
    val remId: String
)


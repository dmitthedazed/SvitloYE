package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a REM (Regional Energy Management) entity
 */
data class Rem(
    @SerializedName("name")
    val name: String,
    @SerializedName("id")
    val id: String
)

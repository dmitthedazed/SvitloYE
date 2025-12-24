package com.occaecat.ztoeschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents an Address entity with queue identifiers
 */
data class Address(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("cherga")
    val cherga: Int,

    @SerializedName("pidcherga")
    val pidcherga: Int
) {
    /**
     * Get parsed house numbers from the raw name string
     * Removes utility text like "побутові споживачі"
     */
    fun getParsedHouseNumbers(): List<String> {
        return parseRawAddressString(name)
    }

    companion object {
        /**
         * Parse raw address string containing multiple house numbers
         * Example: "25 - побутові споживачі, 27А - побутові споживачі"
         * Result: ["25", "27А"]
         */
        private fun parseRawAddressString(raw: String): List<String> {
            return raw
                .split(",")
                .map { it.trim() }
                .mapNotNull { entry ->
                    // Remove suffix patterns like " - побутові споживачі"
                    val cleaned = entry
                        .replace(Regex("""\s*-\s*побутові споживачі\s*"""), "")
                        .replace(Regex("""\s*-\s*\w+\s+споживачі\s*"""), "")
                        .trim()

                    if (cleaned.isNotEmpty()) cleaned else null
                }
                .distinct()
        }
    }
}


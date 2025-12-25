package com.occaecat.ztoeschedule.data.model

import com.occaecat.ztoeschedule.data.local.SavedSelection
import java.util.UUID

/**
 * Represents a saved user address with metadata
 */
data class SavedAddress(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // User defined name: "Home", "Work"
    val iconName: String, // "home", "work", "school", "apartment", "other"
    val priority: Int, // 1 is highest (Main)
    
    // Location Data
    val remId: String,
    val remName: String,
    val cityId: String,
    val cityName: String,
    val streetId: String,
    val streetName: String,
    val addressId: String,
    val addressName: String,
    val cherga: Int,
    val pidcherga: Int
)

/**
 * Maps SavedSelection (DataStore) to SavedAddress
 */
fun SavedSelection.toSavedAddress(name: String, icon: String, priority: Int): SavedAddress {
    return SavedAddress(
        name = name,
        iconName = icon,
        priority = priority,
        remId = remId ?: "",
        remName = remName ?: "",
        cityId = cityId ?: "",
        cityName = cityName ?: "",
        streetId = streetId ?: "",
        streetName = streetName ?: "",
        addressId = addressId,
        addressName = addressName,
        cherga = cherga,
        pidcherga = pidcherga
    )
}

/**
 * Maps SavedAddress to SavedSelection (for DataStore)
 */
fun SavedAddress.toSavedSelection(): SavedSelection {
    return SavedSelection(
        remId = remId,
        remName = remName,
        cityId = cityId,
        cityName = cityName,
        streetId = streetId,
        streetName = streetName,
        addressId = addressId,
        addressName = addressName,
        cherga = cherga,
        pidcherga = pidcherga
    )
}

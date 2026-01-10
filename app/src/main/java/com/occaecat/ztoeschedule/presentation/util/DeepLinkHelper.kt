package com.occaecat.ztoeschedule.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.occaecat.ztoeschedule.data.model.SavedAddress

object DeepLinkHelper {
    private const val SCHEME_CUSTOM = "zt-energy"
    private const val HOST_CUSTOM = "schedule"
    
    private const val SCHEME_WEB = "https"
    private const val AUTHORITY_WEB = "dmitthedazed.github.io"
    private const val PATH_WEB = "svitlo-ye-zhytomyr"
    
    private const val PARAM_STREET_ID = "streetId"
    private const val PARAM_ADDRESS_ID = "addressId"
    private const val PARAM_HOUSE_ID = "houseId"
    private const val PARAM_HOUSE_NAME = "houseName"
    private const val PARAM_ADDRESS_NAME = "addressName"
    private const val PARAM_NAME = "name"
    private const val PARAM_STREET_ID_SHORT = "s"
    private const val PARAM_ADDRESS_ID_SHORT = "a"
    private const val PARAM_HOUSE_NAME_SHORT = "hn"
    private const val PARAM_CITY_NAME_SHORT = "cn"
    private const val PARAM_STREET_NAME_SHORT = "sn"
    private const val PARAM_REM_ID_SHORT = "r"
    private const val PARAM_REM_NAME_SHORT = "rn"
    private const val PARAM_CITY_ID_SHORT = "c"
    private const val PARAM_CHERGA_SHORT = "ch"
    private const val PARAM_PIDCHERGA_SHORT = "pch"
    
    private const val PARAM_CITY_NAME = "cityName"
    private const val PARAM_STREET_NAME = "streetName"

    data class DeepLinkParams(
        val streetId: String?,
        val addressId: String?,
        val houseName: String?,
        val cityName: String? = null,
        val streetName: String? = null,
        val remId: String? = null,
        val remName: String? = null,
        val cityId: String? = null,
        val cherga: Int? = null,
        val pidcherga: Int? = null
    )

    // TODO: Ensure assetlinks.json is uploaded to https://dmitthedazed.github.io/.well-known/ 
    // for App Links auto-verification. See README.md for instructions.

    /**
     * Generates a web deep link with full address data
     */
    fun generateLink(
        streetId: String, 
        addressId: String, 
        houseName: String? = null, 
        cityName: String? = null, 
        streetName: String? = null,
        remId: String? = null,
        remName: String? = null,
        cityId: String? = null,
        cherga: Int? = null,
        pidcherga: Int? = null
    ): String {
        val builder = Uri.Builder()
            .scheme(SCHEME_WEB)
            .authority(AUTHORITY_WEB)
            .appendPath(PATH_WEB)
            .appendPath("") // Adds trailing slash
            .appendQueryParameter(PARAM_STREET_ID_SHORT, streetId)
            .appendQueryParameter(PARAM_ADDRESS_ID_SHORT, addressId)
        
        if (!houseName.isNullOrBlank()) builder.appendQueryParameter(PARAM_HOUSE_NAME_SHORT, houseName)
        if (!cityName.isNullOrBlank()) builder.appendQueryParameter(PARAM_CITY_NAME_SHORT, cityName)
        if (!streetName.isNullOrBlank()) builder.appendQueryParameter(PARAM_STREET_NAME_SHORT, streetName)
        if (!remId.isNullOrBlank()) builder.appendQueryParameter(PARAM_REM_ID_SHORT, remId)
        if (!remName.isNullOrBlank()) builder.appendQueryParameter(PARAM_REM_NAME_SHORT, remName)
        if (!cityId.isNullOrBlank()) builder.appendQueryParameter(PARAM_CITY_ID_SHORT, cityId)
        if (cherga != null) builder.appendQueryParameter(PARAM_CHERGA_SHORT, cherga.toString())
        if (pidcherga != null) builder.appendQueryParameter(PARAM_PIDCHERGA_SHORT, pidcherga.toString())
        
        return builder.build().toString()
    }

    /**
     * Shares the deep link via system chooser.
     */
    fun shareLink(context: Context, address: SavedAddress) {
        val houseName = address.addressName.ifBlank { address.name }
        val link = generateLink(
            streetId = address.streetId,
            addressId = address.addressId,
            houseName = houseName,
            cityName = address.cityName,
            streetName = address.streetName,
            remId = address.remId,
            remName = address.remName,
            cityId = address.cityId,
            cherga = address.cherga,
            pidcherga = address.pidcherga
        )
        val text = "Подивись графік відключень для ${address.name} у додатку СвітлоЄ:\n$link"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поділитися посиланням"))
    }

    /**
     * Extracts parameters from a deep link URI.
     */
    fun parseUri(uri: Uri): DeepLinkParams? {
        // Supports both custom scheme and GitHub Pages web link
        val isCustom = uri.scheme == SCHEME_CUSTOM && uri.host == HOST_CUSTOM
        val isWeb = uri.scheme == SCHEME_WEB && uri.host == AUTHORITY_WEB && uri.path?.startsWith("/$PATH_WEB") == true
        
        if (!isCustom && !isWeb) return null
        
        val streetId = uri.getQueryParameter(PARAM_STREET_ID_SHORT)
            ?: uri.getQueryParameter(PARAM_STREET_ID)
        val addressId = uri.getQueryParameter(PARAM_ADDRESS_ID_SHORT)
            ?: uri.getQueryParameter(PARAM_ADDRESS_ID)
            ?: uri.getQueryParameter(PARAM_HOUSE_ID)
        val houseName = uri.getQueryParameter(PARAM_HOUSE_NAME_SHORT)
            ?: uri.getQueryParameter(PARAM_HOUSE_NAME)
            ?: uri.getQueryParameter(PARAM_ADDRESS_NAME)
            ?: uri.getQueryParameter(PARAM_NAME)
        val cityName = uri.getQueryParameter(PARAM_CITY_NAME_SHORT)
            ?: uri.getQueryParameter(PARAM_CITY_NAME)
        val streetName = uri.getQueryParameter(PARAM_STREET_NAME_SHORT)
            ?: uri.getQueryParameter(PARAM_STREET_NAME)
            
        val remId = uri.getQueryParameter(PARAM_REM_ID_SHORT) ?: uri.getQueryParameter("remId")
        val remName = uri.getQueryParameter(PARAM_REM_NAME_SHORT) ?: uri.getQueryParameter("remName")
        val cityId = uri.getQueryParameter(PARAM_CITY_ID_SHORT) ?: uri.getQueryParameter("cityId")
        val cherga = (uri.getQueryParameter(PARAM_CHERGA_SHORT) ?: uri.getQueryParameter("cherga"))?.toIntOrNull()
        val pidcherga = (uri.getQueryParameter(PARAM_PIDCHERGA_SHORT) ?: uri.getQueryParameter("pidcherga"))?.toIntOrNull()
            
        return DeepLinkParams(streetId, addressId, houseName, cityName, streetName, remId, remName, cityId, cherga, pidcherga)
    }
}

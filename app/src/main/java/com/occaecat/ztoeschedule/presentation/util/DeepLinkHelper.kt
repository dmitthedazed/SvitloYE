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
    private const val PARAM_HOUSE_ID = "houseId"

    // TODO: Ensure assetlinks.json is uploaded to https://dmitthedazed.github.io/.well-known/ 
    // for App Links auto-verification. See README.md for instructions.

    /**
     * Generates a web deep link: https://dmitthedazed.github.io/svitlo-ye-zhytomyr/?streetId=...&houseId=...
     */
    fun generateLink(streetId: String, houseId: String): String {
        return Uri.Builder()
            .scheme(SCHEME_WEB)
            .authority(AUTHORITY_WEB)
            .appendPath(PATH_WEB)
            .appendQueryParameter(PARAM_STREET_ID, streetId)
            .appendQueryParameter(PARAM_HOUSE_ID, houseId)
            .build()
            .toString()
    }

    /**
     * Shares the deep link via system chooser.
     */
    fun shareLink(context: Context, address: SavedAddress) {
        val link = generateLink(address.streetId, address.addressId)
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
    fun parseUri(uri: Uri): Pair<String?, String?>? {
        // Supports both custom scheme and GitHub Pages web link
        val isCustom = uri.scheme == SCHEME_CUSTOM && uri.host == HOST_CUSTOM
        val isWeb = uri.scheme == SCHEME_WEB && uri.host == AUTHORITY_WEB && uri.path?.startsWith("/$PATH_WEB") == true
        
        if (!isCustom && !isWeb) return null
        
        val streetId = uri.getQueryParameter(PARAM_STREET_ID)
        val houseId = uri.getQueryParameter(PARAM_HOUSE_ID)
        return Pair(streetId, houseId)
    }
}

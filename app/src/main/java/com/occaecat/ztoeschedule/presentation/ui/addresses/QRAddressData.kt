package com.occaecat.ztoeschedule.presentation.ui.addresses

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Data class representing address information encoded in QR code.
 * 
 * QR Code Format (JSON):
 * {
 *   "v": 1,                    // version
 *   "remId": "123",
 *   "cityId": "456", 
 *   "streetId": "789",
 *   "addressId": "012",
 *   "cherga": 1,
 *   "pidcherga": 2,
 *   "name": "Дім"              // optional display name
 * }
 * 
 * Alternative URL format (short):
 * https://dmitthedazed.github.io/svitlo-ye-zhytomyr/?s=789&a=012&hn=12%D0%91
 *
 * Legacy URL format:
 * zt-energy://schedule/add?remId=123&cityId=456&streetId=789&addressId=012&cherga=1&pidcherga=2
 */
data class QRAddressData(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("remId") val remId: String? = null,
    @SerializedName("cityId") val cityId: String? = null,
    @SerializedName("streetId") val streetId: String,
    @SerializedName("addressId") val addressId: String,
    @SerializedName("cherga") val cherga: Int? = null,
    @SerializedName("pidcherga") val pidcherga: Int? = null,
    @SerializedName("name") val displayName: String? = null,
    // Additional metadata for display
    @SerializedName("remName") val remName: String? = null,
    @SerializedName("cityName") val cityName: String? = null,
    @SerializedName("streetName") val streetName: String? = null,
    @SerializedName("addressName") val addressName: String? = null,
    @SerializedName("houseName") val houseName: String? = null
) {
    fun preferredHouseName(): String? {
        return houseName?.takeIf { it.isNotBlank() }
            ?: addressName?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
    }

    fun isFullData(): Boolean {
        return !remId.isNullOrBlank()
            && !cityId.isNullOrBlank()
            && !streetId.isNullOrBlank()
            && !addressId.isNullOrBlank()
            && cherga != null
            && pidcherga != null
    }

    companion object {
        private val gson = Gson()
        
        /**
         * Parse QR code content to QRAddressData
         * Supports both JSON format and URL format
         */
        fun parse(content: String): Result<QRAddressData> = runCatching {
            val trimmed = content.trim()
            
            when {
                // JSON format
                trimmed.startsWith("{") -> {
                    gson.fromJson(trimmed, QRAddressData::class.java)
                }
                
                // URL format: zt-energy://schedule/add?... or short web format
                trimmed.startsWith("zt-energy://") -> {
                    parseUrl(trimmed)
                }
                
                // HTTPS format: https://dmitthedazed.github.io/svitlo-ye-zhytomyr/?...
                trimmed.contains("svitlo-ye-zhytomyr") && trimmed.contains("?") -> {
                    parseUrl(trimmed)
                }
                
                else -> throw IllegalArgumentException("Невідомий формат QR-коду")
            }
        }
        
        private fun parseUrl(url: String): QRAddressData {
            val queryStart = url.indexOf('?')
            if (queryStart == -1) {
                throw IllegalArgumentException("URL D�D� D��-�?�,D,�,�O D�D��?D�D�D�,�?�-D�")
            }
            
            val params = url.substring(queryStart + 1)
                .split('&')
                .associate { param ->
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2) {
                        java.net.URLDecoder.decode(parts[0], "UTF-8") to 
                            java.net.URLDecoder.decode(parts[1], "UTF-8")
                    } else {
                        parts[0] to ""
                    }
                }

            val streetId = params["s"] ?: params["streetId"]
                ?: throw IllegalArgumentException("streetId D_D�D_D�'�?D�D�D_D�D,D1")
            val addressId = params["a"] ?: params["addressId"] ?: params["houseId"]
                ?: throw IllegalArgumentException("addressId D_D�D_D�'�?D�D�D_D�D,D1")

            val cityName = params["cn"] ?: params["cityName"]
            val streetName = params["sn"] ?: params["streetName"]
            
            val remId = params["r"] ?: params["remId"]
            val remName = params["rn"] ?: params["remName"]
            val cityId = params["c"] ?: params["cityId"]
            val cherga = (params["ch"] ?: params["cherga"])?.toIntOrNull()
            val pidcherga = (params["pch"] ?: params["pidcherga"])?.toIntOrNull()

            return QRAddressData(
                version = params["v"]?.toIntOrNull() ?: 1,
                remId = remId,
                cityId = cityId,
                streetId = streetId,
                addressId = addressId,
                cherga = cherga,
                pidcherga = pidcherga,
                displayName = params["name"],
                remName = remName,
                cityName = cityName,
                streetName = streetName,
                addressName = params["addressName"],
                houseName = params["hn"] ?: params["houseName"]
            )
        }
        /**
         * Generate QR code content for sharing
         */
        fun generateQRContent(
            remId: String,
            cityId: String,
            streetId: String,
            addressId: String,
            cherga: Int,
            pidcherga: Int,
            displayName: String? = null,
            remName: String? = null,
            cityName: String? = null,
            streetName: String? = null,
            addressName: String? = null
        ): String {
            val houseName = addressName?.takeIf { it.isNotBlank() } ?: displayName
            return com.occaecat.ztoeschedule.presentation.util.DeepLinkHelper.generateLink(
                streetId = streetId,
                addressId = addressId,
                houseName = houseName,
                cityName = cityName,
                streetName = streetName,
                remId = remId,
                remName = remName,
                cityId = cityId,
                cherga = cherga,
                pidcherga = pidcherga
            )
        }
    }
}

sealed class QRScanResult {
    data class Success(val data: QRAddressData) : QRScanResult()
    data class Error(val message: String) : QRScanResult()
    data object Cancelled : QRScanResult()
}




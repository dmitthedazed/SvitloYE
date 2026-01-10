package com.occaecat.ztoeschedule.data.network

import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.data.model.Street
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API Service for GPV (ZTOE) endpoints
 * Base URL: https://www.ztoe.com.ua/gpv/api/
 */
interface GpvApiService {

    /**
     * Fetch list of regional energy managements (REMs)
     */
    @GET("api-rem.php")
    suspend fun getRemList(): List<Rem>

    /**
     * Fetch list of cities for a specific REM
     * @param remId The REM identifier
     */
    @GET("api-city.php")
    suspend fun getCityList(
        @Query("rem_id") remId: String
    ): List<City>

    /**
     * Fetch list of streets for a specific city
     * @param cityId The city identifier
     */
    @GET("api-street.php")
    suspend fun getStreetList(
        @Query("city_id") cityId: String
    ): List<Street>

    /**
     * Fetch list of addresses for a specific street
     * @param streetId The street identifier
     */
    @GET("api-address.php")
    suspend fun getAddressList(
        @Query("street_id") streetId: String
    ): List<Address>

    /**
     * Fetch schedule for specific queue identifiers
     * @param cherga Queue identifier
     * @param pidcherga Sub-queue identifier
     */
    @GET("api-schedule.php")
    suspend fun getSchedule(
        @Query("cherga_id") cherga: Int,
        @Query("pidcherga_id") pidcherga: Int
    ): Response<List<Schedule>>

    /**
     * Fetch informational messages related to schedules
     */
    @GET("api-message.php")
    suspend fun getMessages(): Response<List<ScheduleMessagePart>>

    /**
     * Fetch headers only to get server time from 'Date' header
     */
    @retrofit2.http.HEAD("api-message.php")
    suspend fun getHeaders(): retrofit2.Response<Void>
}


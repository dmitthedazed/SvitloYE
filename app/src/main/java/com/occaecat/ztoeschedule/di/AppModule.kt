package com.occaecat.ztoeschedule.di

import android.content.Context
import com.occaecat.ztoeschedule.data.local.AddressStorage
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.GpvApiService
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): EnergyPreferencesManager {
        return EnergyPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideAddressStorage(@ApplicationContext context: Context): AddressStorage {
        return AddressStorage(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.ztoe.com.ua/gpv/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): GpvApiService {
        return retrofit.create(GpvApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRepository(
        apiService: GpvApiService,
        preferencesManager: EnergyPreferencesManager,
        addressStorage: AddressStorage
    ): EnergyRepository {
        return EnergyRepository(apiService, preferencesManager, addressStorage)
    }
}
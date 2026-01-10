package com.occaecat.ztoeschedule.domain.notification.di

import com.occaecat.ztoeschedule.domain.notification.model.NotificationConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing notification-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    /**
     * Provide NotificationConfig singleton with default values.
     *
     * Configuration can be overridden by changing default parameter values.
     */
    @Provides
    @Singleton
    fun provideNotificationConfig(): NotificationConfig {
        return NotificationConfig(
            enableCoordinator = true,
            enableDedupAlerts = true,
            enablePromotedStyle = true,
            alertDebounceMs = 60_000L,
            statusUpdateIntervalMs = 60_000L,
            workerCheckIntervalMinutes = 15L,
            networkTimeoutMs = 10_000L,
            enableDetailedLogging = false
        )
    }
}

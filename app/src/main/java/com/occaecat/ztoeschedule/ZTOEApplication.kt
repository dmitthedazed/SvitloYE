package com.occaecat.ztoeschedule

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.occaecat.ztoeschedule.domain.notification.NotificationHelper
import com.lyft.kronos.KronosClock

@HiltAndroidApp
class ZTOEApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var kronosClock: KronosClock

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAllChannels(this)
        kronosClock.syncInBackground()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
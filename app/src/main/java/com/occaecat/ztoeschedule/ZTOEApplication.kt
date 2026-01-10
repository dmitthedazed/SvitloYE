package com.occaecat.ztoeschedule

import android.app.Application
import android.app.Activity
import android.os.Bundle
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
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedCount = 0

            override fun onActivityStarted(activity: Activity) {
                startedCount += 1
                AppForegroundState.isForeground = startedCount > 0
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount = (startedCount - 1).coerceAtLeast(0)
                AppForegroundState.isForeground = startedCount > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

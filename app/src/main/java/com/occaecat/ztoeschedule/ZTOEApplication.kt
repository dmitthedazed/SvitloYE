package com.occaecat.ztoeschedule

import android.app.Application
import android.app.Activity
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.occaecat.ztoeschedule.domain.notification.NotificationChannels
import com.lyft.kronos.KronosClock
import com.occaecat.ztoeschedule.widget.WidgetPreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.occaecat.ztoeschedule.analytics.AnalyticsManager
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager

@HiltAndroidApp
class ZTOEApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var kronosClock: KronosClock
    @Inject lateinit var preferences: EnergyPreferencesManager
    @Inject lateinit var analytics: AnalyticsManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        kronosClock.syncInBackground()
        appScope.launch { WidgetPreviews.publishIfNeeded(this@ZTOEApplication) }
        // Keep Firebase in line with the user's choice (also after "delete all data" reset it)
        appScope.launch { preferences.analyticsEnabledFlow.collect(analytics::setCollectionEnabled) }
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

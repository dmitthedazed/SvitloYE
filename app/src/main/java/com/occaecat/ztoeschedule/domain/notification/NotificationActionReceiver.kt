package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            // Trigger immediate network check
            NotificationScheduler.runImmediateCheck(context)
            
            // Feedback for user
            Toast.makeText(context, "Оновлення даних...", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.occaecat.ztoeschedule.ACTION_REFRESH_STATUS"
    }
}

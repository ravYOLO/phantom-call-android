package com.phantomcall.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phantomcall.app.domain.GhostModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TURN_OFF) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { GhostModeController.get().disable() }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_TURN_OFF = "com.phantomcall.app.action.NOTIFICATION_TURN_OFF"

        fun turnOffPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java)
                .setAction(ACTION_TURN_OFF)
            return PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
package com.phantomcall.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.phantomcall.app.MainActivity
import com.phantomcall.app.R
import com.phantomcall.app.data.GhostStateRepository
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StatusNotificationManager private constructor() {

    private var scope: CoroutineScope? = null
    private var tickerJob: Job? = null
    private var isStarted = false
    private var channelEnsured = false
    private var appContext: Context? = null

    fun start() {
        if (isStarted) return
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        isStarted = true
        ensureChannel(context)
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        newScope.launch {
            GhostStateRepository.state.collect { state ->
                if (state.isActive) {
                    val startMs = state.sessionStartMs ?: System.currentTimeMillis()
                    notifyActive(context, startMs)
                    tickerJob?.cancel()
                    tickerJob = launch {
                        while (true) {
                            delay(1000)
                            notifyActive(context, startMs)
                        }
                    }
                } else {
                    tickerJob?.cancel()
                    tickerJob = null
                    NotificationManagerCompat.from(context).cancel(NOTIF_ID)
                }
            }
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        tickerJob?.cancel()
        tickerJob = null
        scope?.cancel()
        scope = null
        appContext?.let { NotificationManagerCompat.from(it).cancel(NOTIF_ID) }
    }

    private fun ensureChannel(context: Context) {
        if (channelEnsured) return
        channelEnsured = true
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_status),
            NotificationManager.IMPORTANCE_LOW
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun notifyActive(context: Context, startMs: Long) {
        val elapsed = formatElapsed(max(0L, System.currentTimeMillis() - startMs))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.status_on))
            .setContentText(context.getString(R.string.session_time) + " " + elapsed)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent(context))
            .addAction(
                R.drawable.ic_shield,
                context.getString(R.string.turn_off),
                NotificationActionReceiver.turnOffPendingIntent(context)
            )
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    private fun contentPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours < 1) {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    companion object {
        private val instance = StatusNotificationManager()

        fun get(): StatusNotificationManager = instance

        fun attach(context: Context) {
            instance.appContext = context.applicationContext
        }

        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "phantom_status"
    }
}
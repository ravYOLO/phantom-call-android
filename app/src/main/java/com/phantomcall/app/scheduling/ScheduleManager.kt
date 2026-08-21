package com.phantomcall.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.phantomcall.app.domain.GhostModeController
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ScheduleManager {

    internal const val PREFS_NAME = "phantom_prefs"
    internal const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    internal const val KEY_SCHEDULE_START = "schedule_start"
    internal const val KEY_SCHEDULE_END = "schedule_end"
    internal const val DEFAULT_MINUTE = 0

    private const val REQUEST_CODE = 77
    private const val MAX_MINUTE_OF_DAY = 1439

    private var prefs: SharedPreferences? = null

    val enabled: Boolean
        get() = prefs?.getBoolean(KEY_SCHEDULE_ENABLED, false) ?: false

    fun initialize(context: Context) {
        runCatching {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (enabled) scheduleNext(context)
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        runCatching {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()?.putBoolean(KEY_SCHEDULE_ENABLED, enabled)?.apply()
            if (enabled) scheduleNext(context) else cancelAlarm(context)
        }
    }

    fun setWindow(context: Context, startMin: Int, endMin: Int) {
        runCatching {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()
                ?.putInt(KEY_SCHEDULE_START, startMin.coerceIn(0, MAX_MINUTE_OF_DAY))
                ?.putInt(KEY_SCHEDULE_END, endMin.coerceIn(0, MAX_MINUTE_OF_DAY))
                ?.apply()
        }
    }

    internal fun scheduleNext(context: Context) {
        runCatching {
            val targetPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!targetPrefs.getBoolean(KEY_SCHEDULE_ENABLED, false)) {
                cancelAlarm(context)
                return
            }
            val startMin = targetPrefs.getInt(KEY_SCHEDULE_START, DEFAULT_MINUTE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextStartMillis(ZonedDateTime.now(), startMin),
                pendingIntent(context)
            )
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ScheduleReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextStartMillis(now: ZonedDateTime, startMin: Int): Long {
        val candidate = now
            .withHour(startMin / 60)
            .withMinute(startMin % 60)
            .withSecond(0)
            .withNano(0)
        val target = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
        return target.toInstant().toEpochMilli()
    }
}

class ScheduleReceiver : BroadcastReceiver() {

    private companion object {
        val modeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val now = ZonedDateTime.now()
            val nowMinute = now.hour * 60 + now.minute
            val targetPrefs = context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
            val startMin = targetPrefs.getInt(ScheduleManager.KEY_SCHEDULE_START, ScheduleManager.DEFAULT_MINUTE)
            val endMin = targetPrefs.getInt(ScheduleManager.KEY_SCHEDULE_END, ScheduleManager.DEFAULT_MINUTE)
            val controller = GhostModeController.get()
            if (isInsideWindow(nowMinute, startMin, endMin)) {
                modeScope.launch { controller.enable() }
            } else {
                modeScope.launch { controller.disable() }
            }
            ScheduleManager.scheduleNext(context)
        }
    }

    private fun isInsideWindow(nowMinute: Int, startMin: Int, endMin: Int): Boolean =
        if (startMin <= endMin) nowMinute in startMin..endMin
        else nowMinute >= startMin || nowMinute <= endMin
}
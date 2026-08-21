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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object TimerManager {

    internal const val PREFS_NAME = "phantom_prefs"
    internal const val KEY_TIMER_DEADLINE_MS = "timer_deadline_ms"

    private const val REQUEST_CODE = 88
    private const val MORNING_HOUR = 6

    private var prefs: SharedPreferences? = null

    private val _active = MutableStateFlow<Long?>(null)
    val active: StateFlow<Long?> = _active

    fun initialize(context: Context) {
        runCatching {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val deadline = prefs?.getLong(KEY_TIMER_DEADLINE_MS, 0L) ?: 0L
            when {
                deadline <= 0L -> _active.value = null
                deadline <= System.currentTimeMillis() -> clearDeadline(context)
                else -> _active.value = deadline
            }
        }
    }

    fun rearmAfterBoot(context: Context) {
        runCatching {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val deadline = prefs?.getLong(KEY_TIMER_DEADLINE_MS, 0L) ?: 0L
            when {
                deadline > System.currentTimeMillis() -> {
                    _active.value = deadline
                    scheduleAlarm(context, deadline)
                }
                else -> clearDeadline(context)
            }
        }
    }

    fun start(context: Context, minutes: Long) {
        runCatching {
            val deadline = System.currentTimeMillis() + minutes * 60_000
            persistDeadline(context, deadline)
            _active.value = deadline
            scheduleAlarm(context, deadline)
        }
    }

    fun startUntilMorning(context: Context) {
        runCatching {
            val now = ZonedDateTime.now()
            val candidate = now
                .withHour(MORNING_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            val target = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
            val deadline = target.toInstant().toEpochMilli()
            persistDeadline(context, deadline)
            _active.value = deadline
            scheduleAlarm(context, deadline)
        }
    }

    fun cancel(context: Context) {
        runCatching {
            clearDeadline(context)
            val pi = pendingIntent(context)
            pi.cancel()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pi)
        }
    }

    internal fun clearAfterFire(context: Context) {
        runCatching { clearDeadline(context) }
    }

    private fun persistDeadline(context: Context, deadline: Long) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putLong(KEY_TIMER_DEADLINE_MS, deadline)?.apply()
    }

    private fun clearDeadline(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.remove(KEY_TIMER_DEADLINE_MS)?.apply()
        _active.value = null
    }

    private fun scheduleAlarm(context: Context, deadline: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, TimerReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class TimerReceiver : BroadcastReceiver() {

    private companion object {
        val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            TimerManager.clearAfterFire(context)
            timerScope.launch { GhostModeController.get().disable() }
        }
    }
}
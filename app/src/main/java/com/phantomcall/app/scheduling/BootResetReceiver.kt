package com.phantomcall.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.service.StatusNotificationManager
import com.phantomcall.app.widget.PhantomWidgetProvider

class BootResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching {
            GhostStateRepository.resetAfterBoot()
            ScheduleManager.initialize(context)
            TimerManager.rearmAfterBoot(context)
            PhantomWidgetProvider.updateAll(context)
            StatusNotificationManager.get().stop()
        }
    }
}
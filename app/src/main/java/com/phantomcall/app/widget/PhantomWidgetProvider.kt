package com.phantomcall.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.phantomcall.app.R
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.domain.GhostModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhantomWidgetProvider : AppWidgetProvider() {

    @Volatile
    private var busy = false

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val active = GhostStateRepository.state.value.isActive
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_phantom)
            views.setImageViewResource(R.id.widget_icon, if (active) R.drawable.ic_widget_on else R.drawable.ic_widget_off)
            views.setContentDescription(R.id.widget_icon, context.getString(if (active) R.string.widget_on_desc else R.string.widget_off_desc))
            val toggleIntent = Intent(context, PhantomWidgetProvider::class.java).setAction(ACTION_TOGGLE)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getBroadcast(
                    context,
                    id,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        if (busy) return
        busy = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (GhostStateRepository.state.value.isActive) {
                    GhostModeController.get().disable()
                } else {
                    GhostModeController.get().enable()
                }
            } finally {
                busy = false
                PhantomWidgetProvider.updateAll(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.phantomcall.app.action.WIDGET_TOGGLE"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PhantomWidgetProvider::class.java))
            PhantomWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}
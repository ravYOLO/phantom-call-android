package com.phantomcall.app

import android.app.Application
import com.phantomcall.app.data.CustomPresetStore
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.service.StatusNotificationManager
import com.phantomcall.app.shell.AutoShellExecutor
import com.phantomcall.app.widget.PhantomWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhantomApp : Application() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        GhostStateRepository.initialize(this)
        AutoShellExecutor.initialize(this)
        CustomPresetStore.initialize(this)
        StatusNotificationManager.attach(this)
        StatusNotificationManager.get().start()
        var lastActive: Boolean? = null
        widgetScope.launch {
            GhostStateRepository.state.collect { s ->
                if (s.isActive != lastActive) {
                    lastActive = s.isActive
                    PhantomWidgetProvider.updateAll(this@PhantomApp)
                }
            }
        }
    }
}
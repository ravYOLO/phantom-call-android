package com.phantomcall.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.phantomcall.app.domain.GhostModeController
import com.phantomcall.app.scheduling.TimerManager
import com.phantomcall.app.service.StatusNotificationManager
import com.phantomcall.app.ui.MainScreen
import com.phantomcall.app.ui.theme.PhantomTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val shortcutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhantomTheme {
                MainScreen()
            }
        }
        handleShortcut(intent)
        requestNotificationsPermissionIfNeeded()
    }

    private fun handleShortcut(intent: Intent?) {
        if (intent?.action != ACTION_SHORTCUT) return
        when (intent.getStringExtra(EXTRA_ACTION)) {
            "enable" -> shortcutScope.launch { GhostModeController.get().enable() }
            "disable" -> shortcutScope.launch { GhostModeController.get().disable() }
            "timer60" -> TimerManager.start(this, 60L)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (prefs.getBoolean(KEY_PERM_REQUESTED, false)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean(KEY_PERM_REQUESTED, true).apply()
            return
        }
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            prefs.edit().putBoolean(KEY_PERM_REQUESTED, true).apply()
            if (granted) StatusNotificationManager.get().start()
        }.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val ACTION_SHORTCUT = "com.phantomcall.app.action.SHORTCUT"
        const val EXTRA_ACTION = "phantom_action"
        private const val PREFS_NAME = "phantom_prefs"
        private const val KEY_PERM_REQUESTED = "perm_requested"
    }
}
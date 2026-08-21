package com.phantomcall.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.phantomcall.app.service.StatusNotificationManager
import com.phantomcall.app.ui.MainScreen
import com.phantomcall.app.ui.theme.PhantomTheme

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhantomTheme {
                MainScreen()
            }
        }
        requestNotificationsPermissionIfNeeded()
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
        private const val PREFS_NAME = "phantom_prefs"
        private const val KEY_PERM_REQUESTED = "perm_requested"
    }
}
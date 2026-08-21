package com.phantomcall.app.shell

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object ShizukuManager {

    private const val PERMISSION_REQUEST_CODE = 1024
    private const val BIND_TIMEOUT_MS = 10_000L

    @Volatile
    private var inFlight = false

    @Volatile
    private var userService: IUserService? = null

    @Volatile
    private var boundComponent: ComponentName? = null

    @Volatile
    private var pendingConnection: CompletableDeferred<IUserService?>? = null

    private val permissionListener: Shizuku.OnRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                AutoShellExecutor.recheckBackend()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val deferred = pendingConnection
            if (deferred == null) {
                try {
                    Shizuku.unbindUserService(Shizuku.UserServiceArgs(name), this, true)
                } catch (t: Exception) {
                }
                return
            }
            val proxy = runCatching { IUserService.Stub.asInterface(service) }.getOrNull()
            deferred.complete(proxy)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            val deferred = pendingConnection
            if (deferred != null) {
                deferred.complete(null)
            } else {
                userService = null
                boundComponent = null
            }
        }
    }

    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Exception) {
        false
    }

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.getBinder() != null &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Exception) {
        false
    }

    fun requestPermission(activity: Activity): Boolean {
        if (!isBinderAlive()) return false
        return try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            true
        } catch (t: Exception) {
            false
        }
    }

    fun bindUserService(component: ComponentName, onConnected: () -> Unit): Boolean {
        if (inFlight) return false
        inFlight = true
        val deferred = CompletableDeferred<IUserService?>()
        pendingConnection = deferred
        val service = try {
            runBlocking(Dispatchers.IO) {
                withTimeout(BIND_TIMEOUT_MS) {
                    Shizuku.bindUserService(Shizuku.UserServiceArgs(component), serviceConnection)
                    deferred.await()
                }
            }
        } catch (t: Exception) {
            null
        } finally {
            pendingConnection = null
            inFlight = false
        }
        if (service == null) return false
        userService = service
        boundComponent = component
        onConnected()
        return true
    }

    fun unbindUserService() {
        val component = boundComponent ?: return
        try {
            userService?.destroy()
            Shizuku.unbindUserService(Shizuku.UserServiceArgs(component), serviceConnection, true)
        } catch (t: Exception) {
        } finally {
            userService = null
            boundComponent = null
        }
    }

    fun addBinderDeathListener(listener: () -> Unit) {
        try {
            Shizuku.addBinderDeadListener { listener() }
        } catch (t: Exception) {
        }
    }

    internal fun currentService(): IUserService? = userService
}
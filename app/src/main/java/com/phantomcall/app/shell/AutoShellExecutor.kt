package com.phantomcall.app.shell

import android.content.ComponentName
import android.content.Context
import android.util.Base64
import com.phantomcall.app.data.BackendType
import com.phantomcall.app.data.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AutoShellExecutor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val readinessFlow = MutableStateFlow<BackendType?>(null)

    val readiness: StateFlow<BackendType?> = readinessFlow

    @Volatile
    private var initialized = false

    @Volatile
    private var recheckReady = false

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var rootExecutor: RootShellExecutor? = null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        ShizukuManager.addBinderDeathListener { recheckReady = true }
        scope.launch { probeBackend(context) }
    }

    suspend fun exec(command: String): CommandResult {
        if (recheckReady) refreshBackend()
        val result = withContext(Dispatchers.IO) {
            when (val backend = readinessFlow.value) {
                BackendType.ROOT -> {
                    val executor = rootExecutor ?: RootShellExecutor().also { rootExecutor = it }
                    executor.exec(command)
                }
                BackendType.SHIZUKU -> execViaShizuku(command)
                null -> CommandResult(-1, "", "no_backend", false)
            }
        }
        LogRepository.add(command, result)
        return result
    }

    fun currentBackend(): BackendType? = readinessFlow.value

    fun recheckBackend() {
        val context = appContext ?: return
        scope.launch { withContext(Dispatchers.IO) { probeBackend(context) } }
    }

    private suspend fun refreshBackend() {
        recheckReady = false
        val context = appContext ?: return
        withContext(Dispatchers.IO) {
            if (readinessFlow.value == BackendType.SHIZUKU) ShizukuManager.unbindUserService()
            probeBackend(context)
        }
    }

    private fun probeBackend(context: Context) {
        val executor = rootExecutor ?: RootShellExecutor().also { rootExecutor = it }
        if (executor.isRootAvailable()) {
            readinessFlow.value = BackendType.ROOT
            return
        }
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.currentService() == null) {
                val bound = ShizukuManager.bindUserService(ComponentName(context, ShizukuUserService::class.java)) {}
                if (!bound) {
                    readinessFlow.value = null
                    return
                }
            }
            readinessFlow.value = BackendType.SHIZUKU
        } else {
            readinessFlow.value = null
        }
    }

    private fun execViaShizuku(command: String): CommandResult {
        val service = ShizukuManager.currentService()
        if (service == null) return CommandResult(-1, "", "no_backend", false)
        return runCatching {
            decodeServiceResult(service.exec(command))
        }.getOrElse {
            CommandResult(-1, "", "decode_error", false)
        }
    }

    private fun decodeServiceResult(encoded: String): CommandResult {
        val parts = encoded.split('|', limit = 3)
        if (parts.size < 3) return CommandResult(-1, "", "decode_error", false)
        val exitCode = parts[0].toInt()
        val out = String(Base64.decode(parts[1], Base64.NO_WRAP), Charsets.UTF_8)
        val err = String(Base64.decode(parts[2], Base64.NO_WRAP), Charsets.UTF_8)
        return CommandResult(exitCode, out, err, false)
    }
}
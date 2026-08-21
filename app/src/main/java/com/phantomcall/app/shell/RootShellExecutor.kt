package com.phantomcall.app.shell

import com.phantomcall.app.data.BackendType
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RootShellExecutor : ShellExecutor {

    @Volatile
    private var cachedAvailable = false

    @Volatile
    private var cachedAtMillis = 0L

    override suspend fun exec(command: String): CommandResult = execBlocking(command)

    override fun backendType(): BackendType? = BackendType.ROOT

    fun isRootAvailable(): Boolean = probeRoot()

    @Synchronized
    private fun probeRoot(): Boolean {
        val now = System.currentTimeMillis()
        if (cachedAvailable && now - cachedAtMillis < ROOT_PROBE_TTL_MILLIS) {
            return true
        }
        val probe = execBlocking("id")
        if (probe.exitCode == 0) {
            cachedAvailable = true
            cachedAtMillis = now
        } else {
            cachedAvailable = false
        }
        return probe.exitCode == 0
    }

    private fun execBlocking(command: String): CommandResult = runCatching {
        val process = ProcessBuilder("su", "-c", command).start()
        var stdoutBytes = ByteArray(0)
        var stderrBytes = ByteArray(0)
        val stdoutReader = thread { stdoutBytes = process.inputStream.readBytes() }
        val stderrReader = thread { stderrBytes = process.errorStream.readBytes() }
        val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val timedOut = !finished
        if (timedOut) {
            process.destroyForcibly()
            process.waitFor()
        }
        stdoutReader.join(2000L)
        stderrReader.join(2000L)
        CommandResult(process.exitValue(), String(stdoutBytes), String(stderrBytes), timedOut)
    }.getOrElse {
        CommandResult(-1, "", "root_exec_error", false)
    }

    companion object {
        private const val TIMEOUT_SECONDS = 20L
        private const val ROOT_PROBE_TTL_MILLIS = 5000L
    }
}
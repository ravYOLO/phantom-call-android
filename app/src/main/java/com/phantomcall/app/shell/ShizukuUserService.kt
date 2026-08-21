package com.phantomcall.app.shell

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Base64
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class ShizukuUserService : Service() {

    private val binder = object : IUserService.Stub() {
        private val activeProcesses = CopyOnWriteArraySet<Process>()

        override fun exec(cmd: String): String {
            val process = try {
                ProcessBuilder("sh", "-c", cmd).start()
            } catch (t: Throwable) {
                return ERROR_RESULT
            }
            activeProcesses.add(process)
            return try {
                val out = StringBuilder()
                val err = StringBuilder()
                val outThread = Thread { out.append(readFully(process.inputStream)) }
                val errThread = Thread { err.append(readFully(process.errorStream)) }
                outThread.start()
                errThread.start()
                val timedOut = !process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (timedOut) process.destroyForcibly()
                outThread.join()
                errThread.join()
                val exitCode = if (timedOut) TIMEOUT_EXIT_CODE else process.exitValue()
                "$exitCode|${encodeBase64(out.toString())}|${encodeBase64(err.toString())}"
            } catch (t: Throwable) {
                ERROR_RESULT
            } finally {
                activeProcesses.remove(process)
            }
        }

        override fun destroy() {
            activeProcesses.forEach { it.destroyForcibly() }
            activeProcesses.clear()
        }

        private fun readFully(stream: InputStream): String = stream.bufferedReader().use { it.readText() }

        private fun encodeBase64(value: String): String =
            Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TIMEOUT_SECONDS = 20L
        private const val TIMEOUT_EXIT_CODE = -1
        private const val ERROR_RESULT = "-1||"
    }
}
package com.phantomcall.app.data

import com.phantomcall.app.shell.CommandResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object LogRepository {

    private const val MAX_ENTRIES = 500
    private const val STDERR_BRIEF_LENGTH = 200

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    fun add(command: String, result: CommandResult) {
        _entries.update { current ->
            val entry = LogEntry(
                timestampMs = System.currentTimeMillis(),
                command = command,
                exitCode = result.exitCode,
                stderrBrief = result.stderr.take(STDERR_BRIEF_LENGTH).replace('\n', ' '),
                timedOut = result.timedOut
            )
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
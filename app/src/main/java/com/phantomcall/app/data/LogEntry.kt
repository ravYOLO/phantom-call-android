package com.phantomcall.app.data

data class LogEntry(val timestampMs: Long, val command: String, val exitCode: Int, val stderrBrief: String, val timedOut: Boolean)
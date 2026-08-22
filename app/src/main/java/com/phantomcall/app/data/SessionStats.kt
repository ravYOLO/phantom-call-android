package com.phantomcall.app.data

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SessionEntry(val startMs: Long, val endMs: Long)

object SessionStats {

    private const val FILE_NAME = "sessions.json"
    private const val TMP_FILE_NAME = "sessions.json.tmp"
    private const val MAX_SESSIONS = 500
    private const val DAY_MS = 86_400_000L
    private const val WEEK_MS = 7 * DAY_MS
    private const val MINUTE_MS = 60_000L

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    private var file: File? = null

    private val _sessions = MutableStateFlow<List<SessionEntry>>(emptyList())
    val sessions: StateFlow<List<SessionEntry>> = _sessions

    fun initialize(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        val source = file
        _sessions.value = runCatching {
            if (source != null && source.exists()) {
                jsonFormat.decodeFromString<List<SessionEntry>>(source.readText())
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    fun recordSession(startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        persist(_sessions.value + SessionEntry(startMs, endMs))
    }

    fun totalMinutesToday(): Long = totalMinutes { endMs -> isSameLocalDay(endMs, System.currentTimeMillis()) }

    fun totalMinutes7Days(): Long {
        val cutoff = System.currentTimeMillis() - WEEK_MS
        return totalMinutes { endMs -> endMs >= cutoff }
    }

    fun totalMinutesAll(): Long = totalMinutes { true }

    fun clear() {
        persist(emptyList())
    }

    fun replaceSessions(context: Context, list: List<SessionEntry>) {
        persist(list)
    }

    private fun totalMinutes(predicate: (Long) -> Boolean): Long =
        _sessions.value.filter { predicate(it.endMs) }.sumOf { it.endMs - it.startMs } / MINUTE_MS

    private fun isSameLocalDay(endMs: Long, nowMs: Long): Boolean {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(endMs).atZone(zone).toLocalDate() ==
            Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    }

    private fun persist(list: List<SessionEntry>) {
        val trimmed = list.takeLast(MAX_SESSIONS)
        _sessions.value = trimmed
        val target = file ?: return
        runCatching {
            val tmp = File(target.parentFile, TMP_FILE_NAME)
            tmp.writeText(jsonFormat.encodeToString(trimmed))
            tmp.renameTo(target)
        }
    }
}
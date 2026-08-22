package com.phantomcall.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupManager {

    private const val RESULT_OK = "backup_ok"
    private const val THEME_SYSTEM = "system"
    private const val THEME_DARK = "dark"
    private const val THEME_LIGHT = "light"

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    fun export(context: Context): String {
        val presets = runCatching {
            jsonFormat.decodeFromString<List<Preset>>(CustomPresetStore.exportAll())
        }.getOrDefault(emptyList())
        val settings = SettingsDto(
            presetId = GhostStateRepository.state.value.presetId,
            simMode = GhostStateRepository.state.value.simMode.name,
            themeName = GhostStateRepository.themeName.value
        )
        val backup = BackupData(
            presets = presets,
            settings = settings,
            sessions = SessionStats.sessions.value
        )
        return jsonFormat.encodeToString(backup)
    }

    fun import(context: Context, json: String): Result<String> = runCatching {
        applyBackup(context, json)
        RESULT_OK
    }

    private fun applyBackup(context: Context, json: String) {
        val data = jsonFormat.decodeFromString<BackupData>(json)
        data.presets.filter { BuiltInPresets.byId(it.id) == null }.forEach { CustomPresetStore.save(it) }
        data.settings?.let { settings ->
            settings.presetId?.let { id ->
                if (BuiltInPresets.byId(id) != null) GhostStateRepository.setPresetId(id)
            }
            settings.simMode?.let { mode ->
                runCatching { SimSlotMode.valueOf(mode) }.getOrNull()?.let { GhostStateRepository.setSimMode(it) }
            }
            settings.themeName?.let { theme ->
                if (theme in setOf(THEME_SYSTEM, THEME_DARK, THEME_LIGHT)) GhostStateRepository.setTheme(theme)
            }
        }
        SessionStats.replaceSessions(context, data.sessions)
    }

    @Serializable
    private data class BackupData(
        val presets: List<Preset> = emptyList(),
        val settings: SettingsDto? = null,
        val sessions: List<SessionEntry> = emptyList()
    )

    @Serializable
    private data class SettingsDto(
        val presetId: String? = null,
        val simMode: String? = null,
        val themeName: String? = null
    )
}
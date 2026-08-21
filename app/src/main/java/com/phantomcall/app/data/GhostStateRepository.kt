package com.phantomcall.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object GhostStateRepository {

    private const val PREFS_NAME = "phantom_prefs"
    private const val KEY_PRESET_ID = "preset_id"
    private const val KEY_SIM_MODE = "sim_mode"
    private const val KEY_THEME_NAME = "theme_name"

    private val _state = MutableStateFlow(GhostState())
    val state: StateFlow<GhostState> = _state

    private val _themeName = MutableStateFlow("system")
    val themeName: StateFlow<String> = _themeName

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = prefs ?: return
        val rawPresetId = loaded.getString(KEY_PRESET_ID, null)
        val presetId = if (rawPresetId.isNullOrBlank() || rawPresetId == "builtin_universal") "universal" else rawPresetId
        val simMode = runCatching {
            SimSlotMode.valueOf(loaded.getString(KEY_SIM_MODE, null) ?: SimSlotMode.BOTH.name)
        }.getOrDefault(SimSlotMode.BOTH)
        _themeName.value = loaded.getString(KEY_THEME_NAME, "system") ?: "system"
        _state.value = GhostState(presetId = presetId, simMode = simMode)
    }

    fun update(transform: (GhostState) -> GhostState) {
        _state.update(transform)
    }

    fun resetAfterBoot() {
        _state.update { it.copy(isActive = false, savedMasks = emptyMap(), sessionStartMs = null) }
    }

    fun setTheme(name: String) {
        _themeName.value = name
        prefs?.edit()?.putString(KEY_THEME_NAME, name)?.apply()
    }

    fun setPresetId(id: String) {
        _state.update { it.copy(presetId = id) }
        prefs?.edit()?.putString(KEY_PRESET_ID, id)?.apply()
    }

    fun setSimMode(mode: SimSlotMode) {
        _state.update { it.copy(simMode = mode) }
        prefs?.edit()?.putString(KEY_SIM_MODE, mode.name)?.apply()
    }
}
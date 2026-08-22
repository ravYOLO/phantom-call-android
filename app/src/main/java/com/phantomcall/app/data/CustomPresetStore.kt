package com.phantomcall.app.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CustomPresetStore {

    private const val FILE_NAME = "custom_presets.json"
    private const val TMP_FILE_NAME = "custom_presets.json.tmp"
    private const val CUSTOM_ID_PREFIX = "custom_"

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    private var file: File? = null

    private val _state = MutableStateFlow<List<Preset>>(emptyList())
    val presets: Flow<List<Preset>> = _state

    fun initialize(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        val source = file
        _state.value = runCatching {
            if (source != null && source.exists()) {
                jsonFormat.decodeFromString<List<Preset>>(source.readText())
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    fun save(preset: Preset): Boolean {
        val id = preset.id.ifBlank { CUSTOM_ID_PREFIX + System.currentTimeMillis() }
        val entry = if (id == preset.id) preset else preset.copy(id = id)
        return persist(_state.value.filterNot { it.id == id } + entry)
    }

    fun delete(id: String): Boolean {
        if (_state.value.none { it.id == id }) return false
        return persist(_state.value.filterNot { it.id == id })
    }

    fun rename(id: String, newName: String): Boolean {
        if (_state.value.none { it.id == id }) return false
        return persist(_state.value.map { if (it.id == id) it.copy(nameOverride = newName) else it })
    }

    fun exportAll(): String = jsonFormat.encodeToString(_state.value)

    fun import(json: String): Result<Int> = runCatching {
        val imported = jsonFormat.decodeFromString<List<Preset>>(json)
        val kept = imported.filter { it.id.isNotBlank() && BuiltInPresets.byId(it.id) == null }
        val keptIds = kept.map { it.id }.toSet()
        val merged = _state.value.filterNot { it.id in keptIds } + kept
        check(persist(merged))
        kept.size
    }

    fun byIdOrNull(id: String): Preset? =
        _state.value.firstOrNull { it.id == id } ?: BuiltInPresets.byId(id)

    private fun persist(list: List<Preset>): Boolean {
        _state.value = list
        val target = file ?: return false
        return runCatching {
            val tmp = File(target.parentFile, TMP_FILE_NAME)
            tmp.writeText(jsonFormat.encodeToString(list))
            tmp.renameTo(target)
        }.getOrDefault(false)
    }
}
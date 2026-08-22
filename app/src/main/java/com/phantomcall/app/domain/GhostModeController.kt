package com.phantomcall.app.domain

import com.phantomcall.app.data.BuiltInPresets
import com.phantomcall.app.data.CustomPresetStore
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.data.Preset
import com.phantomcall.app.data.SessionStats
import com.phantomcall.app.shell.AutoShellExecutor
import com.phantomcall.app.shell.CommandResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class GhostModeController private constructor() {

    private val toggleLock = Mutex()

    companion object {
        private val instance = GhostModeController()

        fun get(): GhostModeController = instance
    }

    suspend fun enable(): ToggleResult = toggleLock.withLock {
        if (GhostStateRepository.state.value.isActive) {
            return@withLock ToggleResult.Success("already_enabled")
        }
        if (AutoShellExecutor.currentBackend() == null) {
            AutoShellExecutor.recheckBackend()
            val ready = withTimeoutOrNull(3000) {
                while (AutoShellExecutor.currentBackend() == null) {
                    delay(200)
                }
                true
            }
            if (ready != true) {
                return@withLock ToggleResult.Failure("error_no_backend")
            }
        }
        val snapshot = GhostStateRepository.state.value
        val preset = resolvePreset(snapshot.presetId)
        val slots = snapshot.simMode.slots
        val masks = captureMasks(slots)
        GhostStateRepository.update { it.copy(savedMasks = masks) }
        val commands = CommandBuilder.enableCommands(preset, slots)
        val results = commands.map { AutoShellExecutor.exec(it) }
        if (results.any { !it.success }) {
            rollbackExecuted(commands, results)
            return@withLock ToggleResult.Failure("enable_failed")
        }
        GhostStateRepository.update { it.copy(isActive = true, backend = AutoShellExecutor.currentBackend(), sessionStartMs = System.currentTimeMillis()) }
        ToggleResult.Success("enabled")
    }

    suspend fun disable(): ToggleResult = toggleLock.withLock {
        val snapshot = GhostStateRepository.state.value
        if (!snapshot.isActive) {
            return@withLock ToggleResult.Success("already_disabled")
        }
        val preset = resolvePreset(snapshot.presetId)
        val slots = snapshot.simMode.slots
        val commands = CommandBuilder.restoreCommands(preset, snapshot.savedMasks, slots)
        val results = commands.map { AutoShellExecutor.exec(it) }
        if (results.all { it.success }) {
            val sessionStart = snapshot.sessionStartMs
            GhostStateRepository.update { it.copy(isActive = false, sessionStartMs = null) }
            if (sessionStart != null) {
                SessionStats.recordSession(sessionStart, System.currentTimeMillis())
            }
            ToggleResult.Success("disabled")
        } else {
            ToggleResult.Failure("error_restore_failed")
        }
    }

    fun diagnosticsCommands(preset: Preset, slots: List<Int>): List<String> =
        slots.flatMap { slot -> CommandBuilder.captureCommands(slot) + CommandBuilder.verifyImsCommands(slot) }

    private fun resolvePreset(presetId: String): Preset =
        CustomPresetStore.byIdOrNull(presetId) ?: BuiltInPresets.byId(presetId) ?: BuiltInPresets.all.first()

    private suspend fun captureMasks(slots: List<Int>): Map<Int, String> {
        val masks = mutableMapOf<Int, String>()
        slots.forEach { slot ->
            val stdout = AutoShellExecutor.exec(CommandBuilder.captureCommands(slot).first()).stdout
            masks.putAll(MaskParser.parseMasks(stdout))
        }
        return masks
    }

    private suspend fun rollbackExecuted(commands: List<String>, results: List<CommandResult>) {
        val executed = commands.zip(results).filter { it.second.success }.map { it.first }
        executed.asReversed().forEach { AutoShellExecutor.exec(it) }
    }
}
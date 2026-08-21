package com.phantomcall.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phantomcall.app.R
import com.phantomcall.app.data.BuiltInPresets
import com.phantomcall.app.data.CustomPresetStore
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.domain.CommandBuilder
import com.phantomcall.app.domain.GhostModeController
import com.phantomcall.app.shell.AutoShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsPanel(onDismiss: () -> Unit) {
    val state by GhostStateRepository.state.collectAsStateWithLifecycle()
    val preset = CustomPresetStore.byIdOrNull(state.presetId) ?: BuiltInPresets.byId(state.presetId) ?: BuiltInPresets.all.first()
    val slots = state.simMode.slots
    var running by remember { mutableStateOf(false) }
    var imsOn by remember { mutableStateOf(false) }
    var lteOnly by remember { mutableStateOf(false) }
    var dataOk by remember { mutableStateOf(false) }
    var ran by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diagnostics_title)) },
        text = {
            Column {
                when {
                    running -> CircularProgressIndicator()
                    ran -> {
                        Card {
                            Text(if (imsOn) stringResource(R.string.diag_ims_on) else stringResource(R.string.diag_ims_off))
                        }
                        Card {
                            Text(if (lteOnly) stringResource(R.string.diag_lte_only) else stringResource(R.string.diag_calls_blocked))
                        }
                        if (dataOk) {
                            Card {
                                Text(stringResource(R.string.diag_data_ok))
                            }
                        }
                    }
                    else -> {
                        TextButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                running = true
                                val cmds = GhostModeController.get().diagnosticsCommands(preset, slots) + "cmd connectivity airplane-mode"
                                val results = cmds.map { it to AutoShellExecutor.exec(it) }
                                imsOn = results.any { it.first.contains("ims") && it.second.success }
                                lteOnly = results.any { it.second.stdout.contains(CommandBuilder.LTE_ONLY_MASK) }
                                dataOk = results.any { it.first == "cmd connectivity airplane-mode" && it.second.success && it.second.stdout.contains("disabled") }
                                running = false
                                ran = true
                            }
                        }) {
                            Text(stringResource(R.string.run_test))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
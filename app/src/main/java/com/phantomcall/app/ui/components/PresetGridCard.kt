package com.phantomcall.app.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phantomcall.app.R
import com.phantomcall.app.data.BuiltInPresets
import com.phantomcall.app.data.CustomPresetStore
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.data.Preset

@Composable
fun PresetGridCard() {
    val state by GhostStateRepository.state.collectAsStateWithLifecycle()
    val customs by CustomPresetStore.presets.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf<Preset?>(null) }
    var showNew by remember { mutableStateOf(false) }
    val cells = remember(customs) { BuiltInPresets.all + customs }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cells.size + 1) { index ->
                    if (index < cells.size) {
                        val preset = cells[index]
                        PresetCell(
                            preset = preset,
                            selected = preset.id == state.presetId,
                            onSelect = { GhostStateRepository.setPresetId(preset.id) },
                            onLongClick = if (preset.id.startsWith("custom_")) {
                                { editing = preset }
                            } else {
                                null
                            }
                        )
                    } else {
                        AddPresetCell(onClick = { showNew = true })
                    }
                }
            }
        }
    }

    if (editing != null || showNew) {
        PresetEditorDialog(
            preset = editing,
            onDismiss = {
                editing = null
                showNew = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCell(
    preset: Preset,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onSelect, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelFor(context, preset),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddPresetCell(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun PresetEditorDialog(preset: Preset?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(preset?.nameOverride.orEmpty()) }
    var enableText by remember { mutableStateOf(preset?.enableCommands?.joinToString("\n") ?: "") }
    var disableText by remember { mutableStateOf(preset?.disableCommands?.joinToString("\n") ?: "") }
    var importJson by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preset?.nameOverride ?: "New preset") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CommandField("Enable commands", enableText, { enableText = it })
                CommandField("Disable commands", disableText, { disableText = it })
                ImportSection(context, importJson, { importJson = it })
            }
        },
        dismissButton = {
            if (preset != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        CustomPresetStore.delete(preset.id)
                        onDismiss()
                    }) {
                        Text("Delete")
                    }
                    TextButton(onClick = { shareExport(context) }) {
                        Text("Export")
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    CustomPresetStore.save(buildPreset(preset, name, enableText, disableText))
                    onDismiss()
                }) {
                    Text("Save")
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    )
}

@Composable
private fun CommandField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    )
}

@Composable
private fun ImportSection(context: Context, importJson: String, onImportJsonChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = importJson,
            onValueChange = onImportJsonChange,
            label = { Text("Import JSON") },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
        TextButton(onClick = {
            val result = CustomPresetStore.import(importJson)
            Toast.makeText(
                context,
                if (result.isSuccess) "Imported ${result.getOrNull()}" else "Import failed",
                Toast.LENGTH_SHORT
            ).show()
        }) {
            Text("Import")
        }
    }
}

private fun buildPreset(preset: Preset?, name: String, enableText: String, disableText: String): Preset {
    val lines = enableText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val disableLines = disableText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    return Preset(
        id = preset?.id ?: "custom_" + System.currentTimeMillis(),
        nameOverride = name.ifBlank { null },
        enableCommands = lines,
        disableCommands = disableLines
    )
}

private fun shareExport(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, CustomPresetStore.exportAll())
    }
    runCatching { context.startActivity(intent) }
}

private fun labelFor(context: Context, p: Preset): String {
    val resId = when (p.nameKey) {
        "preset_universal" -> R.string.preset_universal
        "preset_pixel" -> R.string.preset_pixel
        "preset_xiaomi" -> R.string.preset_xiaomi
        "preset_samsung" -> R.string.preset_samsung
        "preset_oneplus" -> R.string.preset_oneplus
        "preset_vivo" -> R.string.preset_vivo
        "preset_legacy" -> R.string.preset_legacy
        else -> null
    }
    return resId?.let { context.getString(it) } ?: (p.nameOverride ?: p.id)
}
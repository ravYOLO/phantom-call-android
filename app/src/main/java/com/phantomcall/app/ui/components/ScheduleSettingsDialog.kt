package com.phantomcall.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.phantomcall.app.R
import com.phantomcall.app.scheduling.ScheduleManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val DEFAULT_START_MIN = 1380
private const val DEFAULT_END_MIN = 480

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var enabled by remember {
        mutableStateOf(prefs.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false))
    }
    var startMin by remember {
        mutableStateOf(prefs.getInt(ScheduleManager.KEY_SCHEDULE_START, DEFAULT_START_MIN))
    }
    var endMin by remember {
        mutableStateOf(prefs.getInt(ScheduleManager.KEY_SCHEDULE_END, DEFAULT_END_MIN))
    }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_set)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(stringResource(R.string.schedule_enable))
                }
                if (enabled) {
                    Text(stringResource(R.string.schedule_start) + " " + formatTime(startMin))
                    TextButton(onClick = { showStartPicker = true }) {
                        Text(stringResource(R.string.pick))
                    }
                    Text(stringResource(R.string.schedule_end) + " " + formatTime(endMin))
                    TextButton(onClick = { showEndPicker = true }) {
                        Text(stringResource(R.string.pick))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ScheduleManager.setEnabled(context, enabled)
                ScheduleManager.setWindow(context, startMin, endMin)
                onDismiss()
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
    if (showStartPicker) {
        MinutePickerDialog(
            initialMinutes = startMin,
            onConfirm = { minutes ->
                startMin = minutes
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinutePickerDialog(
            initialMinutes = endMin,
            onConfirm = { minutes ->
                endMin = minutes
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinutePickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute); onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = { TimePicker(state = state) }
    )
}

private fun formatTime(minutesOfDay: Int): String =
    LocalTime.of(minutesOfDay / 60, minutesOfDay % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
package com.phantomcall.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phantomcall.app.R
import com.phantomcall.app.data.LogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogDialog(onDismiss: () -> Unit) {
    val entries by LogRepository.entries.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.command_log)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(entries) { e ->
                    Text(
                        text = buildString {
                            append(formatTime(e.timestampMs))
                            append(" | ")
                            append(e.command)
                            append(" | exit=")
                            append(e.exitCode)
                            if (e.timedOut) append(" TIMEOUT")
                            if (e.stderrBrief.isNotEmpty()) {
                                append("\n")
                                append(e.stderrBrief)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { LogRepository.clear() }) {
                    Text(stringResource(R.string.clear_log))
                }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(entries.joinToString("\n") { e -> e.command }))
                }) {
                    Text(stringResource(R.string.copy_log))
                }
            }
        }
    )
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ms))
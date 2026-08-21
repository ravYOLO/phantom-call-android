package com.phantomcall.app.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phantomcall.app.R
import com.phantomcall.app.data.BackendType
import com.phantomcall.app.shell.AutoShellExecutor
import com.phantomcall.app.shell.ShizukuManager

@Composable
fun BackendStatusCard() {
    val backend by AutoShellExecutor.readiness.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dotColor = when (backend) {
        BackendType.ROOT -> Color(0xFF4CAF50)
        BackendType.SHIZUKU -> Color(0xFF2196F3)
        null -> Color(0xFF9E9E9E)
    }
    val label = when (backend) {
        BackendType.ROOT -> stringResource(R.string.backend_root)
        BackendType.SHIZUKU -> stringResource(R.string.backend_shizuku)
        null -> stringResource(R.string.backend_none)
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (backend == null) {
                TextButton(onClick = {
                    runCatching { ShizukuManager.requestPermission(context as Activity) }
                }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}
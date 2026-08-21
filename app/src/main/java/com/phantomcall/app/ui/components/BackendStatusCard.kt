package com.phantomcall.app.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phantomcall.app.R
import com.phantomcall.app.data.BackendType
import com.phantomcall.app.shell.AutoShellExecutor
import com.phantomcall.app.shell.ShizukuManager

private data class BackendBadgeStyle(
    val badgeColor: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val labelRes: Int,
    val descRes: Int
)

@Composable
fun BackendStatusCard() {
    val backend by AutoShellExecutor.readiness.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val style = when (backend) {
        BackendType.ROOT -> BackendBadgeStyle(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF4CAF50),
            Icons.Default.Terminal,
            R.string.backend_root,
            R.string.backend_root_desc
        )
        BackendType.SHIZUKU -> BackendBadgeStyle(
            Color(0xFF2196F3).copy(alpha = 0.15f),
            Color(0xFF2196F3),
            Icons.Default.Android,
            R.string.backend_shizuku,
            R.string.backend_shizuku_desc
        )
        null -> BackendBadgeStyle(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning,
            R.string.backend_none,
            R.string.backend_none_desc
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(style.badgeColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(style.labelRes),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(style.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
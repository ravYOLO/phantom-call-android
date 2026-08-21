package com.phantomcall.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phantomcall.app.R
import com.phantomcall.app.data.GhostState
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.data.SimSlotMode
import com.phantomcall.app.data.SessionStats
import com.phantomcall.app.data.UpdateChecker
import com.phantomcall.app.data.UpdateInfo
import com.phantomcall.app.scheduling.TimerManager
import com.phantomcall.app.ui.components.BackendStatusCard
import com.phantomcall.app.ui.components.BatteryOptimizationCard
import com.phantomcall.app.ui.components.DiagnosticsPanel
import com.phantomcall.app.ui.components.LogDialog
import com.phantomcall.app.ui.components.PresetGridCard
import com.phantomcall.app.ui.components.ScheduleSettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiag by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateChecked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val checkUpdates: () -> Unit = {
        showUpdate = true
        scope.launch {
            updateChecking = true
            updateChecked = false
            updateInfo = UpdateChecker.checkForUpdates(context)
            updateChecking = false
            updateChecked = true
        }
    }

    CollectSnackbarEvents(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                onOpenDiag = { showDiag = true },
                onOpenLog = { showLog = true },
                onOpenAbout = { showAbout = true },
                onOpenSchedule = { showSchedule = true },
                onOpenTheme = { showThemeMenu = true },
                onOpenStats = { showStats = true },
                onOpenUpdate = checkUpdates
            )
        }
    ) { innerPadding ->
        MainContent(state, viewModel::toggle, innerPadding)
    }

    if (showDiag) DiagnosticsPanel(onDismiss = { showDiag = false })
    if (showLog) LogDialog(onDismiss = { showLog = false })
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showSchedule) ScheduleSettingsDialog(onDismiss = { showSchedule = false })
    if (showThemeMenu) ThemeDialog(onDismiss = { showThemeMenu = false })
    if (showStats) StatisticsDialog(onDismiss = { showStats = false })
    if (showUpdate) {
        UpdateDialog(
            updateInfo = updateInfo,
            updateChecking = updateChecking,
            updateChecked = updateChecked,
            onDismiss = { showUpdate = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onOpenDiag: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.diagnostics_title)) },
                    onClick = {
                        menuExpanded = false
                        onOpenDiag()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.command_log)) },
                    onClick = {
                        menuExpanded = false
                        onOpenLog()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
                    onClick = {
                        menuExpanded = false
                        onOpenAbout()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.schedule_set)) },
                    onClick = {
                        menuExpanded = false
                        onOpenSchedule()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_theme)) },
                    onClick = {
                        menuExpanded = false
                        onOpenTheme()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stats_title)) },
                    onClick = {
                        menuExpanded = false
                        onOpenStats()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.update_check)) },
                    onClick = {
                        menuExpanded = false
                        onOpenUpdate()
                    }
                )
            }
        }
    )
}

@Composable
private fun MainContent(state: GhostState, onToggle: () -> Unit, innerPadding: PaddingValues) {
    val elapsedSeconds = rememberSessionElapsed(state)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackendStatusCard() }
        item { BatteryOptimizationCard() }
        item { MainSwitchCard(state, onToggle, elapsedSeconds) }
        item { SimSelectorRow(state.simMode) }
        item { PresetGridCard() }
    }
}

@Composable
private fun rememberSessionElapsed(state: GhostState): Long {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    val sessionStart = state.sessionStartMs
    LaunchedEffect(state.isActive, sessionStart) {
        while (state.isActive && sessionStart != null) {
            elapsedSeconds = (System.currentTimeMillis() - sessionStart) / 1000
            delay(1000)
        }
    }
    return elapsedSeconds
}

@Composable
private fun MainSwitchCard(state: GhostState, onToggle: () -> Unit, elapsedSeconds: Long) {
    val context = LocalContext.current
    val timerDeadline by TimerManager.active.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timerDeadline) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (state.isActive) R.string.status_on else R.string.status_off),
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.isActive && state.sessionStartMs != null) {
                    Text(
                        text = stringResource(R.string.session_time) + ": " + formatElapsed(elapsedSeconds),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Switch(checked = state.isActive, onCheckedChange = { onToggle() })
        }
        val deadline = timerDeadline
        if (deadline != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.timer_active, formatRemaining(deadline, now)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { TimerManager.cancel(context) }) {
                    Text(stringResource(R.string.timer_cancel))
                }
            }
        } else if (state.isActive) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { TimerManager.start(context, 30) },
                    label = { Text(stringResource(R.string.timer_30m)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { TimerManager.start(context, 60) },
                    label = { Text(stringResource(R.string.timer_1h)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { TimerManager.start(context, 120) },
                    label = { Text(stringResource(R.string.timer_2h)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { TimerManager.startUntilMorning(context) },
                    label = { Text(stringResource(R.string.timer_until_morning)) }
                )
            }
        }
    }
}

@Composable
private fun SimSelectorRow(simMode: SimSlotMode) {
    Card(modifier = Modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val options = listOf(R.string.sim_both, R.string.sim_sim1, R.string.sim_sim2)
            options.forEachIndexed { index, resId ->
                SegmentedButton(
                    selected = simMode.ordinal == index,
                    onClick = { GhostStateRepository.setSimMode(SimSlotMode.entries[index]) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(stringResource(resId))
                }
            }
        }
    }
}

@Composable
private fun CollectSnackbarEvents(viewModel: MainViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
        }
    }
}

@Composable
private fun ThemeDialog(onDismiss: () -> Unit) {
    val themeName by GhostStateRepository.themeName.collectAsStateWithLifecycle()
    val themes = listOf(
        R.string.theme_system to "system",
        R.string.theme_dark to "dark",
        R.string.theme_light to "light"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_theme)) },
        text = {
            Column {
                themes.forEach { (labelRes, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeName == name,
                                onClick = {
                                    GhostStateRepository.setTheme(name)
                                    onDismiss()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeName == name, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(labelRes))
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

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column {
                Text("Phantom Call 1.0.0")
                Text("MIT License")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun StatisticsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stats_title)) },
        text = {
            Column {
                Text(stringResource(R.string.stats_today) + ": " + SessionStats.totalMinutesToday())
                Text(stringResource(R.string.stats_7d) + ": " + SessionStats.totalMinutes7Days())
                Text(stringResource(R.string.stats_all) + ": " + SessionStats.totalMinutesAll())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { SessionStats.clear() }) {
                Text(stringResource(R.string.stats_clear))
            }
        }
    )
}

@Composable
private fun UpdateDialog(
    updateInfo: UpdateInfo?,
    updateChecking: Boolean,
    updateChecked: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_check)) },
        text = {
            when {
                updateChecking -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.update_checking))
                }
                updateChecked && updateInfo == null -> Text(stringResource(R.string.update_latest))
                updateInfo != null -> Column {
                    Text(
                        text = "v" + updateInfo.version + " " + updateInfo.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateInfo.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 8
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
            val info = updateInfo
            if (info != null && !updateChecking) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)))
                }) {
                    Text(stringResource(R.string.update_download))
                }
            }
        }
    )
}

private fun formatRemaining(deadlineMillis: Long, nowMillis: Long): String {
    val remainingSeconds = ((deadlineMillis - nowMillis) / 1000).coerceAtLeast(0)
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
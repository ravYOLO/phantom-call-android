package com.phantomcall.app.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.phantomcall.app.R
import com.phantomcall.app.data.GhostState
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.domain.GhostModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhantomTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var busy = false

    private var observeJob: Job? = null

    override fun onStartListening() {
        render(GhostStateRepository.state.value)
        observeJob = scope.launch {
            GhostStateRepository.state.collect { render(it) }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
    }

    override fun onClick() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                if (GhostStateRepository.state.value.isActive) {
                    GhostModeController.get().disable()
                } else {
                    GhostModeController.get().enable()
                }
            } finally {
                busy = false
            }
        }
    }

    private fun render(state: GhostState) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield)
        tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val description = getString(if (state.isActive) R.string.tile_subtitle_on else R.string.tile_subtitle_off)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tile.subtitle = description
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.contentDescription = description
        }
        tile.updateTile()
    }
}
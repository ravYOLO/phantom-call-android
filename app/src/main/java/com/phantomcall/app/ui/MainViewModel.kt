package com.phantomcall.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phantomcall.app.R
import com.phantomcall.app.data.GhostState
import com.phantomcall.app.data.GhostStateRepository
import com.phantomcall.app.domain.GhostModeController
import com.phantomcall.app.domain.ToggleResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val state: StateFlow<GhostState> = GhostStateRepository.state

    private val _snackbarEvents = MutableSharedFlow<Int>()
    val snackbarEvents: SharedFlow<Int> = _snackbarEvents.asSharedFlow()

    fun toggle() {
        viewModelScope.launch {
            val result = if (state.value.isActive) {
                GhostModeController.get().disable()
            } else {
                GhostModeController.get().enable()
            }
            if (result is ToggleResult.Failure) {
                _snackbarEvents.emit(mapFailureReason(result.reason))
            }
        }
    }

    private fun mapFailureReason(reason: String): Int = when (reason) {
        "error_no_backend" -> R.string.error_no_backend
        "error_restore_failed" -> R.string.error_restore_failed
        else -> R.string.enable_failed
    }
}
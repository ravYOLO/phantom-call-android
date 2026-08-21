package com.phantomcall.app.domain

sealed interface ToggleResult {
    data class Success(val summary: String) : ToggleResult
    data class Failure(val reason: String) : ToggleResult
}
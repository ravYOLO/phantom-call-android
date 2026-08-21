package com.phantomcall.app.shell

import com.phantomcall.app.data.BackendType

interface ShellExecutor {
    suspend fun exec(command: String): CommandResult
    fun backendType(): BackendType?
}
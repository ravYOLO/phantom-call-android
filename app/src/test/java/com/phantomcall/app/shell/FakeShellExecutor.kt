package com.phantomcall.app.shell

import com.phantomcall.app.data.BackendType

class FakeShellExecutor : ShellExecutor {
    val recorded = mutableListOf<String>()
    private val queue = ArrayDeque<CommandResult>()

    fun enqueue(vararg results: CommandResult) {
        results.forEach { queue.addLast(it) }
    }

    override suspend fun exec(command: String): CommandResult {
        recorded += command
        return queue.removeFirstOrNull() ?: CommandResult(0, "", "", false)
    }

    override fun backendType(): BackendType? = BackendType.ROOT
}
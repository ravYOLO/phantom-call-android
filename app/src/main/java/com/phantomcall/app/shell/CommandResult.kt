package com.phantomcall.app.shell

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String, val timedOut: Boolean) {
    val success: Boolean get() = exitCode == 0 && !timedOut
    override fun toString(): String = "CommandResult(exit=$exitCode, out=${stdout.length}B, err=${stderr.length}B, timedOut=$timedOut)"
}
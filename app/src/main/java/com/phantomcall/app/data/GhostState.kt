package com.phantomcall.app.data

data class GhostState(
    val isActive: Boolean = false,
    val backend: BackendType? = null,
    val presetId: String = "",
    val simMode: SimSlotMode = SimSlotMode.BOTH,
    val sessionStartMs: Long? = null,
    val savedMasks: Map<Int, String> = emptyMap()
)
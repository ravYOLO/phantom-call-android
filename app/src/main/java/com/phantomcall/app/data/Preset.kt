package com.phantomcall.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Preset(
    val id: String,
    val nameKey: String? = null,
    val nameOverride: String? = null,
    val enableCommands: List<String> = emptyList(),
    val disableCommands: List<String> = emptyList(),
    val requiresImsCmd: Boolean = false,
    val legacy: Boolean = false
)
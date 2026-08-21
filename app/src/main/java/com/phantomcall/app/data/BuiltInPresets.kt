package com.phantomcall.app.data

object BuiltInPresets {

    val all: List<Preset> = listOf(
        Preset(
            id = "universal",
            nameKey = "preset_universal",
            requiresImsCmd = true
        ),
        Preset(
            id = "pixel",
            nameKey = "preset_pixel",
            requiresImsCmd = true
        ),
        Preset(
            id = "xiaomi",
            nameKey = "preset_xiaomi",
            requiresImsCmd = true
        ),
        Preset(
            id = "samsung",
            nameKey = "preset_samsung",
            requiresImsCmd = false
        ),
        Preset(
            id = "oneplus",
            nameKey = "preset_oneplus",
            requiresImsCmd = true
        ),
        Preset(
            id = "vivo",
            nameKey = "preset_vivo",
            requiresImsCmd = true
        ),
        Preset(
            id = "legacy",
            nameKey = "preset_legacy",
            requiresImsCmd = false,
            legacy = true
        )
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }
}
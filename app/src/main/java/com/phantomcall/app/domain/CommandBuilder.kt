package com.phantomcall.app.domain

import com.phantomcall.app.data.Preset

object CommandBuilder {

    const val LTE_ONLY_MASK = "01000001000000000000"
    const val IMS_PKG_SAMSUNG = "com.sec.imsservice"
    const val IMS_PKG_QC = "org.codeaurora.ims"
    const val IMS_PKG_MTK = "com.mediatek.ims"
    const val IMS_PKG_GOOGLE = "com.google.android.ims"

    fun captureCommands(slot: Int): List<String> =
        listOf("cmd phone get-allowed-network-types-for-users -s $slot")

    fun enableCommands(preset: Preset, slots: List<Int>): List<String> {
        if (preset.enableCommands.isNotEmpty()) return preset.enableCommands
        if (preset.legacy) {
            val settings = slots.map { "settings put global ${settingsKeyForSlot(it)} 11" }
            return settings + airplaneCommands()
        }
        val slotCommands = slots.flatMap { slot ->
            listOf(
                "cmd phone ims disable -s $slot",
                "cmd phone set-allowed-network-types-for-users -s $slot $LTE_ONLY_MASK"
            )
        }
        val packageCommands = packagesToDisable(preset).map { "pm disable-user --user 0 $it" }
        return slotCommands + packageCommands
    }

    fun restoreCommands(preset: Preset, savedMasks: Map<Int, String>, slots: List<Int>): List<String> {
        if (preset.disableCommands.isNotEmpty()) return preset.disableCommands
        if (preset.legacy) {
            val settings = slots.map { "settings put global ${settingsKeyForSlot(it)} 0" }
            return settings + airplaneCommands()
        }
        val slotCommands = slots.flatMap { slot ->
            buildList {
                savedMasks[slot]?.let { add("cmd phone set-allowed-network-types-for-users -s $slot $it") }
                if (preset.requiresImsCmd) add("cmd phone ims enable -s $slot")
            }
        }
        val packageCommands = packagesToEnable(preset).map { "pm enable $it" }
        return slotCommands + packageCommands
    }

    fun verifyImsCommands(slot: Int): List<String> = listOf(
        "cmd phone ims get-ims-service -s $slot -d",
        "cmd phone ims get-ims-service -s $slot -c"
    )

    private fun settingsKeyForSlot(slot: Int): String = "preferred_network_mode" + when (slot) {
        1 -> "1"
        2 -> "2"
        else -> ""
    }

    private fun airplaneCommands(): List<String> = listOf(
        "cmd connectivity airplane-mode enable",
        "cmd connectivity airplane-mode disable"
    )

    private fun packagesToDisable(preset: Preset): List<String> = when (preset.id) {
        "samsung" -> listOf(IMS_PKG_SAMSUNG)
        "oneplus", "vivo" -> listOf(IMS_PKG_QC, IMS_PKG_MTK)
        else -> emptyList()
    }

    private fun packagesToEnable(preset: Preset): List<String> = when (preset.id) {
        "samsung" -> listOf(IMS_PKG_SAMSUNG)
        "oneplus", "vivo" -> listOf(IMS_PKG_QC, IMS_PKG_MTK)
        "universal" -> listOf(IMS_PKG_GOOGLE)
        else -> emptyList()
    }
}
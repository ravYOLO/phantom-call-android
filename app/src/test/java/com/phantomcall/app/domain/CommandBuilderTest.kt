package com.phantomcall.app.domain

import com.phantomcall.app.data.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandBuilderTest {

    private fun preset(id: String, requiresIms: Boolean = true, legacy: Boolean = false, enable: List<String> = emptyList(), disable: List<String> = emptyList()) =
        Preset(id = id, nameKey = null, nameOverride = null, enableCommands = enable, disableCommands = disable, requiresImsCmd = requiresIms, legacy = legacy)

    @Test
    fun samsungUsesPackageDisableWithoutImsCmd() {
        val p = preset("samsung", requiresIms = false)
        val cmds = CommandBuilder.enableCommands(p, listOf(0))
        assertTrue(cmds.any { it.contains("pm disable-user") && it.contains("com.sec.imsservice") })
        val restored = CommandBuilder.restoreCommands(p, emptyMap(), listOf(0))
        assertFalse(restored.any { it.contains("ims enable") })
    }

    @Test
    fun oneplusDisablesBothChipsets() {
        val cmds = CommandBuilder.enableCommands(preset("oneplus"), listOf(0))
        assertTrue(cmds.any { it.contains("org.codeaurora.ims") })
        assertTrue(cmds.any { it.contains("com.mediatek.ims") })
    }

    @Test
    fun legacyUsesPreferredNetworkModeAndAirplane() {
        val cmds = CommandBuilder.enableCommands(preset("legacy", legacy = true), listOf(0))
        assertTrue(cmds.any { it.contains("preferred_network_mode") })
        assertTrue(cmds.any { it.contains("airplane-mode") })
    }

    @Test
    fun restoreUsesSavedMask() {
        val saved = mapOf(0 to "01000001000000000000")
        val cmds = CommandBuilder.restoreCommands(preset("pixel"), saved, listOf(0))
        assertTrue(cmds.any { it.contains("set-allowed-network-types-for-users -s 0 01000001000000000000") })
    }

    @Test
    fun bothSlotsExpanded() {
        val cmds = CommandBuilder.enableCommands(preset("pixel"), listOf(0, 1))
        assertEquals(2, cmds.filter { it.contains("ims disable") }.size)
    }

    @Test
    fun customPresetCommandsUsedVerbatim() {
        val p = preset("custom1", enable = listOf("mycmd a", "mycmd b"))
        assertEquals(listOf("mycmd a", "mycmd b"), CommandBuilder.enableCommands(p, listOf(0)))
    }
}
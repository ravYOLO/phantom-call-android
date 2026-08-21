package com.phantomcall.app.domain

object MaskParser {

    fun parseMasks(stdout: String): Map<Int, String> {
        val masks = mutableMapOf<Int, String>()
        stdout.lineSequence().forEach { line ->
            val mask = line.trim().split(Regex("\\s+")).lastOrNull()
                ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) } ?: return@forEach
            val slot = Regex("\\d+").find(line)?.value?.toIntOrNull() ?: return@forEach
            masks[slot] = mask
        }
        return masks
    }

    fun maskForSlot(masks: Map<Int, String>, slot: Int): String? = masks[slot]
}
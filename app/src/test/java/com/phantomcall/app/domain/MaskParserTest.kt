package com.phantomcall.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskParserTest {

    @Test
    fun parsesSlotAndMask() {
        val out = "Allowed network types for user 0\nSlot 0: 01000001000000000000\nSlot 1: 01100001000000000000\n"
        val m = MaskParser.parseMasks(out)
        assertEquals("01000001000000000000", m[0])
        assertEquals("01100001000000000000", m[1])
    }

    @Test
    fun garbageReturnsEmptyMap() {
        assertTrue(MaskParser.parseMasks("no masks here\njust plain text\n").isEmpty())
    }

    @Test
    fun maskForSlotReturnsNullWhenAbsent() {
        assertNull(MaskParser.maskForSlot(mapOf(0 to "01000001000000000000"), 1))
        assertNull(MaskParser.maskForSlot(emptyMap(), 0))
    }
}
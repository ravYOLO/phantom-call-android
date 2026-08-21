package com.phantomcall.app.data

enum class SimSlotMode(val slots: List<Int>) { BOTH(listOf(0, 1)), SIM1(listOf(0)), SIM2(listOf(1)) }
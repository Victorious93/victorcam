package com.victorious.cam.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedSchedulePlannerTest {
    @Before
    fun clearBus() = FeedSwitchBus.clear()

    @Test
    fun nextAndDue() {
        val a = FeedSchedule(1, 10, "A", 1000)
        val b = FeedSchedule(2, 11, "B", 2000)
        assertEquals(a, FeedSchedulePlanner.nextSwitch(listOf(a, b), nowMs = 500))
        assertEquals(listOf(a), FeedSchedulePlanner.due(listOf(a, b), nowMs = 1500))
        assertNull(FeedSchedulePlanner.nextSwitch(listOf(a.copy(enabled = false)), 0))
    }

    @Test
    fun hotkeyOrderDedupes() {
        assertEquals(listOf(1L, 2L, 3L), FeedSchedulePlanner.hotkeyOrder(listOf(1, 2, 2, 3)))
    }

    @Test
    fun busPublishes() {
        var seen: FeedSwitchEvent? = null
        val listener: (FeedSwitchEvent) -> Unit = { seen = it }
        FeedSwitchBus.subscribe(listener)
        FeedSwitchBus.publish(FeedSwitchEvent(9, "X", "test"))
        assertEquals(9L, seen?.presetId)
        assertTrue(FeedSwitchBus.lastEvent()?.presetName == "X")
        FeedSwitchBus.unsubscribe(listener)
    }
}

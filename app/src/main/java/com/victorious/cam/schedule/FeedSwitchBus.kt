package com.victorious.cam.schedule

import java.util.concurrent.CopyOnWriteArrayList

data class FeedSwitchEvent(val presetId: Long, val presetName: String, val source: String)

object FeedSwitchBus {
    private val listeners = CopyOnWriteArrayList<(FeedSwitchEvent) -> Unit>()
    @Volatile private var last: FeedSwitchEvent? = null

    fun publish(event: FeedSwitchEvent) {
        last = event
        listeners.forEach { it.invoke(event) }
    }

    fun lastEvent(): FeedSwitchEvent? = last

    fun subscribe(listener: (FeedSwitchEvent) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (FeedSwitchEvent) -> Unit) {
        listeners.remove(listener)
    }

    fun clear() {
        listeners.clear()
        last = null
    }
}

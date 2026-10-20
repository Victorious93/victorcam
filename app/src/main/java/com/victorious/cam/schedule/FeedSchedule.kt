package com.victorious.cam.schedule

/**
 * Schedule entry to switch to a saved preset at [triggerAtEpochMs].
 */
data class FeedSchedule(
    val id: Long = 0,
    val presetId: Long,
    val presetName: String,
    val triggerAtEpochMs: Long,
    val enabled: Boolean = true
) {
    init {
        require(presetId > 0) { "presetId" }
        require(triggerAtEpochMs > 0) { "triggerAtEpochMs" }
    }
}

/**
 * Pure scheduler planner for tests + WorkManager/AlarmManager wiring.
 */
object FeedSchedulePlanner {
    fun nextSwitch(schedules: List<FeedSchedule>, nowMs: Long): FeedSchedule? =
        schedules
            .filter { it.enabled && it.triggerAtEpochMs >= nowMs }
            .minByOrNull { it.triggerAtEpochMs }

    fun due(schedules: List<FeedSchedule>, nowMs: Long): List<FeedSchedule> =
        schedules.filter { it.enabled && it.triggerAtEpochMs <= nowMs }

    fun hotkeyOrder(presetIds: List<Long>): List<Long> = presetIds.distinct()
}

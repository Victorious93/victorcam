package com.victorious.cam.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules preset switches via WorkManager (primary) and AlarmManager (exact when allowed).
 */
class FeedSwitchScheduler(private val context: Context) {

    fun schedule(schedule: FeedSchedule) {
        val delay = (schedule.triggerAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0)
        val data = Data.Builder()
            .putLong(KEY_PRESET_ID, schedule.presetId)
            .putString(KEY_PRESET_NAME, schedule.presetName)
            .build()
        val req = OneTimeWorkRequestBuilder<FeedSwitchWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tagFor(schedule.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(schedule.id),
            ExistingWorkPolicy.REPLACE,
            req
        )
        // Best-effort exact alarm
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            Intent(context, FeedSwitchReceiver::class.java).apply {
                putExtra(KEY_PRESET_ID, schedule.presetId)
                putExtra(KEY_PRESET_NAME, schedule.presetName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.triggerAtEpochMs, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.triggerAtEpochMs, pi)
        }
    }

    fun cancel(scheduleId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(scheduleId))
    }

    companion object {
        const val KEY_PRESET_ID = "preset_id"
        const val KEY_PRESET_NAME = "preset_name"
        fun tagFor(id: Long) = "feed_switch_$id"
        fun uniqueName(id: Long) = "victorcam_switch_$id"
    }
}

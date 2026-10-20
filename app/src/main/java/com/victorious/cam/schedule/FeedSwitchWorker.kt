package com.victorious.cam.schedule

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class FeedSwitchWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val presetId = inputData.getLong(FeedSwitchScheduler.KEY_PRESET_ID, -1)
        val name = inputData.getString(FeedSwitchScheduler.KEY_PRESET_NAME) ?: ""
        if (presetId <= 0) return Result.failure()
        FeedSwitchBus.publish(FeedSwitchEvent(presetId, name, source = "workmanager"))
        FeedSwitchNotifier.showQuickSwitch(applicationContext, presetId, name)
        return Result.success()
    }
}

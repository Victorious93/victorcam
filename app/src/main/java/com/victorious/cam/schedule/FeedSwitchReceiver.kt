package com.victorious.cam.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FeedSwitchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val presetId = intent?.getLongExtra(FeedSwitchScheduler.KEY_PRESET_ID, -1) ?: return
        val name = intent.getStringExtra(FeedSwitchScheduler.KEY_PRESET_NAME) ?: ""
        if (presetId <= 0) return
        when (intent.action) {
            ACTION_HOTKEY_NEXT,
            ACTION_HOTKEY_PREV,
            null -> {
                FeedSwitchBus.publish(FeedSwitchEvent(presetId, name, source = intent?.action ?: "alarm"))
                FeedSwitchNotifier.showQuickSwitch(context, presetId, name)
            }
        }
    }

    companion object {
        const val ACTION_HOTKEY_NEXT = "com.victorious.cam.action.HOTKEY_NEXT"
        const val ACTION_HOTKEY_PREV = "com.victorious.cam.action.HOTKEY_PREV"
    }
}

package com.victorious.cam.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.victorious.cam.MainActivity
import com.victorious.cam.R

/**
 * Quick-switch notification with Next/Prev hotkey actions between presets.
 */
object FeedSwitchNotifier {
    private const val CHANNEL = "victorcam_feed_switch"
    private const val NOTIF_ID = 4301

    fun showQuickSwitch(context: Context, presetId: Long, presetName: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, FeedSwitchReceiver::class.java).setAction(FeedSwitchReceiver.ACTION_HOTKEY_NEXT)
                .putExtra(FeedSwitchScheduler.KEY_PRESET_ID, presetId)
                .putExtra(FeedSwitchScheduler.KEY_PRESET_NAME, presetName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prev = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, FeedSwitchReceiver::class.java).setAction(FeedSwitchReceiver.ACTION_HOTKEY_PREV)
                .putExtra(FeedSwitchScheduler.KEY_PRESET_ID, presetId)
                .putExtra(FeedSwitchScheduler.KEY_PRESET_NAME, presetName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle(context.getString(R.string.feed_switch_title))
            .setContentText(context.getString(R.string.feed_switch_text, presetName))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.feed_switch_prev), prev)
            .addAction(0, context.getString(R.string.feed_switch_next), next)
            .setOngoing(false)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notif)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Feed switch", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

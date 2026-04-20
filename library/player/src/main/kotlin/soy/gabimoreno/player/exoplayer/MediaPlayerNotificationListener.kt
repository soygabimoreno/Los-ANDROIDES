package soy.gabimoreno.player.exoplayer

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import soy.gabimoreno.player.service.MediaPlayerService

class MediaPlayerNotificationListener(
    private val mediaService: MediaPlayerService,
) : PlayerNotificationManager.NotificationListener {
    override fun onNotificationCancelled(
        notificationId: Int,
        dismissedByUser: Boolean,
    ) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        mediaService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean,
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        mediaService.apply {
            if (ongoing || !isForegroundService) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java),
                )
                startForeground(
                    PLAYBACK_NOTIFICATION_ID,
                    notification,
                )
                isForegroundService = true
            }
        }
    }
}

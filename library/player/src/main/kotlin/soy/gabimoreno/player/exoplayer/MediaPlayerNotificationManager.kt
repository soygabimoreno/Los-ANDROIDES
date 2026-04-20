package soy.gabimoreno.player.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import soy.gabimoreno.player.R

class MediaPlayerNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit,
) {
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager =
            createNotificationManager(mediaController, sessionToken, notificationListener)
    }

    fun showNotification(player: Player) {
        notificationManager.setPlayer(player)
    }

    private fun createNotificationManager(
        mediaController: MediaControllerCompat,
        sessionToken: MediaSessionCompat.Token,
        notificationListener: PlayerNotificationManager.NotificationListener,
    ): PlayerNotificationManager =
        PlayerNotificationManager
            .Builder(
                context,
                PLAYBACK_NOTIFICATION_ID,
                PLAYBACK_NOTIFICATION_CHANNEL_ID,
            ).setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            .setNotificationListener(notificationListener)
            .setChannelNameResourceId(R.string.playback_notification_channel_name)
            .setChannelDescriptionResourceId(R.string.playback_notification_channel_description)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_stat_name)
                setMediaSessionToken(sessionToken)
                setUseStopAction(true)
                setUseNextActionInCompactView(true)
                setUsePreviousActionInCompactView(true)
            }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat,
    ) : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            mediaController.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence =
            mediaController.metadata.description.subtitle
                .toString()

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mediaController.metadata.description.title
                .toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {
            Glide
                .with(context)
                .asBitmap()
                .load(mediaController.metadata.description.iconUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onLoadCleared(placeholder: Drawable?) = Unit

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?,
                        ) {
                            callback.onBitmap(resource)
                        }
                    },
                )
            return null
        }
    }
}

private const val PLAYBACK_NOTIFICATION_CHANNEL_ID = "PLAYBACK_NOTIFICATION_CHANNEL_ID"
const val PLAYBACK_NOTIFICATION_ID = 20220615

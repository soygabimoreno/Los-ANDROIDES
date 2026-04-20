package soy.gabimoreno.player.exoplayer

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator

class MediaPlayerQueueNavigator(
    mediaSession: MediaSessionCompat,
    private val mediaSource: AudioMediaSource,
) : TimelineQueueNavigator(mediaSession) {
    override fun getMediaDescription(
        player: Player,
        windowIndex: Int,
    ): MediaDescriptionCompat = mediaSource.audioMediaMetadataCompat[windowIndex].description
}

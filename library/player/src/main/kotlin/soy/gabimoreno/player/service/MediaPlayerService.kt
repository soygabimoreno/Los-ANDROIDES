package soy.gabimoreno.player.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import soy.gabimoreno.player.di.CacheProvider
import soy.gabimoreno.player.exoplayer.AudioMediaSource
import soy.gabimoreno.player.exoplayer.MediaPlaybackPreparer
import soy.gabimoreno.player.exoplayer.MediaPlayerNotificationListener
import soy.gabimoreno.player.exoplayer.MediaPlayerNotificationManager
import soy.gabimoreno.player.exoplayer.MediaPlayerQueueNavigator
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var mediaSource: AudioMediaSource

    @Inject
    @Named(MAIN_ACTIVITY_PENDING_INTENT)
    lateinit var activityPendingIntent: PendingIntent

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var progressJob: Job? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var mediaPlayerNotificationManager: MediaPlayerNotificationManager

    private var currentPlayingMedia: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    var isForegroundService: Boolean = false

    companion object {
        private const val TAG = "MediaPlayerService"

        var currentDuration: Long = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession =
            MediaSessionCompat(this, TAG).apply {
                setSessionActivity(activityPendingIntent)
                isActive = true
            }

        val mediaPlaybackPreparer =
            MediaPlaybackPreparer(mediaSource) { mediaMetadata ->
                currentPlayingMedia = mediaMetadata
                preparePlayer(mediaSource.audioMediaMetadataCompat, mediaMetadata, true)
            }
        mediaSessionConnector =
            MediaSessionConnector(mediaSession).apply {
                setPlaybackPreparer(mediaPlaybackPreparer)
                setQueueNavigator(MediaPlayerQueueNavigator(mediaSession, mediaSource))
                setPlayer(exoPlayer)
            }

        this.sessionToken = mediaSession.sessionToken

        mediaPlayerNotificationManager =
            MediaPlayerNotificationManager(
                this,
                mediaSession.sessionToken,
                MediaPlayerNotificationListener(this),
            ) {
                currentDuration = exoPlayer.duration
            }
        exoPlayer.addListener(progressListener)
    }

    private val progressListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                }
            }
        }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return

        progressJob =
            serviceScope.launch(Dispatchers.Main) {
                while (isActive) {
                    val position = exoPlayer.currentPosition
                    val duration = exoPlayer.duration.takeIf { it > 0 } ?: 1L
                    val progress = position.toFloat() / duration

                    updatePlaybackState(progress)

                    delay(POSITION_UPDATE_INTERVAL)
                }
            }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    private fun updatePlaybackState(progress: Float) {
        val state =
            PlaybackStateCompat
                .Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, exoPlayer.currentPosition, 1f)
                .setExtras(
                    Bundle().apply {
                        putFloat(PROGRESS_EXTRA, progress)
                    },
                ).build()

        mediaSession.setPlaybackState(state)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = Service.START_STICKY

    override fun onCustomAction(
        action: String,
        extras: Bundle?,
        result: Result<Bundle>,
    ) {
        super.onCustomAction(action, extras, result)
        when (action) {
            START_MEDIA_PLAYBACK_ACTION -> {
                mediaPlayerNotificationManager.showNotification(exoPlayer)
            }

            REFRESH_MEDIA_BROWSER_CHILDREN -> {
                mediaSource.refresh()
                notifyChildrenChanged(MEDIA_ROOT_ID)
            }

            SET_PLAYBACK_SPEED -> {
                val speed = extras?.getFloat(PLAYBACK_SPEED_EXTRA, 1.0f) ?: 1.0f
                exoPlayer.playbackParameters = PlaybackParameters(speed)
            }

            else -> {
                Unit
            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(MEDIA_ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent =
                    mediaSource.whenReady { isInitialized ->
                        if (isInitialized) {
                            result.sendResult(mediaSource.asMediaItems())
                            if (!isPlayerInitialized &&
                                mediaSource.audioMediaMetadataCompat.isNotEmpty()
                            ) {
                                isPlayerInitialized = true
                            }
                        } else {
                            result.sendResult(null)
                        }
                    }
                if (!resultsSent) {
                    result.detach()
                }
            }

            else -> {
                Unit
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(progressListener)
        stopProgressUpdates()
        exoPlayer.release()
        CacheProvider.release()
    }

    private fun preparePlayer(
        mediaMetaData: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playWhenReady: Boolean,
    ) {
        val indexToPlay = if (currentPlayingMedia == null) 0 else mediaMetaData.indexOf(itemToPlay)
        exoPlayer.setMediaSource(mediaSource.asMediaSource(dataSourceFactory))
        exoPlayer.prepare()
        exoPlayer.seekTo(indexToPlay, 0L)
        exoPlayer.playWhenReady = playWhenReady
    }
}

const val MEDIA_ROOT_ID = "TUVESUFfUk9PVF9JRA" // TODO: change it
const val PLAYBACK_SPEED_EXTRA = "PLAYBACK_SPEED_EXTRA"
const val PROGRESS_EXTRA = "PROGRESS"
const val REFRESH_MEDIA_BROWSER_CHILDREN = "REFRESH_MEDIA_BROWSER_CHILDREN"
const val SET_PLAYBACK_SPEED = "SET_PLAYBACK_SPEED"
const val START_MEDIA_PLAYBACK_ACTION = "START_MEDIA_PLAYBACK_ACTION"
private const val POSITION_UPDATE_INTERVAL = 1_000L
const val MAIN_ACTIVITY_PENDING_INTENT = "MAIN_ACTIVITY_PENDING_INTENT"

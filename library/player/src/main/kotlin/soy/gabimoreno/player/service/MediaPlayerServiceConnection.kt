package soy.gabimoreno.player.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.player.exoplayer.AudioMediaSource
import soy.gabimoreno.player.extension.currentPosition
import javax.inject.Inject

class MediaPlayerServiceConnection
    @Inject
    constructor(
        context: Context,
        private val mediaSource: AudioMediaSource,
    ) {
        var playbackState = mutableStateOf<PlaybackStateCompat?>(null)
        var currentPlayingAudio = mutableStateOf<Audio?>(null)
        var onAudioChanged: (() -> Unit)? = null

        lateinit var mediaController: MediaControllerCompat

        private var isConnected: Boolean = false

        val transportControls: MediaControllerCompat.TransportControls
            get() = mediaController.transportControls

        private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

        private val mediaBrowser =
            MediaBrowserCompat(
                context,
                ComponentName(context, MediaPlayerService::class.java),
                mediaBrowserConnectionCallback,
                null,
            ).apply {
                connect()
            }

        private val _progressFlow = MutableStateFlow(0f)
        val progressFlow: StateFlow<Float> = _progressFlow.asStateFlow()

        fun playAudios(audios: List<Audio>) {
            mediaSource.setAudios(audios)
            mediaBrowser.sendCustomAction(START_MEDIA_PLAYBACK_ACTION, null, null)
        }

        fun rewind(seconds: Int = 10) {
            playbackState.value?.currentPosition?.let { currentPosition ->
                transportControls.seekTo(currentPosition - seconds * MILLIS_IN_SECOND)
            }
        }

        fun fastForward(seconds: Int = 10) {
            playbackState.value?.currentPosition?.let { currentPosition ->
                transportControls.seekTo(currentPosition + seconds * MILLIS_IN_SECOND)
            }
        }

        fun skipToPrevious() {
            transportControls.skipToPrevious()
        }

        fun skipToNext() {
            transportControls.skipToNext()
        }

        fun setPlaybackSpeed(speed: Float) {
            val extras =
                Bundle().apply {
                    putFloat(PLAYBACK_SPEED_EXTRA, speed)
                }
            mediaBrowser.sendCustomAction(SET_PLAYBACK_SPEED, extras, null)
        }

        fun subscribe(
            parentId: String,
            callback: MediaBrowserCompat.SubscriptionCallback,
        ) {
            mediaBrowser.subscribe(parentId, callback)
        }

        fun unsubscribe(
            parentId: String,
            callback: MediaBrowserCompat.SubscriptionCallback,
        ) {
            mediaBrowser.unsubscribe(parentId, callback)
        }

        fun refreshMediaBrowserChildren() {
            mediaBrowser.sendCustomAction(REFRESH_MEDIA_BROWSER_CHILDREN, null, null)
        }

        private inner class MediaBrowserConnectionCallback(
            private val context: Context,
        ) : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                isConnected = true
                mediaController =
                    MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                        registerCallback(MediaControllerCallback())
                    }
            }

            override fun onConnectionSuspended() {
                super.onConnectionSuspended()
                isConnected = false
            }

            override fun onConnectionFailed() {
                super.onConnectionFailed()
                isConnected = false
            }
        }

        private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                super.onPlaybackStateChanged(state)
                playbackState.value = state

                val progress = state?.extras?.getFloat(PROGRESS_EXTRA, 0f) ?: 0f
                _progressFlow.value = progress
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                super.onMetadataChanged(metadata)
                currentPlayingAudio.value =
                    metadata?.let {
                        mediaSource.audios.find {
                            it.id == metadata.description?.mediaId
                        }
                    }
                onAudioChanged?.invoke()
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                mediaBrowserConnectionCallback.onConnectionSuspended()
            }
        }
    }

private const val MILLIS_IN_SECOND = 1_000

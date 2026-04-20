package soy.gabimoreno.player.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import soy.gabimoreno.coredomain.model.audio.Audio
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMediaSource
    @Inject
    constructor() {
        var audioMediaMetadataCompat: List<MediaMetadataCompat> = emptyList()
        var audios: List<Audio> = emptyList()
            private set
        private val onReadyListeners = mutableListOf<OnReadyListener>()

        private var state: MusicSourceState =
            MusicSourceState.CREATED
            set(value) {
                if (value == MusicSourceState.INITIALIZED || value == MusicSourceState.ERROR) {
                    synchronized(onReadyListeners) {
                        field = value
                        onReadyListeners.forEach { listener ->
                            listener(isReady)
                        }
                    }
                } else {
                    field = value
                }
            }

        private val isReady: Boolean
            get() = state == MusicSourceState.INITIALIZED

        fun setAudios(audios: List<Audio>) {
            state = MusicSourceState.INITIALIZING
            this.audios = audios
            audioMediaMetadataCompat =
                audios.map { audio ->
                    MediaMetadataCompat
                        .Builder()
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                            audio.id,
                        ).putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            audio.saga.author,
                        ).putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            audio.title,
                        ).putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                            audio.saga.title,
                        ).putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            audio.audioUrl,
                        ).putString(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                            audio.imageUrl,
                        ).build()
                }
            state = MusicSourceState.INITIALIZED
        }

        fun asMediaSource(dataSourceFactory: DataSource.Factory): ConcatenatingMediaSource {
            val concatenatingMediaSource = ConcatenatingMediaSource()
            audioMediaMetadataCompat.forEach { metadata ->
                val mediaItem =
                    MediaItem.fromUri(
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toUri(),
                    )
                val mediaSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                concatenatingMediaSource.addMediaSource(mediaSource)
            }
            return concatenatingMediaSource
        }

        fun asMediaItems() =
            audioMediaMetadataCompat
                .map { metadata ->
                    val description =
                        MediaDescriptionCompat
                            .Builder()
                            .setMediaId(metadata.description.mediaId)
                            .setTitle(metadata.description.title)
                            .setSubtitle(metadata.description.subtitle)
                            .setIconUri(metadata.description.iconUri)
                            .setMediaUri(metadata.description.mediaUri)
                            .build()
                    MediaBrowserCompat.MediaItem(
                        description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE,
                    )
                }.toMutableList()

        fun whenReady(listener: OnReadyListener): Boolean =
            if (state == MusicSourceState.CREATED || state == MusicSourceState.INITIALIZING) {
                onReadyListeners += listener
                false
            } else {
                listener(isReady)
                true
            }

        fun refresh() {
            onReadyListeners.clear()
            state = MusicSourceState.CREATED
        }
    }

typealias OnReadyListener = (Boolean) -> Unit

enum class MusicSourceState {
    CREATED,
    INITIALIZING,
    INITIALIZED,
    ERROR,
}

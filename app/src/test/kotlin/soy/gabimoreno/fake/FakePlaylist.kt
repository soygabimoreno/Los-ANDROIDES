package soy.gabimoreno.fake

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.local.playlist.model.PlaylistDbModel
import soy.gabimoreno.data.local.playlist.model.PlaylistItemsDbModel
import soy.gabimoreno.data.remote.model.Category
import soy.gabimoreno.domain.model.content.Playlist
import soy.gabimoreno.domain.model.content.PlaylistAudioItem

fun buildPlaylist(id: Int = 1) =
    Playlist(
        id = id,
        title = "This is a title",
        description = "This is a description",
        items = buildPlaylistItems(),
        position = 0,
    )

fun buildPlaylistDbModel(id: Int = 1) =
    PlaylistDbModel(
        id = id,
        title = "This is a title",
        description = "This is a description",
        position = id - 1,
    )

fun buildNewPlaylist(id: Int = 1) =
    Playlist(
        id = id,
        title = "This is a title",
        description = "This is a description",
        items = emptyList(),
        position = 0,
    )

fun buildPlaylistItems(): List<PlaylistAudioItem> =
    (1..3).map { index ->
        buildAudioItem(audioId = index.toString(), position = index - 1)
    }

fun buildPlaylistItemsDbModel(id: Int = 1) =
    PlaylistItemsDbModel(
        id = id,
        audioItemId = "audio-$id",
        playlistId = 1,
        position = id - 1,
    )

fun buildAudioItem(
    audioId: String,
    position: Int = 0,
) = PlaylistAudioItem(
    id = audioId,
    url = "",
    audioUrl = "",
    imageUrl = "",
    saga = Saga(author = "This is publisher", title = "This is saga title"),
    thumbnailUrl = "",
    pubDateMillis = 0,
    title = "This is a title",
    audioLengthInSeconds = 2700,
    description = "This is a description",
    category = Category.PREMIUM,
    excerpt = "excerpt",
    hasBeenListened = false,
    position = position,
    playlistItemId = position,
    markedAsFavorite = false,
)

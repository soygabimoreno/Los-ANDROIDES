package soy.gabimoreno.fake

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.domain.model.podcast.Episode

fun buildEpisodes() =
    listOf(
        buildEpisode("d332a4c2-4282-45c0-9ccc-2aeaab8df48d"),
        buildEpisode("d332a4c2-4282-45c0-9ccc-2aeaab8df48e"),
        buildEpisode("d332a4c2-4282-45c0-9ccc-2aeaab8df48f"),
    )

fun buildEpisode(
    id: String = "d332a4c2-4282-45c0-9ccc-2aeaab8df48x",
    hasBeenListened: Boolean = false,
    markedAsFavorite: Boolean = false,
) = Episode(
    id = id,
    url = "",
    audioUrl = "",
    imageUrl = "",
    saga = Saga(author = "This is publisher", title = "This is saga title"),
    thumbnailUrl = "",
    pubDateMillis = 0,
    title = "This is a title",
    audioLengthInSeconds = 2700,
    description = "This is a description",
    hasBeenListened = hasBeenListened,
    markedAsFavorite = markedAsFavorite,
)

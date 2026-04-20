package soy.gabimoreno.fake

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.local.podcast.model.PodcastDbModel

fun buildPodcastDbModels() =
    listOf<PodcastDbModel>(
        buildPodcastDbModel("d332a4c2-4282-45c0-9ccc-2aeaab8df48d"),
        buildPodcastDbModel("d332a4c2-4282-45c0-9ccc-2aeaab8df48e"),
        buildPodcastDbModel("d332a4c2-4282-45c0-9ccc-2aeaab8df48f"),
    )

fun buildPodcastDbModel(
    id: String = "d332a4c2-4282-45c0-9ccc-2aeaab8df48x",
    hasBeenListened: Boolean = false,
    markedAsFavorite: Boolean = false,
) = PodcastDbModel(
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

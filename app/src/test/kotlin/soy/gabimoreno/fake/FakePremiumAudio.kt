package soy.gabimoreno.fake

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.remote.model.Category
import soy.gabimoreno.domain.model.content.PremiumAudio

fun buildPremiumAudios() =
    listOf(
        buildPremiumAudio("1"),
        buildPremiumAudio("2"),
        buildPremiumAudio("3"),
    )

fun buildPremiumAudio(
    id: String = "1",
    hasBeenListened: Boolean = false,
    markedAsFavorite: Boolean = false,
): PremiumAudio =
    PremiumAudio(
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
        category = Category.PREMIUM,
        excerpt = "excerpt",
        hasBeenListened = hasBeenListened,
        markedAsFavorite = markedAsFavorite,
    )

package soy.gabimoreno.data.remote.mapper

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.domain.model.content.Post
import soy.gabimoreno.domain.model.content.PremiumAudio

fun List<Post>.toPremiumAudios(): List<PremiumAudio> {
    val premiumAudios = mutableListOf<PremiumAudio>()
    this.forEach { post ->
        if (
            post.audioUrl != null &&
            post.subcategoryTitle != null &&
            post.category != null
        ) {
            premiumAudios.add(
                PremiumAudio(
                    id = post.getPremiumAudioId(),
                    title = post.title,
                    description = post.content,
                    saga = Saga(author = post.author, post.subcategoryTitle),
                    url = post.url,
                    audioUrl = post.audioUrl,
                    imageUrl = post.category.coverUrl,
                    thumbnailUrl = post.category.coverUrl,
                    pubDateMillis = post.pubDateMillis,
                    // TODO: AudioLengthInSeconds is unknown for now
                    audioLengthInSeconds = EMPTY_AUDIO_LENGTH_IN_SECONDS,
                    category = post.category,
                    excerpt = post.excerpt,
                    hasBeenListened = false,
                    markedAsFavorite = false,
                ),
            )
        }
    }
    return premiumAudios
}

internal const val EMPTY_AUDIO_LENGTH_IN_SECONDS = 0

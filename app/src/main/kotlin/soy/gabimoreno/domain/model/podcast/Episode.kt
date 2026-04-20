package soy.gabimoreno.domain.model.podcast

import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.coredomain.model.audio.Saga

data class Episode(
    override val id: String,
    override val title: String,
    override val description: String,
    override val saga: Saga,
    override val url: String,
    override val audioUrl: String,
    override val imageUrl: String,
    override val thumbnailUrl: String,
    override val pubDateMillis: Long,
    override val audioLengthInSeconds: Int,
    override val hasBeenListened: Boolean,
    override val markedAsFavorite: Boolean,
) : Audio

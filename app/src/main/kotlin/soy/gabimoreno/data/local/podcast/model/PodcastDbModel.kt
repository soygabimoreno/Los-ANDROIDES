package soy.gabimoreno.data.local.podcast.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import soy.gabimoreno.coredomain.model.audio.Saga

@Entity
data class PodcastDbModel(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val saga: Saga,
    val url: String,
    val audioUrl: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val pubDateMillis: Long,
    val audioLengthInSeconds: Int,
    val hasBeenListened: Boolean,
    val markedAsFavorite: Boolean,
)

package soy.gabimoreno.domain.model.content

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.remote.model.Category

data class AudioCourse(
    val id: String,
    val title: String,
    val description: String,
    val excerpt: String,
    val saga: Saga,
    val url: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val pubDateMillis: Long,
    val audioLengthInSeconds: Int,
    val category: Category,
    val isPurchased: Boolean,
    val audios: List<AudioCourseItem> = emptyList(),
)

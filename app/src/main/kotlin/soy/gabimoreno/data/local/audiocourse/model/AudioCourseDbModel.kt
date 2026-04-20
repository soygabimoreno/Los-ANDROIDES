package soy.gabimoreno.data.local.audiocourse.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.remote.model.Category

@Entity
data class AudioCourseDbModel(
    @PrimaryKey
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
)

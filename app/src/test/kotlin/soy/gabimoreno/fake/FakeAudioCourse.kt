package soy.gabimoreno.fake

import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.local.audiocourse.model.AudioCourseDbModel
import soy.gabimoreno.data.local.audiocourse.model.AudioCourseItemDbModel
import soy.gabimoreno.data.local.audiocourse.model.AudioCourseWithItems
import soy.gabimoreno.data.remote.model.Category
import soy.gabimoreno.domain.model.content.AudioCourse
import soy.gabimoreno.domain.model.content.AudioCourseItem

fun buildAudioCourses(hasBeenListened: Boolean = false): List<AudioCourse> =
    (1..3).map { index ->
        buildAudioCourse(
            hasBeenListened = hasBeenListened,
            courseId = index.toString(),
        )
    }

fun buildAudioCourseItems(
    hasBeenListened: Boolean,
    courseIndex: Int,
) = listOf(
    buildAudioCourseItem().copy(id = "$courseIndex-1", hasBeenListened = hasBeenListened),
    buildAudioCourseItem().copy(id = "$courseIndex-2", hasBeenListened = hasBeenListened),
    buildAudioCourseItem().copy(id = "$courseIndex-3", hasBeenListened = hasBeenListened),
)

fun buildAudioCourse(
    hasBeenListened: Boolean = false,
    courseId: String = "1",
    audios: List<AudioCourseItem> = buildAudioCourseItems(hasBeenListened, courseId.toInt()),
): AudioCourse =
    AudioCourse(
        id = courseId,
        url = "",
        videoUrl = "",
        thumbnailUrl = "",
        pubDateMillis = 0,
        title = "This is a title",
        audioLengthInSeconds = 2700,
        description = "This is a description",
        category = Category.AUDIOCOURSES,
        excerpt = "excerpt",
        saga = Saga(author = "This is publisher", title = "This is saga title"),
        isPurchased = false,
        audios = audios,
    )

fun buildAudioCourseItem(
    hasBeenListened: Boolean = false,
    markedAsFavorite: Boolean = false,
) = AudioCourseItem(
    id = "1-1",
    title = "item title",
    url = "item url",
    hasBeenListened = hasBeenListened,
    markedAsFavorite = markedAsFavorite,
)

fun buildAudioCoursesDbModel() =
    listOf(
        buildAudioCourseDbModel().copy(id = "1"),
        buildAudioCourseDbModel().copy(id = "2"),
        buildAudioCourseDbModel().copy(id = "3"),
    )

fun buildAudioCourseDbModel() =
    AudioCourseDbModel(
        id = "1",
        title = "This is a title",
        description = "This is a description",
        excerpt = "excerpt",
        saga = Saga(author = "This is publisher", title = "This is saga title"),
        url = "",
        videoUrl = "",
        thumbnailUrl = "",
        pubDateMillis = 0,
        audioLengthInSeconds = 2700,
        category = Category.AUDIOCOURSES,
        isPurchased = false,
    )

fun buildAudioCourseDbItems(idAudioCourse: String = "1") =
    listOf(
        buildAudioCourseItemDbModel(idAudioCourse).copy(id = "1-1"),
        buildAudioCourseItemDbModel(idAudioCourse).copy(id = "1-2"),
        buildAudioCourseItemDbModel(idAudioCourse).copy(id = "1-3"),
    )

fun buildAudioCourseItemDbModel(
    idAudioCourse: String = "1",
    hasBeenListened: Boolean = false,
    markedAsFavorite: Boolean = false,
) = AudioCourseItemDbModel(
    id = "$idAudioCourse-1",
    idAudioCourse = idAudioCourse,
    title = "item title",
    url = "item url",
    hasBeenListened = hasBeenListened,
    markedAsFavorite = markedAsFavorite,
)

fun buildAudioCourseWithItems() =
    AudioCourseWithItems(
        audioCourseDbModel = buildAudioCourseDbModel(),
        audioCourseItemDbModels = buildAudioCourseDbItems(),
    )

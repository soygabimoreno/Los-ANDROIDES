package soy.gabimoreno.presentation.screen.audiocourses.detail

import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.domain.model.content.AudioCourse

data class AudioCourseDetailState(
    val isLoading: Boolean = true,
    val audioCourse: AudioCourse? = null,
    val audio: Audio? = null,
    val audios: List<Audio> = emptyList(),
)

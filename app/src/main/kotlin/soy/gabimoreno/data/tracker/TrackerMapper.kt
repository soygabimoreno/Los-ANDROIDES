package soy.gabimoreno.data.tracker

import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.data.tracker.domain.TRACKER_KEY_AUDIO_ID
import soy.gabimoreno.data.tracker.domain.TRACKER_KEY_AUDIO_TITLE
import soy.gabimoreno.data.tracker.domain.TRACKER_KEY_EPISODE_ID
import soy.gabimoreno.data.tracker.domain.TRACKER_KEY_EPISODE_TITLE
import soy.gabimoreno.domain.model.podcast.Episode

fun Episode.toMap() =
    mapOf(
        TRACKER_KEY_EPISODE_ID to id,
        TRACKER_KEY_EPISODE_TITLE to title,
    )

fun Audio.toMap() =
    mapOf(
        TRACKER_KEY_AUDIO_ID to id,
        TRACKER_KEY_AUDIO_TITLE to title,
    )

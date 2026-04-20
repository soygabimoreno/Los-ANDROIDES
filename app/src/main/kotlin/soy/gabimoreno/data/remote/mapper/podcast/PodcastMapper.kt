package soy.gabimoreno.data.remote.mapper.podcast

import androidx.annotation.VisibleForTesting
import com.prof18.rssparser.model.ItunesItemData
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.domain.model.GABI_MORENO_WEB_BASE_URL
import soy.gabimoreno.domain.model.podcast.Episode
import soy.gabimoreno.domain.model.podcast.EpisodesWrapper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun RssChannel.toDomain(): EpisodesWrapper {
    val numberOfEpisodes = items.size.toLong()
    val podcastTitle = title ?: PODCAST_TITLE
    val podcastAuthor = podcastTitle.uppercase()

    return EpisodesWrapper(
        count = numberOfEpisodes,
        total = numberOfEpisodes,
        episodes =
            items.toDomain(
                podcastAuthor = podcastAuthor,
                podcastTitle = podcastTitle,
            ),
    )
}

fun List<RssItem>.toDomain(
    podcastAuthor: String,
    podcastTitle: String,
): List<Episode> =
    map { item ->
        item.toDomain(
            podcastAuthor = podcastAuthor,
            podcastTitle = podcastTitle,
        )
    }

fun RssItem.toDomain(
    podcastAuthor: String,
    podcastTitle: String,
): Episode {
    val cleanDescription = description?.removeAnchorMessage().orEmpty()
    val imageUrl = getEpisodeCoverUrl()

    return Episode(
        id = getEpisodeId(),
        url = getEpisodeUrl(),
        audioUrl = requireNotNull(audio),
        imageUrl = imageUrl,
        saga =
            Saga(
                author = podcastAuthor,
                title = podcastTitle,
            ),
        thumbnailUrl = imageUrl,
        pubDateMillis =
            pubDate?.let {
                rssDateFormat.parse(it)?.time
            } ?: 0L,
        title = title.orEmpty(),
        audioLengthInSeconds = itunesItemData.getAudioLengthInSeconds(),
        description = cleanDescription,
        hasBeenListened = false,
        markedAsFavorite = false,
    )
}

private fun String.removeAnchorMessage() = replace(ANCHOR_MESSAGE, "")

private fun RssItem.getEpisodeId(): String =
    requireNotNull(guid)
        .removePrefix(IVOOX_URL)

private fun RssItem.getEpisodeUrl(): String = "$GABI_MORENO_WEB_BASE_URL/${getEpisodeNumber()}"

private fun RssItem.getEpisodeCoverUrl(): String = itunesItemData?.image.orEmpty()

private fun RssItem.getEpisodeNumber(): String {
    itunesItemData?.episode?.let { return it }
    val titleValue = title ?: return ""
    val index = titleValue.indexOfFirst { it == '.' }
    if (index == -1) {
        return INVALID_EPISODE_NUMBER
    }
    return titleValue.substring(0, index)
}

private fun ItunesItemData?.getAudioLengthInSeconds(): Int {
    val duration = this?.duration ?: return EPISODE_AUDIO_LENGTH_DEFAULT_DURATION
    return duration.toIntOrNull() ?: parseTimeFormatDuration(duration)
}

private fun parseTimeFormatDuration(duration: String): Int {
    val dateFormat = SimpleDateFormat(HOURS_MINUTES_SECONDS_PATTERN, Locale.ROOT)
    val date = dateFormat.parse(duration) ?: return EPISODE_AUDIO_LENGTH_DEFAULT_DURATION

    return Calendar.getInstance().apply { time = date }.let { calendar ->
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        hours * ONE_HOUR_IN_SECONDS + minutes * ONE_MINUTE_IN_SECONDS + seconds
    }
}

private val rssDateFormat =
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

private const val PODCAST_TITLE = "Los ANDROIDES"
private const val ANCHOR_MESSAGE =
    "\n\n" +
        "--- \n" +
        "\n" +
        "Send in a voice message: https://anchor.fm/losandroides/message"
private const val IVOOX_URL = "https://www.ivoox.com/"
private const val EPISODE_AUDIO_LENGTH_DEFAULT_DURATION = 0

private const val HOURS_MINUTES_SECONDS_PATTERN = "HH:mm:ss"
private const val ONE_MINUTE_IN_SECONDS = 60
private const val ONE_HOUR_IN_SECONDS = 60 * ONE_MINUTE_IN_SECONDS

@VisibleForTesting
internal const val INVALID_EPISODE_NUMBER = "-1"

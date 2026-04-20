package soy.gabimoreno.data.remote.mapper.senioraudio

import com.prof18.rssparser.model.ItunesItemData
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.domain.model.content.SeniorAudio
import soy.gabimoreno.domain.model.content.SeniorAudiosWrapper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun RssChannel.toDomain(): SeniorAudiosWrapper {
    val numberOfSeniorAudios = items.size.toLong()
    val seniorAudiosTitle = title ?: SENIOR_AUDIOS_TITLE
    val seniorAudiosAuthor = seniorAudiosTitle.uppercase()

    return SeniorAudiosWrapper(
        count = numberOfSeniorAudios,
        total = numberOfSeniorAudios,
        seniorAudios =
            items.toDomain(
                seniorAudiosAuthor = seniorAudiosAuthor,
                seniorAudiosTitle = seniorAudiosTitle,
            ),
    )
}

fun List<RssItem>.toDomain(
    seniorAudiosAuthor: String,
    seniorAudiosTitle: String,
): List<SeniorAudio> =
    map { item ->
        item.toDomain(
            seniorAudiosAuthor = seniorAudiosAuthor,
            seniorAudiosTitle = seniorAudiosTitle,
        )
    }

fun RssItem.toDomain(
    seniorAudiosAuthor: String,
    seniorAudiosTitle: String,
): SeniorAudio {
    val cleanDescription = description.orEmpty()
    val imageUrl = getSeniorAudioCoverUrl()

    return SeniorAudio(
        id = getSeniorAudioId(),
        url = "https://gabimoreno.soy", // TODO: Put something if we need to
        audioUrl = requireNotNull(audio),
        imageUrl = imageUrl,
        saga =
            Saga(
                author = seniorAudiosAuthor,
                title = seniorAudiosTitle,
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

private fun RssItem.getSeniorAudioId(): String =
    requireNotNull(guid)
        .removePrefix(SPREAKER_URL)

private fun RssItem.getSeniorAudioCoverUrl(): String = itunesItemData?.image.orEmpty()

private fun ItunesItemData?.getAudioLengthInSeconds(): Int {
    val duration = this?.duration ?: return SENIOR_AUDIO_LENGTH_DEFAULT_DURATION
    return duration.toIntOrNull() ?: parseTimeFormatDuration(duration)
}

private fun parseTimeFormatDuration(duration: String): Int {
    val dateFormat = SimpleDateFormat(HOURS_MINUTES_SECONDS_PATTERN, Locale.ROOT)
    val date = dateFormat.parse(duration) ?: return SENIOR_AUDIO_LENGTH_DEFAULT_DURATION

    return Calendar.getInstance().apply { time = date }.let { calendar ->
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        hours * ONE_HOUR_IN_SECONDS + minutes * ONE_MINUTE_IN_SECONDS + seconds
    }
}

private val rssDateFormat =
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

private const val SENIOR_AUDIOS_TITLE = "Audios Senior"
private const val SPREAKER_URL = "https://api.spreaker.com/episode/"
private const val SENIOR_AUDIO_LENGTH_DEFAULT_DURATION = 0

private const val HOURS_MINUTES_SECONDS_PATTERN = "HH:mm:ss"
private const val ONE_MINUTE_IN_SECONDS = 60
private const val ONE_HOUR_IN_SECONDS = 60 * ONE_MINUTE_IN_SECONDS

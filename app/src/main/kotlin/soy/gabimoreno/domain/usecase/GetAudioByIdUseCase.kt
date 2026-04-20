package soy.gabimoreno.domain.usecase

import arrow.core.Either
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.domain.repository.audiocourse.AudioCourseRepository
import soy.gabimoreno.domain.repository.podcast.PodcastRepository
import soy.gabimoreno.domain.repository.premiumaudio.PremiumAudiosRepository
import soy.gabimoreno.domain.repository.senioraudio.SeniorAudioRepository
import soy.gabimoreno.domain.util.AudioItemType
import soy.gabimoreno.domain.util.audioItemTypeDetector
import javax.inject.Inject

class GetAudioByIdUseCase
    @Inject
    constructor(
        private val audioCourseRepository: AudioCourseRepository,
        private val podcastRepository: PodcastRepository,
        private val seniorAudioRepository: SeniorAudioRepository,
        private val premiumAudiosRepository: PremiumAudiosRepository,
    ) {
        suspend operator fun invoke(audioId: String): Either<Throwable, Audio> =
            when (audioItemTypeDetector(audioId)) {
                AudioItemType.AUDIO_COURSE -> {
                    audioCourseRepository.getAudioCourseItemById(audioId)
                }

                AudioItemType.PREMIUM_AUDIO -> {
                    premiumAudiosRepository.getPremiumAudioById(audioId)
                }

                AudioItemType.PODCAST -> {
                    podcastRepository.getPodcastById(audioId)
                }

                AudioItemType.SENIOR_AUDIO -> {
                    seniorAudioRepository.getSeniorAudioById(audioId)
                }
            }
    }

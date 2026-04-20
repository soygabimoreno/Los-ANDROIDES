package soy.gabimoreno.presentation.screen.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.insets.navigationBarsPadding
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.data.remote.mapper.EMPTY_AUDIO_LENGTH_IN_SECONDS
import soy.gabimoreno.data.remote.model.Category
import soy.gabimoreno.data.tracker.domain.toPlayPause
import soy.gabimoreno.domain.model.content.PremiumAudio
import soy.gabimoreno.domain.model.podcast.Episode
import soy.gabimoreno.framework.parseFromHtmlFormat
import soy.gabimoreno.presentation.navigation.Feature
import soy.gabimoreno.presentation.screen.ViewModelProvider
import soy.gabimoreno.presentation.screen.home.HomeViewModel
import soy.gabimoreno.presentation.screen.senioraudios.SeniorAudiosViewModel
import soy.gabimoreno.presentation.theme.PinkBright
import soy.gabimoreno.presentation.theme.Spacing
import soy.gabimoreno.presentation.theme.White
import soy.gabimoreno.presentation.ui.AudioImage
import soy.gabimoreno.presentation.ui.BackButton
import soy.gabimoreno.presentation.ui.EmphasisText
import soy.gabimoreno.presentation.ui.SelectableEmphasisText
import soy.gabimoreno.presentation.ui.button.AnimatedIconButton
import soy.gabimoreno.presentation.ui.button.PrimaryButton
import soy.gabimoreno.util.formatMillisecondsAsDate
import soy.gabimoreno.util.toDurationMinutes

@Composable
fun DetailScreen(
    audioId: String,
    feature: Feature,
    onAddToPlaylistClick: (audioId: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val currentContext = LocalContext.current
    val scrollState = rememberScrollState()
    val homeViewModel = ViewModelProvider.homeViewModel
    val seniorViewModel = ViewModelProvider.seniorAudiosViewModel
    val detailViewModel = ViewModelProvider.detailViewModel
    val playerViewModel = ViewModelProvider.playerViewModel
    val premiumViewModel = ViewModelProvider.premiumAudiosViewModel

    var audios: List<Audio>? = null
    var audio: Audio? = null
    when (feature) {
        Feature.PODCAST -> {
            val viewState: HomeViewModel.ViewState = homeViewModel.viewState
            if (viewState is HomeViewModel.ViewState.Success) {
                audios = viewState.episodes
                audio = homeViewModel.findEpisodeFromId(audioId)
            }
        }

        Feature.SENIOR_AUDIOS -> {
            val viewState = seniorViewModel.viewState
            if (viewState is SeniorAudiosViewModel.ViewState.Success) {
                audios = viewState.seniorAudios
                audio = seniorViewModel.findSeniorAudioFromId(audioId)
            }
        }

        Feature.PREMIUM_AUDIOS -> {
            val viewState by premiumViewModel.state.collectAsStateWithLifecycle()
            audios = viewState.premiumAudios
            premiumViewModel.state.value.selectedPremiumAudio?.let { premiumAudio ->
                audio = premiumAudio
            } ?: run {
                premiumViewModel.loadSelectedPremiumAudio(audioId)
                return
            }
        }

        else -> {
            Unit
        }
    }

    LaunchedEffect(Unit) {
        audio?.let {
            detailViewModel.onViewScreen(audio)
        }
    }

    Surface {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        ) {
            Row {
                BackButton {
                    audio?.let {
                        detailViewModel.onBackClick(audio)
                    }
                    onBackClick()
                }
            }

            if (audio != null) {
                val isPlaying =
                    playerViewModel.podcastIsPlaying &&
                        playerViewModel.currentPlayingAudio.value?.id == audio.id
                val playButtonText =
                    if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                val iconFavoriteColor by animateColorAsState(
                    targetValue =
                        if (detailViewModel.audioState?.markedAsFavorite ?: false) {
                            PinkBright
                        } else {
                            White.copy(alpha = 0.2f)
                        },
                    animationSpec = tween(durationMillis = CHANGE_COLOR_ANIMATION_DURATION),
                    label = "favoriteIconColorAnimation",
                )

                Column(
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .navigationBarsPadding()
                            .padding(vertical = Spacing.s24, horizontal = Spacing.s16)
                            .padding(
                                bottom =
                                    if (playerViewModel.currentPlayingAudio.value !=
                                        null
                                    ) {
                                        Spacing.s64
                                    } else {
                                        Spacing.s0
                                    },
                            ),
                ) {
                    AudioImage(
                        url = audio.imageUrl,
                        modifier = Modifier.height(Spacing.oddSpacing120),
                    )

                    Spacer(modifier = Modifier.height(Spacing.s32))

                    Text(
                        audio.title,
                        style = MaterialTheme.typography.h4,
                    )
                    Spacer(modifier = Modifier.height(Spacing.s24))

                    Text(
                        audio.saga.author,
                        style = MaterialTheme.typography.body1,
                    )

                    val dateAndDurationText =
                        if (audio.audioLengthInSeconds != EMPTY_AUDIO_LENGTH_IN_SECONDS) {
                            "${
                                audio.pubDateMillis.formatMillisecondsAsDate(
                                    "MMM dd",
                                )
                            } • ${audio.audioLengthInSeconds.toDurationMinutes()}"
                        } else {
                            audio.pubDateMillis.formatMillisecondsAsDate("MMM dd")
                        }
                    EmphasisText(
                        text = dateAndDurationText,
                    )

                    Spacer(modifier = Modifier.height(Spacing.s16))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PrimaryButton(
                            text = playButtonText,
                            height = Spacing.s48,
                        ) {
                            detailViewModel.onPlayPauseClick(audio, isPlaying.toPlayPause())
                            audios?.let {
                                playerViewModel.playPauseAudio(audios, audio)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (detailViewModel.audioState is PremiumAudio ||
                            detailViewModel.audioState is Episode
                        ) {
                            IconButton(
                                modifier = Modifier.size(Spacing.s32),
                                onClick = { detailViewModel.onFavoriteStatusChanged() },
                            ) {
                                Icon(
                                    modifier =
                                        Modifier
                                            .size(Spacing.s32),
                                    imageVector =
                                        if (detailViewModel.audioState!!.markedAsFavorite) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                    contentDescription =
                                        stringResource(
                                            R.string.audio_favorite,
                                        ),
                                    tint = iconFavoriteColor,
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.s16))
                            IconButton(
                                modifier = Modifier.size(Spacing.s32),
                                onClick = {
                                    onAddToPlaylistClick(
                                        detailViewModel.audioState?.id ?: "",
                                    )
                                },
                            ) {
                                Icon(
                                    modifier =
                                        Modifier
                                            .size(Spacing.s32),
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription =
                                        stringResource(
                                            R.string.playlists_add_audio_to_playlist,
                                        ),
                                    tint = White,
                                )
                            }
                        }

                        AnimatedIconButton(
                            showAnimation = (
                                playerViewModel.currentPlayingAudio.value?.id == audio.id &&
                                    playerViewModel.hasTriggeredEightyPercent
                            ),
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = White,
                            padding = Spacing.s16,
                        ) {
                            detailViewModel.onShareClick(currentContext, audio)
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.s16))
                    if (audio is PremiumAudio) {
                        SelectableEmphasisText(
                            text = audio.excerpt.parseFromHtmlFormat(),
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.s16))
                    if (audio !is PremiumAudio ||
                        audio.category != Category.PREMIUM_AUDIO_COURSES
                    ) {
                        SelectableEmphasisText(
                            text = audio.description.parseFromHtmlFormat(),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private const val CHANGE_COLOR_ANIMATION_DURATION = 300

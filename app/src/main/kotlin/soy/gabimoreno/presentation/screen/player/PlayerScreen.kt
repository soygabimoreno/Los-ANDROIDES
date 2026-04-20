@file:OptIn(ExperimentalMaterialApi::class)

package soy.gabimoreno.presentation.screen.player

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults.IndicatorBackgroundOpacity
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.SwipeableState
import androidx.compose.material.Text
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.insets.systemBarsPadding
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.tracker.domain.toPlayPause
import soy.gabimoreno.domain.model.podcast.Episode
import soy.gabimoreno.presentation.screen.ViewModelProvider
import soy.gabimoreno.presentation.theme.Percent
import soy.gabimoreno.presentation.theme.PurpleDark
import soy.gabimoreno.presentation.theme.Spacing
import soy.gabimoreno.presentation.theme.White
import soy.gabimoreno.presentation.ui.ArrowDownButton
import soy.gabimoreno.presentation.ui.EmphasisText
import soy.gabimoreno.presentation.ui.PreviewContent
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(backDispatcher: OnBackPressedDispatcher) {
    val playerViewModel = ViewModelProvider.playerViewModel
    val audio = playerViewModel.currentPlayingAudio.value

    val showPlayerFullScreen = playerViewModel.showPlayerFullScreen
    if (showPlayerFullScreen) {
        audio?.let {
            playerViewModel.onViewScreen(audio)
        }
    }

    AnimatedVisibility(
        visible = audio != null && showPlayerFullScreen,
        enter =
            slideInVertically(
                initialOffsetY = { it },
            ),
        exit =
            slideOutVertically(
                targetOffsetY = { it },
            ),
    ) {
        if (audio != null) {
            PodcastPlayerBody(audio, backDispatcher)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PodcastPlayerBody(
    audio: Audio,
    backDispatcher: OnBackPressedDispatcher,
) {
    val playerViewModel = ViewModelProvider.playerViewModel
    val swipeableState = rememberSwipeableState(0)
    val anchors = rememberSwipeAnchors()
    val backCallback = rememberBackCallback(playerViewModel)

    var sliderIsChanging by remember { mutableStateOf(false) }
    var localSliderValue by remember { mutableFloatStateOf(0f) }
    val sliderProgress =
        if (sliderIsChanging) localSliderValue else playerViewModel.currentAudioProgress

    HandleSwipeDismiss(swipeableState) {
        playerViewModel.showPlayerFullScreen = false
    }

    PodcastPlayerStatelessContent(
        audio = audio,
        gradientColor = MaterialTheme.colors.background,
        yOffset = swipeableState.offset.value.roundToInt(),
        playPauseIcon = playerViewModel.podcastIsPlaying.toIcon(),
        playbackProgress = sliderProgress,
        currentTime = playerViewModel.getCurrentPlaybackFormattedPosition(),
        totalTime = playerViewModel.currentAudioFormattedDuration,
        onRewind = { playerViewModel.onRewindClick() },
        onForward = { playerViewModel.onForwardClick() },
        onSkipToPrevious = { playerViewModel.onSkipToPrevious() },
        onSkipToNext = { playerViewModel.onSkipToNext() },
        onTooglePlayback = {
            playerViewModel.onPlayPauseClickFromPlayer(
                audio,
                playerViewModel.podcastIsPlaying.toPlayPause(),
            )
            playerViewModel.togglePlaybackState()
        },
        onSliderChange = {
            localSliderValue = it
            sliderIsChanging = true
        },
        onSliderChangeFinished = {
            playerViewModel.onSliderChangeFinished(localSliderValue)
            sliderIsChanging = false
        },
        shouldIShowSpeedControls = playerViewModel.shouldIShowSpeedControls,
        selectedPlaybackSpeed = playerViewModel.selectedPlaybackSpeed,
        onSpeedControlClick = { playerViewModel.onSpeedControlClick() },
        onSetPlaybackSpeed = { playerViewModel.onSetPlaybackSpeed(it) },
    ) { playerViewModel.showPlayerFullScreen = false }

    HandleSideEffects(backDispatcher, backCallback, playerViewModel)
}

@Composable
private fun rememberSwipeAnchors(): Map<Float, Int> {
    val endAnchor = LocalConfiguration.current.screenHeightDp * LocalDensity.current.density
    return remember(endAnchor) {
        mapOf(0f to 0, endAnchor to 1)
    }
}

@Composable
private fun rememberBackCallback(viewModel: PlayerViewModel): OnBackPressedCallback =
    remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.showPlayerFullScreen = false
            }
        }
    }

@Composable
private fun HandleSwipeDismiss(
    swipeableState: SwipeableState<Int>,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .swipeable(
                    state = swipeableState,
                    anchors = rememberSwipeAnchors(),
                    thresholds = { _, _ -> FractionalThreshold(Percent.THIRTY_FIVE) },
                    orientation = Orientation.Vertical,
                ),
    ) {
        if (swipeableState.currentValue >= 1) {
            LaunchedEffect(Unit) { onDismiss() }
        }
    }
}

@Composable
private fun HandleSideEffects(
    backDispatcher: OnBackPressedDispatcher,
    backCallback: OnBackPressedCallback,
    viewModel: PlayerViewModel,
) {
    LaunchedEffect("playbackPosition") {
        viewModel.updateCurrentPlaybackPosition()
    }

    DisposableEffect(backDispatcher) {
        backDispatcher.addCallback(backCallback)
        onDispose {
            backCallback.remove()
            viewModel.showPlayerFullScreen = false
        }
    }
}

private fun Boolean.toIcon(): Int =
    if (this) {
        R.drawable.ic_baseline_pause_24
    } else {
        R.drawable.ic_baseline_play_arrow_24
    }

@Composable
fun PodcastPlayerStatelessContent(
    audio: Audio,
    gradientColor: Color,
    yOffset: Int,
    @DrawableRes playPauseIcon: Int,
    playbackProgress: Float,
    currentTime: String,
    totalTime: String,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onTooglePlayback: () -> Unit,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    shouldIShowSpeedControls: Boolean,
    selectedPlaybackSpeed: PlaybackSpeed,
    onSpeedControlClick: () -> Unit,
    onSetPlaybackSpeed: (PlaybackSpeed) -> Unit,
    onClose: () -> Unit,
) {
    PodcastPlayerBackground(gradientColor, yOffset) {
        PodcastPlayerHeader(onClose)
        Column(modifier = Modifier.padding(horizontal = Spacing.s24)) {
            PodcastPlayerImage(
                audio = audio,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .align(Alignment.CenterHorizontally),
                shouldIShowSpeedControls = shouldIShowSpeedControls,
                selectedPlaybackSpeed = selectedPlaybackSpeed,
                onSpeedControlClick = onSpeedControlClick,
                onSetPlaybackSpeed = onSetPlaybackSpeed,
            )
            PodcastPlayerTitle(audio)
            PodcastPlayerSlider(
                playbackProgress,
                currentTime,
                totalTime,
                onSliderChange,
                onSliderChangeFinished,
            )
            PodcastPlayerControls(
                playPauseIcon,
                onRewind,
                onForward,
                onSkipToPrevious,
                onSkipToNext,
                onTooglePlayback,
            )
        }
    }
}

@Composable
private fun PodcastPlayerBackground(
    gradientColor: Color,
    yOffset: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val gradientColors = listOf(gradientColor, MaterialTheme.colors.background)
    val endY =
        LocalConfiguration.current.screenHeightDp.toFloat() * LocalDensity.current.density / 2

    Box(
        modifier =
            Modifier
                .offset { IntOffset(0, yOffset) }
                .fillMaxSize(),
    ) {
        Surface {
            Box(
                modifier =
                    Modifier
                        .background(Brush.verticalGradient(gradientColors, endY = endY))
                        .fillMaxSize()
                        .systemBarsPadding(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                            ),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun PodcastPlayerHeader(onClose: () -> Unit) {
    ArrowDownButton(onClick = onClose)
}

@Composable
private fun PodcastPlayerImage(
    audio: Audio,
    modifier: Modifier = Modifier,
    shouldIShowSpeedControls: Boolean,
    selectedPlaybackSpeed: PlaybackSpeed,
    onSpeedControlClick: () -> Unit,
    onSetPlaybackSpeed: (PlaybackSpeed) -> Unit,
) {
    Box(
        modifier =
            modifier
                .padding(vertical = Spacing.s32)
                .clip(MaterialTheme.shapes.medium)
                .aspectRatio(1f)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.08f)),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(audio.imageUrl)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
            contentDescription = stringResource(R.string.podcast_thumbnail),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_baseline_mic_24),
            modifier = Modifier.fillMaxSize(),
        )
        PlaybackSpeedSelector(
            shouldIShowSpeedControls = shouldIShowSpeedControls,
            selectedPlaybackSpeed = selectedPlaybackSpeed,
            onSpeedControlClick = onSpeedControlClick,
            onSetPlaybackSpeed = onSetPlaybackSpeed,
        )
    }
}

@Composable
private fun BoxScope.PlaybackSpeedSelector(
    shouldIShowSpeedControls: Boolean,
    selectedPlaybackSpeed: PlaybackSpeed,
    onSpeedControlClick: () -> Unit,
    onSetPlaybackSpeed: (PlaybackSpeed) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(IntrinsicSize.Max)
                .background(
                    White,
                    shape = RoundedCornerShape(topStart = Spacing.s8, bottomEnd = Spacing.s8),
                )
                .animateContentSize()
                .align(Alignment.BottomEnd)
                .clickable {
                    onSpeedControlClick()
                },
    ) {
        if (!shouldIShowSpeedControls) {
            Text(
                selectedPlaybackSpeed.label,
                fontWeight = FontWeight.Black,
                color = PurpleDark,
                modifier =
                    Modifier
                        .padding(Spacing.s8)
                        .clickable {
                            onSpeedControlClick()
                        },
            )
        } else {
            Column {
                PlaybackSpeed.entries.reversed().forEach { speed ->
                    Text(
                        speed.label,
                        fontWeight = FontWeight.Black,
                        color = PurpleDark,
                        modifier =
                            Modifier
                                .padding(Spacing.s8)
                                .clickable {
                                    onSetPlaybackSpeed(speed)
                                },
                    )
                    Box(
                        modifier =
                            Modifier
                                .height(Spacing.s1)
                                .fillMaxWidth()
                                .background(PurpleDark),
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastPlayerTitle(audio: Audio) {
    Text(
        audio.title,
        style = MaterialTheme.typography.h5,
        color = MaterialTheme.colors.onBackground,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )

    Text(
        audio.saga.title,
        style = MaterialTheme.typography.subtitle1,
        color = MaterialTheme.colors.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.graphicsLayer { alpha = Percent.SIXTY },
    )
}

@Composable
private fun PodcastPlayerSlider(
    playbackProgress: Float,
    currentTime: String,
    totalTime: String,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
) {
    val sliderColors =
        SliderDefaults.colors(
            thumbColor = MaterialTheme.colors.onBackground,
            activeTrackColor = MaterialTheme.colors.onBackground,
            inactiveTrackColor =
                MaterialTheme.colors.onBackground.copy(
                    alpha = IndicatorBackgroundOpacity,
                ),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.s24),
    ) {
        Slider(
            value = playbackProgress,
            onValueChange = onSliderChange,
            onValueChangeFinished = onSliderChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = sliderColors,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmphasisText(text = currentTime)
            EmphasisText(text = totalTime)
        }
    }
}

@Composable
private fun PodcastPlayerControls(
    @DrawableRes playPauseIcon: Int,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onTooglePlayback: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s8, bottom = Spacing.s32),
    ) {
        ControlIcon(
            R.drawable.ic_baseline_skip_previous_24,
            R.string.skip_to_previous,
            onSkipToPrevious,
        )
        ControlIcon(R.drawable.ic_baseline_replay_10_24, R.string.replay_10_seconds, onRewind)
        Icon(
            painter = painterResource(playPauseIcon),
            contentDescription = stringResource(R.string.play),
            tint = MaterialTheme.colors.background,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onBackground)
                    .clickable(onClick = onTooglePlayback)
                    .size(Spacing.s64)
                    .padding(Spacing.s8),
        )
        ControlIcon(
            R.drawable.ic_baseline_forward_10_24,
            R.string.forward_10_seconds,
            onForward,
        )
        ControlIcon(R.drawable.ic_baseline_skip_next_24, R.string.skip_to_next, onSkipToNext)
    }
}

@Composable
private fun ControlIcon(
    @DrawableRes iconRes: Int,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(contentDescription),
        modifier =
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(Spacing.oddSpacing12)
                .size(Spacing.s32),
    )
}

@Preview(name = "Player")
@Composable
fun PodcastPlayerPreview() {
    PreviewContent {
        PodcastPlayerStatelessContent(
            audio =
                Episode(
                    id = "1",
                    url = "",
                    audioUrl = "",
                    imageUrl = "",
                    saga = Saga("This is publisher", "This is podcast title"),
                    thumbnailUrl = "",
                    pubDateMillis = 0,
                    title = "This is a title",
                    audioLengthInSeconds = 2700,
                    description = "This is a description",
                    hasBeenListened = false,
                    markedAsFavorite = false,
                ),
            gradientColor = Color.DarkGray,
            yOffset = 0,
            playPauseIcon = R.drawable.ic_baseline_play_arrow_24,
            playbackProgress = 0f,
            currentTime = "0:00",
            totalTime = "10:00",
            onClose = { },
            onForward = { },
            onSkipToPrevious = {},
            onSkipToNext = {},
            onRewind = { },
            onTooglePlayback = { },
            onSliderChange = { },
            onSliderChangeFinished = { },
            shouldIShowSpeedControls = true,
            selectedPlaybackSpeed = PlaybackSpeed.SPEED_1X,
            onSpeedControlClick = { },
            onSetPlaybackSpeed = { },
        )
    }
}

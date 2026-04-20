package soy.gabimoreno.presentation.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.tracker.domain.toPlayPause
import soy.gabimoreno.domain.model.podcast.Episode
import soy.gabimoreno.presentation.screen.ViewModelProvider
import soy.gabimoreno.presentation.theme.Percent
import soy.gabimoreno.presentation.theme.PurpleLight
import soy.gabimoreno.presentation.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun AudioBottomBar(modifier: Modifier = Modifier) {
    val audio = ViewModelProvider.playerViewModel.currentPlayingAudio.value

    AnimatedVisibility(
        visible = audio != null,
        modifier = modifier,
    ) {
        if (audio != null) {
            AudioBottomBarContent(audio)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AudioBottomBarContent(audio: Audio) {
    val swipeableState = rememberSwipeableState(INITIAL_VALUE)
    val playerViewModel = ViewModelProvider.playerViewModel

    val endAnchor = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    val anchors =
        mapOf(
            0f to 0,
            endAnchor to 1,
        )

    val isPlaying = playerViewModel.podcastIsPlaying
    val iconResId =
        if (isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(Percent.FIFTY_FIVE) },
                    orientation = Orientation.Horizontal,
                ),
    ) {
        if (swipeableState.currentValue >= 1) {
            LaunchedEffect("key") {
                playerViewModel.onAudioBottomBarSwiped()
                playerViewModel.stopPlayback()
            }
        }

        AudioBottomBarStatelessContent(
            audio = audio,
            xOffset = swipeableState.offset.value.roundToInt(),
            icon = iconResId,
            onTogglePlaybackState = {
                playerViewModel.onPlayPauseClickFromAudioBottomBar(
                    audio,
                    isPlaying.toPlayPause(),
                )
                playerViewModel.togglePlaybackState()
            },
        ) {
            playerViewModel.showPlayerFullScreen = true
        }
    }
}

@Composable
fun AudioBottomBarStatelessContent(
    audio: Audio,
    xOffset: Int,
    @DrawableRes icon: Int,
    onTogglePlaybackState: () -> Unit,
    onTap: (Offset) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .offset { IntOffset(xOffset, 0) }
                .background(PurpleLight)
                .height(Spacing.s64)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = onTap,
                    )
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioImage(audioThumbnailUrl = audio.thumbnailUrl)
            Column(
                verticalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .weight(Percent.ONE_HUNDRED)
                        .fillMaxHeight()
                        .padding(Spacing.s8),
            ) {
                Text(
                    audio.title,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    audio.saga.title,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = Percent.SIXTY_FIVE
                        },
                )
            }
            PlayIcon(
                icon = icon,
                onTogglePlaybackState = onTogglePlaybackState,
            )
        }
    }
}

@Composable
private fun AudioImage(audioThumbnailUrl: String) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(audioThumbnailUrl)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
        contentDescription = stringResource(R.string.podcast_thumbnail),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(Spacing.s64),
        error = painterResource(R.drawable.ic_baseline_mic_24),
    )
}

@Composable
private fun PlayIcon(
    @DrawableRes icon: Int,
    onTogglePlaybackState: () -> Unit,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(R.string.play),
        tint = MaterialTheme.colors.onBackground,
        modifier =
            Modifier
                .padding(end = Spacing.s8)
                .size(Spacing.s40)
                .clip(CircleShape)
                .clickable(onClick = onTogglePlaybackState)
                .padding(Spacing.oddSpacing6),
    )
}

@Preview(name = "Bottom Bar")
@Composable
fun AudioBottomBarPreview() {
    PreviewContent {
        AudioBottomBarStatelessContent(
            audio =
                Episode(
                    id = "1",
                    url = "",
                    audioUrl = "",
                    imageUrl = "https://picsum.photos/200",
                    saga = Saga("This is publisher", "This is podcast title"),
                    thumbnailUrl = "https://picsum.photos/200",
                    pubDateMillis = 0,
                    title = "This is a title",
                    audioLengthInSeconds = 2700,
                    description = "This is a description",
                    hasBeenListened = false,
                    markedAsFavorite = false,
                ),
            xOffset = 0,
            icon = R.drawable.ic_baseline_play_arrow_24,
            onTogglePlaybackState = { },
            onTap = { },
        )
    }
}

private const val INITIAL_VALUE = 0

package soy.gabimoreno.presentation.screen.home.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.domain.model.podcast.Episode
import soy.gabimoreno.presentation.theme.GabiMorenoTheme
import soy.gabimoreno.presentation.theme.Orange
import soy.gabimoreno.presentation.theme.Percent
import soy.gabimoreno.presentation.theme.PinkBright
import soy.gabimoreno.presentation.theme.Spacing
import soy.gabimoreno.presentation.theme.White
import soy.gabimoreno.presentation.ui.AudioImage

@Composable
fun EpisodeView(
    episode: Episode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFavoriteStatusChanged: (episode: Episode) -> Unit,
    onListenedToggled: (episode: Episode) -> Unit,
    onAddToPlaylistClick: (episodeId: String) -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colors.background)
                .clickable(onClick = onClick),
    ) {
        AudioImage(
            url = episode.thumbnailUrl,
            aspectRatio = 1f,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            ListenedIcon(
                episode = episode,
                onListenedToggled = { onListenedToggled(episode) },
            )
            FavoriteIcon(
                episode = episode,
                onFavoriteStatusChanged = { onFavoriteStatusChanged(episode) },
            )
            PlaylistIcon(
                episodeId = episode.id,
                onAddToPlaylistClick = { onAddToPlaylistClick(episode.id) },
            )
        }
        Text(
            episode.title,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(horizontal = Spacing.s8),
        )
    }
}

@Composable
private fun PlaylistIcon(
    episodeId: String,
    onAddToPlaylistClick: (episodeId: String) -> Unit,
) {
    IconButton(
        onClick = { onAddToPlaylistClick(episodeId) },
    ) {
        Icon(
            modifier =
                Modifier
                    .size(Spacing.s32),
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = stringResource(R.string.playlists_add_audio_to_playlist),
            tint = White,
        )
    }
}

@Composable
private fun FavoriteIcon(
    episode: Episode,
    onFavoriteStatusChanged: (episode: Episode) -> Unit,
) {
    val iconFavoriteColor by animateColorAsState(
        targetValue =
            if (episode.markedAsFavorite) {
                PinkBright
            } else {
                White.copy(alpha = Percent.TWENTY)
            },
        animationSpec = tween(durationMillis = CHANGE_COLOR_ANIMATION_DURATION),
        label = "favoriteIconColorAnimation",
    )
    IconButton(
        onClick = { onFavoriteStatusChanged(episode) },
    ) {
        Icon(
            modifier =
                Modifier
                    .size(Spacing.s32),
            imageVector =
                if (episode.markedAsFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
            contentDescription = stringResource(R.string.audio_favorite),
            tint = iconFavoriteColor,
        )
    }
}

@Composable
private fun ListenedIcon(
    episode: Episode,
    onListenedToggled: (episode: Episode) -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue =
            if (episode.hasBeenListened) {
                Orange
            } else {
                White.copy(alpha = Percent.TWENTY)
            },
        animationSpec = tween(durationMillis = CHANGE_COLOR_ANIMATION_DURATION),
        label = "checkIconColorAnimation",
    )
    IconButton(
        onClick = { onListenedToggled(episode) },
    ) {
        Icon(
            modifier =
                Modifier
                    .size(Spacing.s32),
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.podcast_audio_listened),
            tint = iconColor,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    GabiMorenoTheme {
        EpisodeView(
            episode =
                Episode(
                    id = "id",
                    url = "",
                    audioUrl = "",
                    imageUrl = "",
                    saga = Saga(author = "This is publisher", title = "This is saga title"),
                    thumbnailUrl = "",
                    pubDateMillis = 0,
                    title = "This is a title",
                    audioLengthInSeconds = 2700,
                    description = "This is a description",
                    hasBeenListened = true,
                    markedAsFavorite = true,
                ),
            onClick = {},
            onFavoriteStatusChanged = {},
            onListenedToggled = {},
            onAddToPlaylistClick = {},
        )
    }
}

private const val CHANGE_COLOR_ANIMATION_DURATION = 300

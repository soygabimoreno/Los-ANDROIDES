package soy.gabimoreno.presentation.screen.playlist.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Audio
import soy.gabimoreno.domain.model.content.Playlist
import soy.gabimoreno.framework.toast
import soy.gabimoreno.presentation.screen.ViewModelProvider
import soy.gabimoreno.presentation.screen.playlist.detail.view.ReorderablePlaylistItemColumn
import soy.gabimoreno.presentation.screen.playlist.view.PlaylistDialog
import soy.gabimoreno.presentation.theme.GabiMorenoTheme
import soy.gabimoreno.presentation.theme.Orange
import soy.gabimoreno.presentation.theme.PurpleDark
import soy.gabimoreno.presentation.theme.PurpleLight
import soy.gabimoreno.presentation.theme.Spacing
import soy.gabimoreno.presentation.theme.White
import soy.gabimoreno.presentation.ui.BackButton
import soy.gabimoreno.presentation.ui.dialog.CustomDialog
import soy.gabimoreno.presentation.ui.dialog.TypeDialog

@Composable
fun PlaylistDetailScreenRoot(
    playlistId: String,
    onBackClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val unexpectedErrorMessage = stringResource(R.string.unexpected_error)
    val playerViewModel = ViewModelProvider.playerViewModel
    LaunchedEffect(Unit) {
        viewModel.onScreenView(playlistId.toInt())
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlaylistDetailEvent.Error -> {
                    context.toast(unexpectedErrorMessage)
                }

                PlaylistDetailEvent.PlayAudio -> {
                    if (viewModel.state.playlistAudioItems.isNotEmpty()) {
                        playerViewModel.playPauseAudio(
                            viewModel.state.playlistAudioItems,
                            viewModel.state.audio as Audio,
                        )
                    }
                }
            }
        }
    }

    PlaylistDetailScreen(
        state = viewModel.state,
        onAction = { action ->
            when (action) {
                is PlaylistDetailAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
fun PlaylistDetailScreen(
    state: PlaylistDetailState,
    onAction: (PlaylistDetailAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PurpleLight),
    ) {
        when {
            state.playlist != null && !state.isLoading -> {
                PlaylistDetailHeader(state, onAction)

                if (state.playlistAudioItems.isNotEmpty()) {
                    ReorderablePlaylistItemColumn(
                        playlistAudioItems = state.playlistAudioItems,
                        onItemClick = { onAction(PlaylistDetailAction.OnAudioItemClick(it)) },
                        onDragFinish = { onAction(PlaylistDetailAction.OnAudioItemsReordered(it)) },
                        onRemoveClick = {
                            onAction(
                                PlaylistDetailAction.OnRemovePlaylistAudioItem(
                                    it.id,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PlaylistEmptyState()
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }

    PlaylistDialogs(state = state, onAction = onAction)
}

@Composable
private fun PlaylistDetailHeader(
    state: PlaylistDetailState,
    onAction: (PlaylistDetailAction) -> Unit,
) {
    Box(contentAlignment = Alignment.TopStart) {
        Image(
            painter = painterResource(R.drawable.ic_audio_courses_header),
            contentDescription = stringResource(R.string.playlist_detail_header),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
        )

        BackButton(
            onClick = { onAction(PlaylistDetailAction.OnBackClick) },
            modifier =
                Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                    )
                    .align(Alignment.TopStart),
        )

        if (state.playlistAudioItems.isNotEmpty()) {
            IconOverlay(
                icon = Icons.Default.PlayCircleOutline,
                contentDescription = stringResource(R.string.play),
                onClick = { onAction(PlaylistDetailAction.OnPlayClick) },
                modifier = Modifier.align(Alignment.TopCenter),
                size = Spacing.s96,
            )
        }

        IconOverlay(
            icon = Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.playlist_edit),
            onClick = { onAction(PlaylistDetailAction.OnEditPlaylistClick) },
            modifier = Modifier.align(Alignment.TopEnd),
            size = Spacing.s48,
        )

        PlaylistHeaderText(
            state,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun IconOverlay(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    size: Dp = Spacing.s64,
) {
    Box(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .clip(CircleShape)
                .background(PurpleDark.copy(alpha = 0.8f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Orange,
            modifier =
                Modifier
                    .size(size)
                    .padding(Spacing.s8)
                    .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun PlaylistHeaderText(
    state: PlaylistDetailState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(PurpleDark.copy(alpha = 0.8f))
                .padding(Spacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.playlist?.let {
            Text(
                text = it.title,
                color = White,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        state.playlist?.let {
            Text(
                text = it.description,
                color = White,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.W300,
            )
        }
    }
}

@Composable
private fun PlaylistEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.playlist_playlist_empty),
            style = MaterialTheme.typography.h6,
        )
        Text(
            text = stringResource(R.string.playlist_add_items),
            style = MaterialTheme.typography.h6,
            color = Orange,
        )
    }
}

@Composable
private fun PlaylistDialogs(
    state: PlaylistDetailState,
    onAction: (PlaylistDetailAction) -> Unit,
) {
    when {
        state.shouldIShowDialog && state.dialogType is PlaylistDialogType.Delete -> {
            CustomDialog(
                title = stringResource(R.string.profile_reset_dialog_title),
                text = stringResource(R.string.playlist_delete_confirmation),
                confirmText = stringResource(R.string.playlists_remove),
                dismissText = stringResource(R.string.close),
                onConfirm = { onAction(PlaylistDetailAction.OnConfirmDialog) },
                onDismiss = { onAction(PlaylistDetailAction.OnDismissDialog) },
                typeDialog = TypeDialog.CONFIRMATION,
            )
        }

        state.shouldIShowDialog && state.dialogType is PlaylistDialogType.Edit -> {
            PlaylistDialog(
                titleDialog = stringResource(id = R.string.playlists_update_playlist),
                title = state.dialogTitle,
                titleError = state.dialogTitleError,
                description = state.dialogDescription,
                onTitleChange = { onAction(PlaylistDetailAction.OnDialogTitleChange(it)) },
                onDescriptionChange = {
                    onAction(
                        PlaylistDetailAction.OnDialogDescriptionChange(it),
                    )
                },
                onConfirm = { onAction(PlaylistDetailAction.OnEditPlaylistConfirmDialog) },
                onDismiss = { onAction(PlaylistDetailAction.OnEditPlaylistDismissDialog) },
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GabiMorenoTheme {
        PlaylistDetailScreen(
            state =
                PlaylistDetailState(
                    playlist =
                        Playlist(
                            id = 1,
                            title = "Audiocurso de CI/CD",
                            description = "Domina con este curso CI/CD para AHORRAR TIEMPO",
                            position = 0,
                            items = emptyList(),
                        ),
                ),
            onAction = {},
        )
    }
}

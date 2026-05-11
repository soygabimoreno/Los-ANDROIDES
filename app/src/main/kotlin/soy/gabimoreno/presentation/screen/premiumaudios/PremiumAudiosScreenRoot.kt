@file:OptIn(ExperimentalMaterialApi::class)

package soy.gabimoreno.presentation.screen.premiumaudios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import soy.gabimoreno.R
import soy.gabimoreno.coredomain.model.audio.Saga
import soy.gabimoreno.data.remote.model.Category
import soy.gabimoreno.domain.model.content.PremiumAudio
import soy.gabimoreno.framework.datastore.isMemberActive
import soy.gabimoreno.framework.toast
import soy.gabimoreno.presentation.screen.ViewModelProvider
import soy.gabimoreno.presentation.theme.Black
import soy.gabimoreno.presentation.theme.GabiMorenoTheme
import soy.gabimoreno.presentation.theme.Orange
import soy.gabimoreno.presentation.theme.Percent
import soy.gabimoreno.presentation.theme.PinkBright
import soy.gabimoreno.presentation.theme.PurpleLight
import soy.gabimoreno.presentation.theme.Spacing
import soy.gabimoreno.presentation.theme.White

@Composable
fun PremiumAudiosScreenRoot(
    onRequireAuth: () -> Unit,
    onItemClick: (premiumAudioId: String) -> Unit,
    onAddToPlaylistClick: (premiumAudioId: String) -> Unit,
    onPlaylistClick: () -> Unit,
) {
    val context = LocalContext.current
    val unexpectedErrorMessage = stringResource(R.string.unexpected_error)
    val premiumErrorTokenExpiredMessage = stringResource(R.string.premium_error_token_expired)
    val premiumViewModel = ViewModelProvider.premiumAudiosViewModel
    val state by premiumViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        launch {
            context
                .isMemberActive()
                .distinctUntilChanged()
                .collect { isMemberActive ->
                    premiumViewModel.onViewScreen(isMemberActive)
                }
        }
    }
    LaunchedEffect(Unit) {
        launch {
            premiumViewModel.events.collect { event ->
                when (event) {
                    is PremiumAudiosEvent.Error -> {
                        context.toast(unexpectedErrorMessage)
                    }

                    PremiumAudiosEvent.ShowTokenExpiredError -> {
                        context.toast(premiumErrorTokenExpiredMessage)
                    }

                    is PremiumAudiosEvent.ShowDetail -> {
                        onItemClick(event.premiumAudioId)
                    }
                }
            }
        }
    }

    PremiumScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is PremiumAudiosAction.OnLoginClick -> {
                    onRequireAuth()
                }

                is PremiumAudiosAction.OnPlaylistClick -> {
                    onPlaylistClick()
                }

                is PremiumAudiosAction.OnAddToPlaylistClick -> {
                    onAddToPlaylistClick(
                        action.premiumAudioId,
                    )
                }

                else -> {
                    Unit
                }
            }
            premiumViewModel.onAction(action)
        },
    )
}

@Composable
fun PremiumScreen(
    state: PremiumAudiosState,
    onAction: (PremiumAudiosAction) -> Unit,
) {
    val premiumAudiosFlow = state.premiumAudioFlow.collectAsLazyPagingItems()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.nav_item_premium_audios).uppercase(),
                style = MaterialTheme.typography.h5,
                modifier =
                    Modifier
                        .weight(Percent.EIGHTY),
            )
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = "Playlist",
                modifier =
                    Modifier
                        .padding(end = Spacing.s16)
                        .clickable {
                            onAction(PremiumAudiosAction.OnPlaylistClick)
                        },
            )
            Icon(
                imageVector =
                    if (state.shouldIAccessPremium) {
                        Icons.AutoMirrored.Filled.Logout
                    } else {
                        Icons.Default.Person
                    },
                contentDescription =
                    stringResource(
                        if (state.shouldIAccessPremium) {
                            R.string.premium_logout
                        } else {
                            R.string.premium_login
                        },
                    ),
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            onAction(PremiumAudiosAction.OnLoginClick)
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            if (state.shouldIAccessPremium) {
                PremiumContent(
                    premiumAudios = premiumAudiosFlow,
                    onItemClick = {
                        onAction(
                            PremiumAudiosAction.OnPremiumAudiosItemClick(it),
                        )
                    },
                    onListenedToggled = { onAction(PremiumAudiosAction.OnListenedToggled(it)) },
                    onPullRefreshTriggered = { onAction(PremiumAudiosAction.OnRefreshContent) },
                    onAddToPlaylistClick = {
                        onAction(
                            PremiumAudiosAction.OnAddToPlaylistClick(
                                it,
                            ),
                        )
                    },
                    onFavoriteStatusChanged = {
                        onAction(
                            PremiumAudiosAction.OnFavoriteStatusChanged(it),
                        )
                    },
                )
            } else {
                NonPremiumContent()
            }
            ShowLoading(state.isLoading)
        }
    }
}

@Composable
fun NonPremiumContent() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_premium_logo),
            contentDescription = stringResource(R.string.non_premium_image_cd),
            contentScale = ContentScale.FillWidth,
            modifier =
                Modifier
                    .width(350.dp),
        )
        Spacer()
        val annotatedLoginString =
            buildAnnotatedString {
                append(stringResource(R.string.non_premium_title))
                append(" ")
                withStyle(
                    SpanStyle(color = Orange, fontWeight = FontWeight.Bold),
                ) {
                    append(stringResource(R.string.non_premium_title_span))
                }
            }
        Text(
            text = annotatedLoginString,
            style = MaterialTheme.typography.h5,
        )
        Box(modifier = Modifier.weight(Percent.SIXTY))
        Text(
            stringResource(R.string.non_premium_text),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Justify,
        )
        Box(modifier = Modifier.weight(Percent.FIVE))
        TextButton(
            onClick = {
                uriHandler.openUri(URL_ANDROIDES_PREMIUM)
            },
        ) {
            Text(
                stringResource(R.string.non_premium_info_link),
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            )
        }
        Box(modifier = Modifier.weight(Percent.TWENTY_FIVE))
    }
}

@Composable
fun PremiumContent(
    premiumAudios: LazyPagingItems<PremiumAudio>,
    onItemClick: (premiumAudioId: String) -> Unit,
    onListenedToggled: (premiumAudio: PremiumAudio) -> Unit,
    onPullRefreshTriggered: () -> Unit,
    onAddToPlaylistClick: (premiumAudioId: String) -> Unit,
    onFavoriteStatusChanged: (premiumAudio: PremiumAudio) -> Unit,
) {
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    fun refresh() =
        refreshScope.launch {
            refreshing = true
            delay(REFRESH_DELAY)
            onPullRefreshTriggered()
            refreshing = false
        }

    val pullRefreshState = rememberPullRefreshState(refreshing, ::refresh)

    Box(
        Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        when {
            premiumAudios.loadState.refresh is LoadState.Loading ||
                premiumAudios.itemCount == 0 -> {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.Center),
                )
            }

            else -> {
                LazyColumn {
                    items(premiumAudios.itemCount) {
                        premiumAudios[it]?.let { premiumAudio ->
                            PremiumItem(
                                premiumAudio = premiumAudio,
                                onItemClick = onItemClick,
                                onListenedToggled = onListenedToggled,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onFavoriteStatusChanged = onFavoriteStatusChanged,
                            )
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
fun PremiumItem(
    premiumAudio: PremiumAudio,
    onItemClick: (premiumAudioId: String) -> Unit,
    onListenedToggled: (premiumAudio: PremiumAudio) -> Unit,
    onAddToPlaylistClick: (premiumAudioId: String) -> Unit,
    onFavoriteStatusChanged: (premiumAudio: PremiumAudio) -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue =
            if (premiumAudio.hasBeenListened) {
                Orange
            } else {
                Black.copy(alpha = Percent.TWENTY)
            },
        animationSpec = tween(durationMillis = CHANGE_COLOR_ANIMATION_DURATION),
        label = "checkIconColorAnimation",
    )
    val iconFavoriteColor by animateColorAsState(
        targetValue =
            if (premiumAudio.markedAsFavorite) {
                PinkBright
            } else {
                White.copy(alpha = Percent.TWENTY)
            },
        animationSpec = tween(durationMillis = CHANGE_COLOR_ANIMATION_DURATION),
        label = "favoriteIconColorAnimation",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onItemClick(premiumAudio.id)
                }.background(
                    brush =
                        run {
                            // TODO: This is a provisional patch for visualization
                            val backgroundColor =
                                if (premiumAudio.id == "5453") {
                                    Orange
                                } else {
                                    PurpleLight
                                }
                            SolidColor(backgroundColor)
                        },
                    alpha = 0.5f,
                ).padding(horizontal = Spacing.s16, vertical = Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Icon(
            imageVector = premiumAudio.category.icon,
            contentDescription = premiumAudio.category.title,
            modifier = Modifier.weight(Percent.EIGHT),
        )
        Spacer(modifier = Modifier.width(Spacing.s16))
        Text(
            text = premiumAudio.title,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .padding(end = Spacing.s16)
                    .weight(Percent.SIXTY_FIVE),
        )
        IconButton(
            modifier = Modifier.weight(Percent.EIGHT),
            onClick = { onListenedToggled(premiumAudio) },
        ) {
            Icon(
                modifier =
                    Modifier
                        .size(Spacing.s32),
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.premium_audio_listened),
                tint = iconColor,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.s16))
        IconButton(
            modifier = Modifier.weight(Percent.EIGHT),
            onClick = { onFavoriteStatusChanged(premiumAudio) },
        ) {
            Icon(
                modifier =
                    Modifier
                        .size(Spacing.s32),
                imageVector =
                    if (premiumAudio.markedAsFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                contentDescription = stringResource(R.string.audio_favorite),
                tint = iconFavoriteColor,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.s16))
        IconButton(
            modifier = Modifier.weight(Percent.EIGHT),
            onClick = { onAddToPlaylistClick(premiumAudio.id) },
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
}

@Composable
private fun ShowLoading(showLoading: Boolean) {
    if (showLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .background(brush = SolidColor(Black), alpha = Percent.TWENTY)
                    .fillMaxSize()
                    .clickable(false) {},
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun Spacer() {
    Spacer(modifier = Modifier.height(Spacing.s16))
}

@Preview
@Composable
private fun PremiumItemPreview() {
    GabiMorenoTheme {
        PremiumItem(
            premiumAudio =
                PremiumAudio(
                    id = "1",
                    url = "",
                    audioUrl = "",
                    imageUrl = "",
                    saga = Saga(author = "This is publisher", title = "This is saga title"),
                    thumbnailUrl = "",
                    pubDateMillis = 0,
                    title = "This is a title",
                    audioLengthInSeconds = 2700,
                    description = "This is a description",
                    category = Category.PREMIUM,
                    excerpt = "excerpt",
                    hasBeenListened = false,
                    markedAsFavorite = false,
                ),
            onItemClick = {},
            onListenedToggled = {},
            onAddToPlaylistClick = {},
            onFavoriteStatusChanged = {},
        )
    }
}

private const val CHANGE_COLOR_ANIMATION_DURATION = 300
private const val URL_ANDROIDES_PREMIUM = "https://gabimoreno.soy/los-androides-premium"
private const val REFRESH_DELAY = 1_500L

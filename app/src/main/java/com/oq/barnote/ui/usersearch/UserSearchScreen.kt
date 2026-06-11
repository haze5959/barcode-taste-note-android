package com.oq.barnote.ui.usersearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.SearchBar
import com.oq.barnote.ui.component.UserRow
import com.oq.barnote.ui.component.UserRowSkeleton
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun UserSearchRoute(
    onBack: () -> Unit,
    onShowUserNoteList: (userId: String) -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(UserSearchUiEvent.OnAppear) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is UserSearchNavEffect.UserNoteList -> onShowUserNoteList(effect.userId)
            }
        }
    }
    UserSearchScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun UserSearchScreen(
    state: UserSearchUiState,
    onEvent: (UserSearchUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val keyboard = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier
                        .size(Dimens.FabHSize)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .padding(12.dp),
                )
                SearchBar(
                    value = state.query,
                    placeholder = stringResource(R.string.nigneimeuro_geomsaeg),
                    onValueChange = { onEvent(UserSearchUiEvent.QueryChanged(it)) },
                    onSearch = {
                        keyboard?.hide()
                        onEvent(UserSearchUiEvent.Search)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            val listState = rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    val total = listState.layoutInfo.totalItemsCount
                    total > 0 && last >= total - 1
                }
            }
            LaunchedEffect(state.users, state.hasMore, state.isLoading) {
                snapshotFlow { shouldLoadMore }
                    .distinctUntilChanged()
                    .collect { atEnd ->
                        if (atEnd && state.hasMore && !state.isLoading) {
                            onEvent(UserSearchUiEvent.FetchNextPage)
                        }
                    }
            }

            when {
                // iOS: 초기 로딩 시 4개 스켈레톤 행.
                state.isLoading && state.users.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    repeat(4) { UserRowSkeleton() }
                }

                // iOS: ContentUnavailableView("'<query>' 유저를 찾을 수 없습니다", person.fill.questionmark).
                state.users.isEmpty() && state.hasSearchedOnce -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateView(
                        title = stringResource(
                            R.string.yujeoreul_cajeul_su_eobsseubnida,
                            state.query,
                        ),
                        icon = Icons.Filled.PersonOff,
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    items(state.users, key = { it.id }) { user ->
                        Box(modifier = Modifier.clickable {
                            onEvent(UserSearchUiEvent.TappedUser(user.id))
                        }) {
                            UserRow(
                                userInfo = user,
                                onButtonTap = { id, isFollowing ->
                                    onEvent(UserSearchUiEvent.ToggleFollow(user))
                                },
                            )
                        }
                    }
                    // iOS: 페이징 중 하단 스켈레톤/스피너.
                    if (state.isLoading && state.users.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.Padding),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }
                }
            }
        }
    }
}

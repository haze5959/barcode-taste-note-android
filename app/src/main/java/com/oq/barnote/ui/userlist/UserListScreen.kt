package com.oq.barnote.ui.userlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.oq.barnote.ui.util.RefreshOnResume
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.UserRow
import com.oq.barnote.ui.component.UserRowSkeleton

@Composable
fun UserListRoute(
    type: UserListListType,
    onBack: () -> Unit,
    onShowUserNoteList: (userId: String) -> Unit,
    onShowUserSearch: () -> Unit,
    viewModel: UserListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(type) { viewModel.onEvent(UserListUiEvent.OnAppear(type)) }
    RefreshOnResume { viewModel.onEvent(UserListUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is UserListNavEffect.UserNoteList -> onShowUserNoteList(effect.userId)
                UserListNavEffect.UserSearch -> onShowUserSearch()
            }
        }
    }
    UserListScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun UserListScreen(
    state: UserListUiState,
    onEvent: (UserListUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        if (state.type == UserListListType.Followers) R.string.palroweo
                        else R.string.palroing,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                // iOS `UserListView`: `person.badge.plus` 툴바 버튼은 listType == .following 일 때만 노출.
                // 숨길 때는 동일 크기 Spacer 로 대체해 타이틀이 가운데에서 어긋나지 않게 유지.
                if (state.type == UserListListType.Following) {
                    Icon(
                        imageVector = Icons.Filled.PersonSearch,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier
                            .size(Dimens.FabHSize)
                            .clip(CircleShape)
                            .clickable { onEvent(UserListUiEvent.TappedSearch) }
                            .padding(12.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.size(Dimens.FabHSize))
                }
            }

            when {
                // iOS: `if store.isLoading { ForEach(0..<4) { UserRowSkeletonView() } }`
                state.isLoading -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.BtnPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    items(4) { UserRowSkeleton() }
                }

                // iOS: ContentUnavailableView(<followers/following title>, systemImage: "person.2.slash")
                state.users.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateView(
                        title = stringResource(
                            if (state.type == UserListListType.Followers) R.string.palroweoga_eobsseubnida
                            else R.string.palroinghaneun_yujeoga_eobsseubnida,
                        ),
                        icon = Icons.Filled.GroupOff,
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    items(state.users, key = { it.id }) { user ->
                        UserRowItem(
                            userInfo = user,
                            onTap = { onEvent(UserListUiEvent.TappedUser(user.id)) },
                            onToggleFollow = { _, _ -> onEvent(UserListUiEvent.ToggleFollow(user)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRowItem(
    userInfo: UserInfo,
    onTap: () -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
) {
    Box(modifier = Modifier.clickable(onClick = onTap)) {
        UserRow(
            userInfo = userInfo,
            onButtonTap = onToggleFollow,
        )
    }
}

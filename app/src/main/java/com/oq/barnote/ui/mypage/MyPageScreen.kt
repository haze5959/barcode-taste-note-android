package com.oq.barnote.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.ui.navigation.MainBottomBarHeight
import com.oq.barnote.core.oqcore.views.OQRoundedButton
import com.oq.barnote.core.oqcore.views.OQRoundedButtonStyleType
import com.oq.barnote.core.oqcore.views.SkeletonView
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.component.DashboardCard
import com.oq.barnote.ui.component.DashboardRow
import com.oq.barnote.ui.component.SubscriptionPromotionRow

/**
 * 마이페이지 라우트. iOS `MyPageView` 에 대응.
 */
@Composable
fun MyPageRoute(
    onShowLogin: () -> Unit,
    onShowUserDetail: () -> Unit,
    onShowNoteList: (isMine: Boolean) -> Unit,
    onShowProductList: (ProductListType) -> Unit,
    onShowNeededReviewNoteList: () -> Unit,
    onShowSubscribe: () -> Unit,
    onShowUserNoteList: (userId: String) -> Unit,
    onShowUserList: (UserListType) -> Unit,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(MyPageUiEvent.OnAppear)
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                MyPageNavEffect.Login -> onShowLogin()
                MyPageNavEffect.UserDetail -> onShowUserDetail()
                is MyPageNavEffect.NoteList -> onShowNoteList(effect.isMine)
                is MyPageNavEffect.ProductList -> onShowProductList(effect.type)
                MyPageNavEffect.NeededReviewNoteList -> onShowNeededReviewNoteList()
                MyPageNavEffect.Subscribe -> onShowSubscribe()
                is MyPageNavEffect.UserNoteList -> onShowUserNoteList(effect.userId)
                is MyPageNavEffect.UserList -> onShowUserList(effect.type)
            }
        }
    }

    MyPageScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun MyPageScreen(
    state: MyPageUiState,
    onEvent: (MyPageUiEvent) -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.BtnPadding)
                .padding(top = Dimens.SectionSpacing)
                // MainBottomBar(오버레이) 뒤로 콘텐츠가 스크롤되므로 바 높이만큼 하단 여백 추가.
                .padding(bottom = MainBottomBarHeight),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            when {
                // 로딩 중이거나 아직 최초 로그인 판별 전이면 스켈레톤 (로그아웃 화면 깜빡임 방지).
                state.isLoading || !state.hasLoadedOnce -> SkeletonContent()
                state.myInfo != null -> LoggedInContent(state = state, onEvent = onEvent)
                else -> LoggedOutContent(onLogin = { onEvent(MyPageUiEvent.TappedLogin) })
            }
        }

    }
}

@Composable
private fun SkeletonContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SkeletonView(
                modifier = Modifier
                    .width(Dimens.LargeCardSize)
                    .height(Dimens.IconSize),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.LargeCardSize),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.LargeCardSize),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
            ) {
                SkeletonView(modifier = Modifier.weight(1f))
                SkeletonView(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoggedInContent(
    state: MyPageUiState,
    onEvent: (MyPageUiEvent) -> Unit,
) {
    val user = state.myInfo!!
    val palette = barNotePalette()
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    // Header: 인사말 + 프로필 이미지
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(MyPageUiEvent.TappedMyInfo) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.nim_annyeonghaseyo, user.nickName),
            style = MaterialTheme.typography.titleLarge.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.weight(1f),
        )
        ProfileAvatar(profileImageId = user.profileImageId, accent = accent)
    }

    // Dashboard
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
        if (!state.isSubscribed) {
            SubscriptionPromotionRow(onClick = { onEvent(MyPageUiEvent.TappedSubscribe) })
        }

        DashboardRow(
            icon = Icons.Filled.Reorder,
            title = stringResource(R.string.nae_pideu),
            value = stringResource(R.string.nae_noteuwa_jeulgyeocajneun_jepum_gongyu),
            onClick = { onEvent(MyPageUiEvent.TappedProfile) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
            DashboardCard(
                icon = Icons.Filled.NoteAlt,
                title = stringResource(R.string.teiseuting_noteu),
                value = stringResource(R.string.gae, state.noteCount.toString()),
                modifier = Modifier.weight(1f),
                onClick = { onEvent(MyPageUiEvent.TappedMyNotes) },
            )
            DashboardCard(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.jjimhan_jepum),
                value = stringResource(R.string.gae, state.favoriteCount.toString()),
                modifier = Modifier.weight(1f),
                onClick = { onEvent(MyPageUiEvent.TappedFavorites) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
            DashboardCard(
                icon = Icons.Filled.PersonAdd,
                title = stringResource(R.string.palroing),
                value = stringResource(R.string.bogi),
                modifier = Modifier.weight(1f),
                onClick = { onEvent(MyPageUiEvent.TappedFollowing) },
            )
            DashboardCard(
                icon = Icons.Filled.Groups,
                title = stringResource(R.string.palroweo),
                value = if (state.followerCount > 0) state.followerCount.toString()
                else stringResource(R.string.bogi),
                modifier = Modifier.weight(1f),
                onClick = { onEvent(MyPageUiEvent.TappedFollowers) },
            )
        }

        // 마셔본 제품 Row (tip 오버레이 제거 — iOS 처럼 Row 는 그대로 두고 tip 을 아래에 인라인 삽입).
        DashboardRow(
            icon = Icons.Filled.WineBar,
            title = stringResource(R.string.masyeobon_jepum),
            onClick = { onEvent(MyPageUiEvent.TappedTastedProducts) },
        )

        // iOS `TipView(neededNoteProductTip, arrowEdge: .bottom)` 대응 — 오버레이/풍선이 아니라
        // "마셔본 제품"과 "노트 작성이 필요한 제품" Row 사이에 끼워지는 인라인 안내(아래 Row 설명).
        // close 시 영구 dismiss 되며 빠진다.
        com.oq.barnote.ui.tip.BarNoteInlineTip(
            tip = com.oq.barnote.ui.tip.BarnoteTip.NeededNoteProduct,
        )

        DashboardRow(
            icon = Icons.Filled.Edit,
            title = stringResource(R.string.noteu_jagseongi_pilyohan_jepum),
            hasNewBadge = state.neededReviewProduct,
            onClick = { onEvent(MyPageUiEvent.TappedNeededReviewNotes) },
        )
    }

    Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

    OQRoundedButton(
        text = stringResource(R.string.rogeuaus),
        onClick = { onEvent(MyPageUiEvent.TappedLogout) },
        style = OQRoundedButtonStyleType.TextSecondary,
        palette = palette,
        radius = Dimens.Radius.value,
    )

    Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
}

@Composable
private fun LoggedOutContent(onLogin: () -> Unit) {
    val palette = barNotePalette()
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Spacer(modifier = Modifier.height(Dimens.ViewSpacing))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.HelpOutline,
            contentDescription = null,
            tint = textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(Dimens.LargeCardSize),
        )

        Text(
            text = stringResource(R.string.str_3co_mane_rogeuinhago_namanui_sieum_noteureul_jagseonghae),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.Padding))

        OQFillButton(
            text = stringResource(R.string.rogeuinhagi),
            onClick = onLogin,
            palette = palette,
            radius = Dimens.Radius.value,
        )
    }
}

@Composable
private fun ProfileAvatar(profileImageId: String?, accent: Color) {
    Box(
        modifier = Modifier
            .size(Dimens.LargeIconSize)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImageId != null) {
            BTNImage(
                path = profileImageId,
                modifier = Modifier
                    .size(Dimens.LargeIconSize)
                    .clip(CircleShape),
                cornerRadius = 999.dp,
                contentScale = ContentScale.Crop,
                fallbackIcon = Icons.Filled.AccountCircle,
            )
        } else {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = accent.copy(alpha = 0.3f),
                modifier = Modifier.size(Dimens.LargeIconSize),
            )
        }
    }
}


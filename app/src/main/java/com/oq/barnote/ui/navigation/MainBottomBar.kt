package com.oq.barnote.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.component.AutoResizeText

/**
 * 최상위 탭 항목. iOS `AppNavigationFeature.Tab` (home/search/barcode/myPage/settings) 에 대응.
 *
 * - [Barcode] 는 별도 탭이지만 화면 자체로 라우팅하지 않고, AppRoot 가 가로채 카메라 권한 체크 후
 *   BarcodeScanner destination 으로 navigate 합니다. 따라서 NavBar 의 selected 표시에는 활성화되지 않음.
 */
internal enum class MainTab(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Home(Destinations.HOME, Icons.Filled.Home, R.string.hom),
    Search(Destinations.SEARCH, Icons.Filled.Search, R.string.geomsaeg),
    Barcode(Destinations.BARCODE_SCANNER, Icons.Filled.QrCodeScanner, R.string.bakodeu_seukaenhagi),
    MyPage(Destinations.MY_PAGE, Icons.Filled.Person, R.string.maipeiji),
    Settings(Destinations.SETTINGS, Icons.Filled.Settings, R.string.seoljeong);

    companion object {
        /** BottomNavBar 를 노출할 destination 의 집합 (top-level routes). */
        val routes: Set<String> = values().map { it.route }.toSet()
    }
}

/** 탭 바의 콘텐츠 높이(시스템 nav inset 제외) — 커스텀 탭 Row 높이. */
private val TabBarContentHeight = 76.dp

/**
 * MainBottomBar(floating pill)가 콘텐츠 위를 덮는 높이 (시스템 nav inset 제외).
 * = 탭 Row 높이([TabBarContentHeight]) + Box 의 vertical padding(Dimens.Spacing * 2).
 *
 * 바는 Scaffold bottomBar 슬롯이 아니라 오버레이라, 탭 화면 콘텐츠가 바 뒤로 스크롤된다. 마지막 항목이
 * 바에 가리지 않도록 탭 화면(Home/Search/MyPage/Settings)의 스크롤 하단 여백으로 이 값을 더해준다.
 * 콘텐츠와 바 모두 navigationBarsPadding 로 시스템 nav inset 을 각자 처리하므로 이 값은 디바이스 무관.
 */
val MainBottomBarHeight = TabBarContentHeight + Dimens.Spacing * 2

/**
 * 최상위 BottomNavBar. iOS `AppNavigationView.tabBar` 를 커스텀 [Row] 로 구현 (Material3 `NavigationBar` 미사용).
 *
 * - **floating glass pill**: `.ultraThinMaterial` 근사 배경 + 흰 그라데이션 stroke + 그림자 + 둥근 모서리.
 * - **선택 인디케이터**: 아이콘+라벨 묶음 뒤의 둥근 사각(accent 0.15) — 폭을 항목의 일부로 제한해 좌우 끝
 *   항목에서도 잘리지 않고, 색을 [animateColorAsState] 로 크로스페이드해 탭 전환이 부드럽다.
 * - **라벨**: iOS `lineLimit(1).minimumScaleFactor(0.5)` 와 동일 — 항상 1줄, 길면 축소. 모든 탭 높이가
 *   같아 아이콘 세로 정렬이 자동으로 일정해진다.
 * - **중앙 스캔 버튼**: accent 그라데이션 원 + 흰 stroke + accent 글로우 그림자(라벨 없이 강조).
 *
 * @param onTabClick Barcode 처럼 일반 navigate 가 아닌 권한 체크 등 special 처리 필요한 탭의 콜백.
 *                  null 또는 false 반환 시 기본 navigate 동작 수행.
 */
@Composable
internal fun MainBottomBar(
    navController: NavController,
    onTabClick: (MainTab) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    // 좁은(작은) 디바이스에서 5개 탭 + 중앙 버튼이 폭을 넘지 않도록 화면 폭에 비례해 아이콘/버튼을 축소.
    val compactScale = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.8f, 1f)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun onTab(tab: MainTab) {
        // 외부 special 처리가 true 를 반환하면 기본 navigate 생략.
        if (onTabClick(tab)) return
        if (currentRoute != tab.route) {
            // 다른 탭으로 전환 — startDestination 까지 popUpTo + saveState/restoreState 로 탭별 백스택 보존/복원.
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            // iOS `tabSelected` 의 `state.path.removeAll()` 대응 — 이미 선택된 탭을 다시 탭하면 루트까지 pop.
            navController.popBackStack(route = tab.route, inclusive = false)
        }
    }

    val glassStroke = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.3f),
        ),
    )
    Box(
        modifier = modifier
            .navigationBarsPadding()
            // floating pill — 좌우를 화면 가장자리에서 확실히 띄운다(그림자가 가장자리에 닿아 "붙어 보임" 방지).
            .padding(horizontal = 24.dp, vertical = Dimens.Spacing)
            .shadow(Dimens.Radius, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(colorResource(com.oq.barnote.core.designsystem.R.color.tabbar_background))
            .border(1.dp, glassStroke, RoundedCornerShape(Dimens.Radius)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TabBarContentHeight)
                .padding(horizontal = Dimens.Padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.values().forEach { tab ->
                // Search 의 등록 route 는 "search?keyword={keyword}" 이므로 쿼리부를 떼고 비교.
                val selected = backStack?.destination?.hierarchy?.any {
                    it.route?.substringBefore('?') == tab.route
                } == true
                if (tab == MainTab.Barcode) {
                    CentralScanButton(
                        tab = tab,
                        accent = accent,
                        compactScale = compactScale,
                        modifier = Modifier.weight(1f),
                        onClick = { onTab(tab) },
                    )
                } else {
                    TabButton(
                        tab = tab,
                        selected = selected,
                        accent = accent,
                        textSecondary = textSecondary,
                        compactScale = compactScale,
                        modifier = Modifier.weight(1f),
                        onClick = { onTab(tab) },
                    )
                }
            }
        }
    }
}

/**
 * 라벨이 있는 일반 탭. 아이콘 + 라벨(항상 1줄, 길면 축소) 묶음 뒤에 선택 인디케이터(둥근 사각)를 그린다.
 * iOS `tabButton` 대응 — 선택 시 accent 색 + 인디케이터, 미선택 시 textSecondary.
 */
@Composable
private fun TabButton(
    tab: MainTab,
    selected: Boolean,
    accent: Color,
    textSecondary: Color,
    compactScale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 탭 전환 시 색/인디케이터를 부드럽게 크로스페이드 (iOS matchedGeometry 슬라이드의 단순 대응).
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else textSecondary,
        label = "tabContentColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent,
        label = "tabIndicatorColor",
    )
    // iOS `.symbolEffect(.bounce)` 근사 — 선택 시 살짝 커지는 스프링.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "tabIconBounce",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                // 인디케이터/콘텐츠 폭을 항목의 84% 로 제한 → 좌우 끝 항목에서도 둥근 모서리에 닿아 잘리지 않음.
                .fillMaxWidth(0.84f)
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(indicatorColor)
                .padding(vertical = Dimens.Padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp * compactScale)
                    .scale(iconScale),
            )
            // iOS `lineLimit(1).minimumScaleFactor(0.5)` — 항상 1줄, 좁으면 폰트 축소(2줄 줄바꿈 안 함).
            AutoResizeText(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contentColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                minScaleFactor = 0.5f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 중앙 바코드 스캔 버튼(라벨 없음). iOS `centralScanButton` 대응 —
 * accent 그라데이션 원 + 흰 stroke + accent 글로우 그림자로 강조.
 */
@Composable
private fun CentralScanButton(
    tab: MainTab,
    accent: Color,
    compactScale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp * compactScale)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    spotColor = accent,
                    ambientColor = accent,
                )
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.82f))),
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint = Color.White,
                modifier = Modifier.size(28.dp * compactScale),
            )
        }
    }
}

/** 현재 destination 이 BottomBar 를 보여야 하는 top-level 인지 판단. */
@Composable
internal fun rememberShouldShowBottomBar(navController: NavController): Boolean {
    val backStack by navController.currentBackStackEntryAsState()
    // Search 는 optional `?keyword=...` 쿼리 때문에 destination.route 가 "search?keyword={keyword}" 로
    // 등록되어 MainTab.routes("search") 와 그대로는 안 맞음 → 쿼리부를 떼어 base 라우트로 비교.
    val route = (backStack?.destination?.route ?: return false).substringBefore('?')
    // Barcode 자체는 풀스크린 스캐너라 그 안에서는 BottomBar 숨김.
    if (route == Destinations.BARCODE_SCANNER) return false
    return route in MainTab.routes
}

package com.oq.barnote.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * MainBottomBar(floating pill)가 콘텐츠 위를 덮는 높이 (시스템 nav inset 제외).
 * = Material3 NavigationBar 기본 높이(80dp) + Box 의 vertical padding(Dimens.Spacing * 2).
 *
 * 바는 Scaffold bottomBar 슬롯이 아니라 오버레이라, 탭 화면 콘텐츠가 바 뒤로 스크롤된다. 마지막 항목이
 * 바에 가리지 않도록 탭 화면(Home/Search/MyPage/Settings)의 스크롤 하단 여백으로 이 값을 더해준다.
 * 콘텐츠와 바 모두 navigationBarsPadding 로 시스템 nav inset 을 각자 처리하므로 이 값은 디바이스 무관.
 */
val MainBottomBarHeight = 80.dp + Dimens.Spacing * 2

/**
 * 최상위 BottomNavBar.
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

    // 좁은(작은) 디바이스에서 5개 탭 + 중앙 FAB 가 폭을 넘어 좌우 끝 항목(홈/설정)이 잘리는 것을 막기 위해
    // 화면 폭에 비례해 아이콘/FAB 를 축소한다. 360dp 이상은 1.0, 그 미만은 최소 0.8 까지(아이콘 minimumScaleFactor).
    // 라벨(title)은 아래 AutoResizeText 가 각 항목 폭에 맞춰 별도로 축소한다.
    val compactScale = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.8f, 1f)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // iOS `tabBar`: floating glass pill — 화면 가장자리에서 떨어져 둥근 모서리 + white 그라데이션
    // stroke + shadow. 배경은 iOS `.ultraThinMaterial` 근사 — 반투명 surface 틴트(tabbar_background)로
    // 채워 콘텐츠가 비치는 프로스티드 글래스 느낌(Android엔 블러 머티리얼이 없어 알파 fill로 근사).
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
            // floating pill — 좌우를 화면 가장자리에서 확실히 띄운다. 아래 shadow(elevation 12dp, clip=false)가
            // 바깥으로 번져 BtnPadding(18dp) 만으론 그림자가 가장자리에 거의 닿아 "붙어 보였음" → 24dp 로 키움.
            .padding(horizontal = 24.dp, vertical = Dimens.Spacing)
            .shadow(Dimens.Radius, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(colorResource(com.oq.barnote.core.designsystem.R.color.tabbar_background))
            .border(1.dp, glassStroke, RoundedCornerShape(Dimens.Radius)),
    ) {
        NavigationBar(
            // 좌우 끝 항목(홈/설정)이 선택될 때 인디케이터 pill 이 둥근 모서리에 닿지 않도록 내부 수평 패딩.
            modifier = Modifier.padding(horizontal = Dimens.Padding),
            containerColor = Color.Transparent,
            // 인셋은 바깥 Box 의 navigationBarsPadding 이 처리 — 내부는 0.
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            MainTab.values().forEach { tab ->
                // Search 의 등록 route 는 "search?keyword={keyword}" 이므로 쿼리부를 떼고 비교.
                val selected = backStack?.destination?.hierarchy?.any {
                    it.route?.substringBefore('?') == tab.route
                } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        // 외부 special 처리가 true 를 반환하면 기본 navigate 생략.
                        if (onTabClick(tab)) return@NavigationBarItem
                        if (currentRoute != tab.route) {
                            // 다른 탭으로 전환 — startDestination 까지 popUpTo + saveState/restoreState 로
                            // 탭별 백스택을 보존/복원.
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            // iOS `tabSelected` 의 `state.path.removeAll()` 대응 — 이미 선택된 탭을 다시 탭하면
                            // 그 탭의 백스택을 루트(탭 route)까지 pop (inclusive=false 라 루트 자체는 유지).
                            navController.popBackStack(route = tab.route, inclusive = false)
                        }
                    },
                    icon = {
                        if (tab == MainTab.Barcode) {
                            // iOS centralScanButton — accent 그라데이션 원형 + white stroke(1.5) + accent glow.
                            // 다른 탭보다 크고 라벨 없이 아이콘만 노출해 바코드 스캔 진입을 강조.
                            Box(
                                modifier = Modifier
                                    .size(Dimens.FabHSize * compactScale)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = CircleShape,
                                        clip = false,
                                        spotColor = accent,
                                        ambientColor = accent,
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(accent, accent.copy(alpha = 0.82f)),
                                        ),
                                    )
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes),
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp * compactScale),
                                )
                            }
                        } else {
                            // iOS `.symbolEffect(.bounce)` 근사 — 선택 시 스프링 스케일 강조.
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.2f else 1f,
                                animationSpec = spring(dampingRatio = 0.4f),
                                label = "tabIconBounce",
                            )
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                // 기본 24dp 를 좁은 화면에서 compactScale 로 축소(아이콘 minimumScaleFactor).
                                // 선택 시 bounce(iconScale)는 그 위에 그대로 곱해 적용.
                                modifier = Modifier
                                    .size(24.dp * compactScale)
                                    .scale(iconScale),
                            )
                        }
                    },
                    // 바코드 탭은 강조 원형 버튼이라 라벨 없음(iOS centralScanButton 과 동일).
                    label = if (tab == MainTab.Barcode) {
                        null
                    } else {
                        @Composable {
                            // 라벨(title)이 좁은 항목 폭에서 2줄로 줄바꿈되거나 잘리지 않도록 한 줄 유지 +
                            // 폰트 자동 축소(iOS `.lineLimit(1).minimumScaleFactor` 대응). 색/굵기는
                            // NavigationBarItem 의 selected/unselectedTextColor 를 LocalContentColor 로 상속한다
                            // (labelMedium 의 color 가 Unspecified 이므로 내부 Text 가 LocalContentColor 사용).
                            AutoResizeText(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                minScaleFactor = 0.7f,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        // 바코드는 자체 강조 스타일이라 NavigationBar 인디케이터 pill 숨김.
                        indicatorColor = if (tab == MainTab.Barcode) Color.Transparent
                        else accent.copy(alpha = 0.15f),
                        unselectedIconColor = textSecondary,
                        unselectedTextColor = textSecondary,
                    ),
                )
            }
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

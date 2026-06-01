package com.oq.barnote.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens

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
 * 최상위 BottomNavBar.
 *
 * @param onTabClick Barcode 처럼 일반 navigate 가 아닌 권한 체크 등 special 처리 필요한 탭의 콜백.
 *                  null 또는 false 반환 시 기본 navigate 동작 수행.
 */
@Composable
internal fun MainBottomBar(
    navController: NavController,
    onTabClick: (MainTab) -> Boolean = { false },
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surfacePrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // iOS `tabBar`: floating glass pill — 화면 가장자리에서 떨어져 둥근 모서리 + 반투명 배경 +
    // white 그라데이션 stroke + shadow. Compose 는 backdrop blur(.ultraThinMaterial 의 핵심)를
    // 직접 지원하지 않아 surfacePrimary 반투명(0.78) 으로 frosted 틴트를 근사합니다
    // (InfoTagStyle.Material 과 동일한 트레이드오프). 둥근 pill + 그라데이션 보더 + 그림자가
    // 부유 글래스 느낌을 만듭니다.
    val glassStroke = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.3f),
        ),
    )
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Spacing)
            .shadow(Dimens.Radius, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary.copy(alpha = 0.78f))
            .border(1.dp, glassStroke, RoundedCornerShape(Dimens.Radius)),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            // 인셋은 바깥 Box 의 navigationBarsPadding 이 처리 — 내부는 0.
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            MainTab.values().forEach { tab ->
                val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
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
                        Icon(imageVector = tab.icon, contentDescription = null)
                    },
                    label = { Text(text = stringResource(tab.labelRes)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = accent.copy(alpha = 0.15f),
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
    val route = backStack?.destination?.route ?: return false
    // Barcode 자체는 풀스크린 스캐너라 그 안에서는 BottomBar 숨김.
    if (route == Destinations.BARCODE_SCANNER) return false
    return route in MainTab.routes
}

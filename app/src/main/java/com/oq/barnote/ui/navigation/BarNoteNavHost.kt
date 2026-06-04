package com.oq.barnote.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oq.barnote.ui.addnote.AddNoteRoute
import com.oq.barnote.ui.addproduct.AddProductRoute
import com.oq.barnote.ui.aicamera.AICameraRoute
import com.oq.barnote.ui.customercenter.CustomerCenterRoute
import com.oq.barnote.ui.editnote.EditNoteRoute
import com.oq.barnote.ui.home.HomeRoute
import com.oq.barnote.ui.mypage.MyPageRoute
import com.oq.barnote.ui.mypage.ProductListType
import com.oq.barnote.ui.mypage.UserListType
import com.oq.barnote.ui.mypage.subscription.SubscriptionRoute
import com.oq.barnote.ui.mypage.userdetail.UserDetailRoute
import com.oq.barnote.ui.notedetail.NoteDetailRoute
import com.oq.barnote.ui.notelist.NoteListListType
import com.oq.barnote.ui.notelist.NoteListRoute
import com.oq.barnote.ui.placeholder.PlaceholderScreen
import com.oq.barnote.ui.productdetail.ProductDetailRoute
import com.oq.barnote.ui.productlist.ProductListFetchType
import com.oq.barnote.ui.productlist.ProductListRoute
import com.oq.barnote.ui.report.ReportRoute
import com.oq.barnote.ui.scanner.BarcodeScannerRoute
import com.oq.barnote.ui.search.SearchRoute
import com.oq.barnote.ui.settings.SettingsRoute
import com.oq.barnote.ui.settings.reservation.ReservationSettingsRoute
import com.oq.barnote.ui.userlist.UserListListType as UserListListTypeUL
import com.oq.barnote.ui.userlist.UserListRoute
import com.oq.barnote.ui.usernotelist.UserNoteListRoute
import com.oq.barnote.ui.usersearch.UserSearchRoute

@Composable
fun BarNoteNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.HOME,
    contentPadding: PaddingValues = PaddingValues(),
    // iOS `delegate(.neededLogin)` → `AppNavigationFeature.neededLogin` → 글로벌 "로그인 필요" alert.
    // AppRoot 와 동일한 Activity-scoped 인스턴스를 가리켜 (이 호출부는 NavBackStackEntry 가 아닌
    // Activity ViewModelStoreOwner 범위), MainActivity 가 렌더하는 showNeededLoginAlert 를 띄움.
    appNavViewModel: AppNavigationViewModel = hiltViewModel(),
) {
    // 로그인 필요 시 LOGIN 화면으로 직접 push 하는 대신 글로벌 alert 를 띄움 (alert 확인 버튼이 로그인 진입).
    val onNeededLogin: () -> Unit = {
        appNavViewModel.onEvent(AppNavigationUiEvent.ShowNeededLogin)
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // --- Home -----------------------------------------------------------
        composable(Destinations.HOME) {
            HomeRoute(
                onShowBarcodeScanner = { navController.navigate(Destinations.BARCODE_SCANNER) },
                onShowNoteList = { isMine -> navController.navigate(Destinations.noteList(isMine)) },
                onShowNoteDetail = { id, productName ->
                    navController.navigate(Destinations.noteDetail(id, productName))
                },
                onShowRecentProductList = { navController.navigate(Destinations.RECENT_PRODUCT_LIST) },
                onShowProductDetail = { id, productName ->
                    navController.navigate(Destinations.productDetail(id, productName))
                },
                onShowMyPage = { navController.navigate(Destinations.MY_PAGE) },
                onShowSearch = { navController.navigate(Destinations.SEARCH) },
            )
        }

        // --- Search ---------------------------------------------------------
        composable(
            route = Destinations.SEARCH_ROUTE,
            arguments = listOf(
                navArgument(Destinations.SEARCH_ARG_KEYWORD) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val keyword = entry.arguments?.getString(Destinations.SEARCH_ARG_KEYWORD)
            SearchRoute(
                prefillKeyword = keyword,
                onBack = { navController.popBackStack() },
                onShowProductDetail = { id, productName ->
                    navController.navigate(Destinations.productDetail(id, productName))
                },
                onShowAddProduct = { navController.navigate(Destinations.ADD_PRODUCT) },
                onShowBarcodeScanner = { navController.navigate(Destinations.BARCODE_SCANNER) },
            )
        }

        composable(
            route = Destinations.ADD_PRODUCT_ROUTE,
            arguments = listOf(
                navArgument(Destinations.ADD_PRODUCT_ARG_BARCODE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Destinations.ADD_PRODUCT_ARG_DEFAULT_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val barcode = entry.arguments?.getString(Destinations.ADD_PRODUCT_ARG_BARCODE)
            val defaultName =
                entry.arguments?.getString(Destinations.ADD_PRODUCT_ARG_DEFAULT_NAME).orEmpty()
            AddProductRoute(
                barcode = barcode,
                defaultName = defaultName,
                onBack = { navController.popBackStack() },
                // iOS `delegate.searchProduct(name)` 대응 — AddProduct 닫고 Search 진입 + 검색어 자동 채움.
                onSearchWithKeyword = { keyword ->
                    navController.replaceTop(
                        route = Destinations.search(keyword),
                        popUpToRoute = Destinations.ADD_PRODUCT_ROUTE,
                    )
                },
            )
        }

        // --- Settings -------------------------------------------------------
        composable(Destinations.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                // iOS `settings.delegate(.neededLogin)` → `AppNavigationFeature.neededLogin` → 글로벌 "로그인 필요" alert.
                onShowLogin = onNeededLogin,
                onShowCustomerCenter = { navController.navigate(Destinations.CUSTOMER_CENTER) },
                onShowReservationSettings = {
                    navController.navigate(Destinations.RESERVATION_SETTINGS)
                },
                onShowSubscription = { navController.navigate(Destinations.SUBSCRIBE) },
            )
        }

        composable(Destinations.RESERVATION_SETTINGS) {
            ReservationSettingsRoute(
                onBack = { navController.popBackStack() },
                onWriteNote = { product ->
                    navController.navigate(Destinations.writeNote(product.id))
                },
            )
        }

        composable(Destinations.CUSTOMER_CENTER) {
            CustomerCenterRoute(
                onBack = { navController.popBackStack() },
                onShowReport = { navController.navigate(Destinations.report(null)) },
            )
        }

        composable(
            route = Destinations.WRITE_NOTE_ROUTE,
            arguments = listOf(
                navArgument(Destinations.WRITE_NOTE_ARG_PRODUCT_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val productId =
                entry.arguments?.getString(Destinations.WRITE_NOTE_ARG_PRODUCT_ID).orEmpty()
            AddNoteRoute(
                productId = productId,
                onBack = { navController.popBackStack() },
                // iOS `addNote.delegate(.showLogin)` 은 글로벌 `.neededLogin` alert 가 아니라 곧장 로그인으로 보냄.
                // AddNote 는 submit 직전 로그아웃 감지 시 자체 `showLoginAlert` 를 먼저 띄우고, 사용자가 확인하면
                // 이 콜백이 호출되는 구조이므로 여기서 글로벌 alert 를 또 띄우면 이중 안내가 됨 → 직접 LOGIN 으로 push.
                // iOS: 모든 로그인 진입은 전역 "로그인 필요" alert → webAuth 직행 (전용 화면 없음).
                onShowLogin = onNeededLogin,
            )
        }

        // iOS NoteDetail 의 `.fullScreenCover(item: editNote)` — 노트 수정은 슬라이드 업 모달.
        modalComposable(
            route = Destinations.EDIT_NOTE_ROUTE,
            arguments = listOf(
                navArgument(Destinations.EDIT_NOTE_ARG_NOTE_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val noteId =
                entry.arguments?.getString(Destinations.EDIT_NOTE_ARG_NOTE_ID).orEmpty()
            EditNoteRoute(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.REPORT_ROUTE,
            arguments = listOf(
                navArgument(Destinations.REPORT_ARG_PRODUCT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val productId = entry.arguments?.getString(Destinations.REPORT_ARG_PRODUCT_ID)
            ReportRoute(
                productId = productId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.USER_SEARCH) {
            UserSearchRoute(
                onBack = { navController.popBackStack() },
                onShowUserNoteList = { userId ->
                    navController.navigate(Destinations.userNoteList(userId))
                },
            )
        }

        // iOS `.fullScreenCover(isPresented: showAICamera)` — AI 라벨 스캔 모달.
        modalComposable(
            Destinations.AI_CAMERA_ROUTE,
            arguments = listOf(
                navArgument(Destinations.AI_CAMERA_ARG_BARCODE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AICameraRoute(
                onBack = { navController.popBackStack() },
                onProductCreated = { id, productName ->
                    // pop + navigate 를 한 트랜잭션으로 묶어 중간 백스택 상태(AI_CAMERA 잔존) 방지.
                    navController.replaceTop(
                        route = Destinations.productDetail(id, productName),
                        popUpToRoute = Destinations.AI_CAMERA_ROUTE,
                    )
                },
            )
        }

        // --- MyPage ----------------------------------------------------------
        composable(Destinations.MY_PAGE) {
            MyPageRoute(
                // iOS `myPage.delegate(.showLogin)` 은 글로벌 `.neededLogin` alert 가 아니라 곧장 로그인으로 보냄
                // (명시적 "로그인" 버튼 탭이라 "로그인 필요" 안내 alert 를 거치는 것이 오히려 부자연스러움) → 직접 LOGIN 으로 push.
                // iOS: 모든 로그인 진입은 전역 "로그인 필요" alert → webAuth 직행 (전용 화면 없음).
                onShowLogin = onNeededLogin,
                onShowUserDetail = { navController.navigate(Destinations.USER_DETAIL) },
                onShowNoteList = { isMine -> navController.navigate(Destinations.noteList(isMine)) },
                onShowProductList = { type ->
                    navController.navigate(Destinations.productList(type.name.lowercase()))
                },
                onShowNeededReviewNoteList = {
                    navController.navigate(Destinations.NEEDED_REVIEW_NOTE_LIST)
                },
                onShowSubscribe = { navController.navigate(Destinations.SUBSCRIBE) },
                onShowUserNoteList = { userId ->
                    navController.navigate(Destinations.userNoteList(userId))
                },
                onShowUserList = { type ->
                    navController.navigate(Destinations.userList(type.name.lowercase()))
                },
            )
        }

        composable(Destinations.USER_DETAIL) {
            UserDetailRoute(
                onBack = { navController.popBackStack() },
                onSubscribe = { navController.navigate(Destinations.SUBSCRIBE) },
                onDeleteAccount = {
                    // iOS `send(.tabSelected(.home))` 대응 — 회원 탈퇴 후 Home 탭으로 자동 전환.
                    // saveState=false + restoreState=false → 다른 탭의 캐시된 backstack 도 모두 비워서
                    // 비로그인 상태에서 stale 한 로그인-only 화면이 노출되지 않도록.
                    navController.navigate(Destinations.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
            )
        }

        // iOS `.fullScreenCover(isPresented: appController.showSubscriptionBinding())` — 구독 화면은 모달.
        modalComposable(Destinations.SUBSCRIBE) {
            SubscriptionRoute(onBack = { navController.popBackStack() })
        }

        composable(Destinations.NEEDED_REVIEW_NOTE_LIST) {
            NoteListRoute(
                type = NoteListListType.NeededReview,
                onBack = { navController.popBackStack() },
                onShowNoteDetail = { id, productName ->
                    navController.navigate(Destinations.noteDetail(id, productName))
                },
                onShowAddNote = { productId ->
                    navController.navigate(Destinations.writeNote(productId))
                },
                onGoSubscription = { navController.navigate(Destinations.SUBSCRIBE) },
            )
        }

        composable(
            route = Destinations.PRODUCT_LIST_ROUTE,
            arguments = listOf(
                navArgument(Destinations.PRODUCT_LIST_ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = ProductListType.Favorites.name.lowercase()
                },
            ),
        ) { entry ->
            val typeKey = entry.arguments?.getString(Destinations.PRODUCT_LIST_ARG_TYPE)
                ?: ProductListType.Favorites.name.lowercase()
            val fetchType = when (typeKey) {
                ProductListType.Tasted.name.lowercase() -> ProductListFetchType.Tasted
                else -> ProductListFetchType.Favorites
            }
            ProductListRoute(
                fetchType = fetchType,
                onBack = { navController.popBackStack() },
                onShowProductDetail = { id, productName ->
                    navController.navigate(Destinations.productDetail(id, productName))
                },
            )
        }

        composable(
            route = Destinations.USER_LIST_ROUTE,
            arguments = listOf(
                navArgument(Destinations.USER_LIST_ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = UserListType.Following.name.lowercase()
                },
            ),
        ) { entry ->
            val typeKey = entry.arguments?.getString(Destinations.USER_LIST_ARG_TYPE)
                ?: UserListType.Following.name.lowercase()
            val listType = when (typeKey) {
                UserListType.Followers.name.lowercase() -> UserListListTypeUL.Followers
                else -> UserListListTypeUL.Following
            }
            UserListRoute(
                type = listType,
                onBack = { navController.popBackStack() },
                onShowUserNoteList = { userId ->
                    navController.navigate(Destinations.userNoteList(userId))
                },
                onShowUserSearch = { navController.navigate(Destinations.USER_SEARCH) },
            )
        }

        composable(
            route = Destinations.USER_NOTE_LIST_ROUTE,
            arguments = listOf(
                navArgument(Destinations.USER_NOTE_LIST_ARG_USER_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val userId =
                entry.arguments?.getString(Destinations.USER_NOTE_LIST_ARG_USER_ID).orEmpty()
            UserNoteListRoute(
                userId = userId,
                onBack = { navController.popBackStack() },
                onShowNoteDetail = { id, productName ->
                    navController.navigate(Destinations.noteDetail(id, productName))
                },
                onShowProductDetail = { id, productName ->
                    navController.navigate(Destinations.productDetail(id, productName))
                },
                // iOS `userNoteList.delegate(.neededLogin)` → `AppNavigationFeature.neededLogin` → 글로벌 "로그인 필요" alert.
                onShowLogin = onNeededLogin,
            )
        }

        // iOS `.fullScreenCover(isPresented: shwoBarcodeCamera)` — 바코드 스캐너 모달.
        modalComposable(Destinations.BARCODE_SCANNER) {
            BarcodeScannerRoute(
                onBack = { navController.popBackStack() },
                onProductFound = { id, productName ->
                    navController.replaceTop(
                        route = Destinations.productDetail(id, productName),
                        popUpToRoute = Destinations.BARCODE_SCANNER,
                    )
                },
                onGoAddProduct = { barcode ->
                    navController.replaceTop(
                        route = Destinations.addProduct(barcode = barcode),
                        popUpToRoute = Destinations.BARCODE_SCANNER,
                    )
                },
                // iOS `delegate.requestAICamera` (NotFound alert / bottom sheet 의 "AI 스캔하기")
                // NotFound 바코드를 AI 생성에 연계 (iOS pendingBarcodeForProductRegistration).
                onGoAICamera = { barcode ->
                    navController.replaceTop(
                        route = Destinations.aiCamera(barcode),
                        popUpToRoute = Destinations.BARCODE_SCANNER,
                    )
                },
                // iOS `checkCameraPermission(.ai)` 미로그인 → 글로벌 "로그인 필요" alert.
                onNeedLogin = onNeededLogin,
                // iOS `tabSelected(.search)` (bottom sheet 의 "제품 검색하기")
                onGoSearch = {
                    navController.replaceTop(
                        route = Destinations.SEARCH,
                        popUpToRoute = Destinations.BARCODE_SCANNER,
                    )
                },
            )
        }

        composable(Destinations.RECENT_PRODUCT_LIST) {
            ProductListRoute(
                fetchType = ProductListFetchType.Recent,
                onBack = { navController.popBackStack() },
                onShowProductDetail = { id, productName ->
                    navController.navigate(Destinations.productDetail(id, productName))
                },
            )
        }

        composable(
            route = Destinations.NOTE_LIST_ROUTE,
            arguments = listOf(
                navArgument(Destinations.NOTE_LIST_ARG_IS_MINE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val isMine = entry.arguments?.getBoolean(Destinations.NOTE_LIST_ARG_IS_MINE) ?: false
            NoteListRoute(
                type = if (isMine) NoteListListType.Mine else NoteListListType.All,
                onBack = { navController.popBackStack() },
                onShowNoteDetail = { id, productName ->
                    navController.navigate(Destinations.noteDetail(id, productName))
                },
                onShowAddNote = { productId ->
                    navController.navigate(Destinations.writeNote(productId))
                },
                onGoSubscription = { navController.navigate(Destinations.SUBSCRIBE) },
            )
        }

        composable(
            route = Destinations.NOTE_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(Destinations.NOTE_DETAIL_ARG_ID) { type = NavType.StringType },
                navArgument(Destinations.NOTE_DETAIL_ARG_PRODUCT_NAME) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString(Destinations.NOTE_DETAIL_ARG_ID).orEmpty()
            val productName =
                entry.arguments?.getString(Destinations.NOTE_DETAIL_ARG_PRODUCT_NAME).orEmpty()
            NoteDetailRoute(
                noteId = id,
                productName = productName,
                onBack = { navController.popBackStack() },
                onShowEdit = { noteId ->
                    navController.navigate(Destinations.editNote(noteId))
                },
                onShowProductDetail = { pid, pname ->
                    navController.navigate(Destinations.productDetail(pid, pname))
                },
                onShowUserNoteList = { userId ->
                    navController.navigate(Destinations.userNoteList(userId))
                },
            )
        }

        composable(
            route = Destinations.PRODUCT_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(Destinations.PRODUCT_DETAIL_ARG_ID) { type = NavType.StringType },
                navArgument(Destinations.PRODUCT_DETAIL_ARG_PRODUCT_NAME) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString(Destinations.PRODUCT_DETAIL_ARG_ID).orEmpty()
            val productName =
                entry.arguments?.getString(Destinations.PRODUCT_DETAIL_ARG_PRODUCT_NAME).orEmpty()
            ProductDetailRoute(
                productId = id,
                productName = productName,
                onBack = { navController.popBackStack() },
                onShowAddNote = { pid -> navController.navigate(Destinations.writeNote(pid)) },
                onShowNoteDetail = { nid, pname ->
                    navController.navigate(Destinations.noteDetail(nid, pname))
                },
                onShowReport = { pid -> navController.navigate(Destinations.report(pid)) },
                // iOS `productDetail.delegate(.neededLogin)` → `AppNavigationFeature.neededLogin` → 글로벌 "로그인 필요" alert.
                onNeededLogin = onNeededLogin,
                // iOS `appController.showSubscription = true` → 구독 화면.
                onGoSubscription = { navController.navigate(Destinations.SUBSCRIBE) },
                // iOS `delegate(.showReservationSettings)` (예약 토스트 의 "설정" 액션 대응).
                onGoReservationSettings = {
                    navController.navigate(Destinations.RESERVATION_SETTINGS)
                },
                // iOS `delegate(.showProductList(type:))` — 마셔본 제품 목록 진입.
                onGoProductList = { type ->
                    navController.navigate(Destinations.productList(type))
                },
            )
        }
    }
}

/**
 * iOS `.fullScreenCover(...)` 대응 — bottom 에서 slide-up 으로 등장, 닫힐 때 slide-down.
 *
 * 일반 `composable(...)` 의 default push 애니메이션 (수평 슬라이드) 대신 수직 슬라이드를 적용해
 * iOS modal presentation 의 인지적 동등성을 확보합니다. 카메라/구독/EditNote 같이 "한시적 task" 임을
 * 사용자에게 시각적으로 알려야 하는 화면에 사용합니다.
 */
private fun NavGraphBuilder.modalComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideInVertically(
                animationSpec = tween(durationMillis = 350),
                initialOffsetY = { fullHeight -> fullHeight },
            )
        },
        // 다른 destination 이 이 modal 위로 push 되는 경우는 상정하지 않음 (modal 은 마지막 layer).
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            slideOutVertically(
                animationSpec = tween(durationMillis = 300),
                targetOffsetY = { fullHeight -> fullHeight },
            )
        },
        content = { entry -> content(entry) },
    )
}

/**
 * "Modal-style finish" — 현재 destination 을 백스택에서 제거하고 새 destination 으로 atomic 이동.
 *
 * iOS 의 fullScreenCover 가 dismiss 와 동시에 새 화면을 push 할 때처럼,
 * `popBackStack() + navigate(...)` 의 두 단계 분리로 인한 깜빡임 / 백스택 중간 상태를 방지합니다.
 */
internal fun NavHostController.replaceTop(
    route: String,
    popUpToRoute: String,
) {
    navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
        launchSingleTop = true
    }
}

package com.oq.barnote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.oq.barnote.core.data.notification.NotificationSchedulerImpl
import com.oq.barnote.core.data.notification.NotificationTapDispatch
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.views.OQParticleEmitterHost
import kotlinx.serialization.json.Json
import com.oq.barnote.ui.navigation.AppNavigationNavEffect
import com.oq.barnote.ui.navigation.AppNavigationUiEvent
import com.oq.barnote.ui.navigation.AppNavigationViewModel
import com.oq.barnote.ui.navigation.BarNoteNavHost
import com.oq.barnote.ui.navigation.Destinations
import com.oq.barnote.ui.navigation.GlobalAiScanLoadingOverlay
import com.oq.barnote.ui.navigation.GlobalErrorDialogHost
import com.oq.barnote.ui.navigation.GlobalLoadingOverlay
import com.oq.barnote.ui.navigation.GlobalToastHost
import com.oq.barnote.ui.navigation.MainBottomBar
import com.oq.barnote.ui.navigation.MainTab
import com.oq.barnote.ui.navigation.rememberShouldShowBottomBar
import com.oq.barnote.ui.review.AppReviewRequester
import com.oq.barnote.ui.theme.AppLanguageApplicator
import com.oq.barnote.ui.theme.AppThemeApplicator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 앱 진입점 Activity. iOS `AppNavigationView` 에 대응.
 *
 * - edge-to-edge + Scaffold (BottomBar + GlobalOverlay)
 * - 5탭 BottomNavBar (Home / Search / Barcode / MyPage / Settings)
 * - 글로벌 다이얼로그 (로그인 필요 alert)
 * - Deep link 처리: Intent.ACTION_VIEW data URI → AppNavigationViewModel
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appThemeApplicator: AppThemeApplicator

    @Inject
    lateinit var appLanguageApplicator: AppLanguageApplicator

    @Inject
    lateinit var appController: AppController

    @Inject
    lateinit var json: Json

    override fun onCreate(savedInstanceState: Bundle?) {
        appThemeApplicator.applyOnStartup()
        appLanguageApplicator.applyOnStartup()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 알림 탭으로 launch 된 경우 NotificationEvent 를 emit (iOS `didReceive response` 등가).
        // setIntent 대신 onCreate 의 intent 를 직접 dispatch — savedInstance 가 있어도 같은 intent 가 들어옴.
        dispatchNotificationTap(intent)

        setContent {
            MaterialTheme {
                AppRoot(
                    appController = appController,
                    initialDeepLink = intent?.dataString,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱이 이미 떠 있는 상태에서 deep link / 알림 탭이 들어오면 setIntent 후 Composable 가 collect.
        setIntent(intent)
        dispatchNotificationTap(intent)
    }

    /**
     * Intent extras 에 [NotificationTapDispatch.EXTRA_TYPE] 가 있으면 [NotificationEvent] 로 변환해
     * [NotificationSchedulerImpl.emitEvent] 발행. AppNavigationViewModel 의 eventStream 구독자가
     * 받아 적절한 destination 으로 라우팅합니다.
     *
     * 처리 후 [NotificationTapDispatch.consume] 으로 extras 를 제거 — Activity 재구성 시 중복 dispatch 방지.
     */
    private fun dispatchNotificationTap(intent: Intent?) {
        val event = NotificationTapDispatch.parseEvent(intent, json) ?: return
        NotificationSchedulerImpl.emitEvent(event)
        NotificationTapDispatch.consume(intent)
    }

    // 구독 상태 foreground 동기화는 [BarNoteApp.SubscriptionResumeObserver] (ProcessLifecycleOwner) 가
    // 처리합니다 — Activity 단위가 아닌 process 단위 lifecycle 이라 미래에 Activity 가 추가돼도 일관됩니다.
}

@Composable
private fun AppRoot(
    appController: AppController,
    initialDeepLink: String? = null,
    appNavViewModel: AppNavigationViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val showBottomBar = rememberShouldShowBottomBar(navController)
    val appNavState by appNavViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 글로벌 NavEffect 처리.
    //
    // 알림 / deep link 로 트리거되는 NavEffect (GoProductDetail / GoNoteDetail /
    // GoUserNoteList / GoFollowersList) 는 iOS `state.path.removeAll(); state.path.append(...)` 와
    // 동등하게 기존 backstack 을 root (HOME) 까지 비우고 push 합니다. 그렇게 하지 않으면
    // 사용자가 깊은 화면에 있다가 알림을 탭했을 때 뒤로가기 동선이 비정상이 됩니다.
    LaunchedEffect(Unit) {
        appNavViewModel.navEffect.collect { effect ->
            when (effect) {
                AppNavigationNavEffect.GoLogin ->
                    navController.navigate(Destinations.LOGIN)
                is AppNavigationNavEffect.GoAddNote ->
                    navController.navigate(Destinations.writeNote(effect.productId))
                AppNavigationNavEffect.GoSubscription ->
                    navController.navigate(Destinations.SUBSCRIBE)
                AppNavigationNavEffect.GoAICamera ->
                    navController.navigate(Destinations.AI_CAMERA)
                is AppNavigationNavEffect.GoProductDetail ->
                    navController.navigateClearingStack(
                        Destinations.productDetail(effect.productId, effect.productName),
                    )
                is AppNavigationNavEffect.GoNoteDetail ->
                    navController.navigateClearingStack(
                        Destinations.noteDetail(effect.noteId, effect.productName),
                    )
                is AppNavigationNavEffect.GoUserNoteList ->
                    navController.navigateClearingStack(
                        Destinations.userNoteList(effect.userId),
                    )
                AppNavigationNavEffect.GoFollowersList ->
                    navController.navigateClearingStack(Destinations.followersList())
            }
        }
    }

    // 콜드 스타트 세션(토큰) 명시적 검증 + 사용자 캐시 예열.
    // iOS `App.onTask` 가 getUser/currentCredentials 로 토큰을 조기 검증하던 흐름의 등가물.
    LaunchedEffect(Unit) {
        appNavViewModel.validateSessionOnColdStart()
    }

    // 초기 / 새 deep link
    LaunchedEffect(initialDeepLink) {
        if (!initialDeepLink.isNullOrBlank()) {
            appNavViewModel.onEvent(AppNavigationUiEvent.HandleDeepLink(initialDeepLink))
        }
    }

    // Google Play In-App Review 요청 — Activity context 가 필요해 여기서 collect.
    LaunchedEffect(Unit) {
        appController.reviewRequestEvent.collect {
            val activity = context as? Activity ?: return@collect
            AppReviewRequester.request(activity)
        }
    }

    // iOS `appController.showSubscription = true` → `.fullScreenCover` 패턴 글로벌 트리거.
    // 어느 ViewModel 이든 `appController.requestSubscription()` 호출만으로 구독 화면이 떠야 함.
    LaunchedEffect(Unit) {
        appController.subscriptionRequestEvent.collect {
            navController.navigate(Destinations.SUBSCRIBE)
        }
    }

    // iOS `@AppStorage(lastSelectedTabKey)` 콜드 스타트 복원 — initialDeepLink 가 없을 때만 발효.
    // (deep link 가 있으면 그 destination 이 우선이라 last tab 으로 jump 하면 안 됨)
    LaunchedEffect(Unit) {
        if (!initialDeepLink.isNullOrBlank()) return@LaunchedEffect
        val savedRoute = appNavViewModel.consumeLastSelectedTab() ?: return@LaunchedEffect
        if (savedRoute == Destinations.HOME) return@LaunchedEffect // 이미 startDestination 이라 skip.
        if (savedRoute !in com.oq.barnote.ui.navigation.MainTab.routes) return@LaunchedEffect
        // BARCODE_SCANNER 는 탭 자체로 모달이라 자동 복원하지 않음 (사용자 의도 위반).
        if (savedRoute == Destinations.BARCODE_SCANNER) return@LaunchedEffect
        navController.navigate(savedRoute) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    navController = navController,
                    onTabClick = { tab ->
                        // Barcode 탭은 별도 처리 (카메라 권한은 BarcodeScannerScreen 내부에서 체크).
                        if (tab == MainTab.Barcode) {
                            navController.navigate(Destinations.BARCODE_SCANNER)
                            true
                        } else {
                            // iOS `state.selectedTab = tab` + `UserDefaults.set(...)` 와 동등 —
                            // 일반 탭 변경 시 lastSelectedTab 영속화 (다음 콜드 스타트 복원용).
                            appNavViewModel.rememberLastSelectedTab(tab.route)
                            false
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            BarNoteNavHost(
                navController = navController,
                contentPadding = innerPadding,
            )
            GlobalLoadingOverlay(appController = appController)
            // iOS AI 스캔용 별도 풀스크린 로딩 오버레이.
            GlobalAiScanLoadingOverlay(appController = appController)
            // iOS OQParticleEmitter.burstAtBottom() 글로벌 오버레이.
            OQParticleEmitterHost(trigger = appController.particleBurstEvent)
            // iOS OQToast.show(...) 글로벌 호스트 — OQToastConfig.position 에 따라 top/bottom 자동.
            GlobalToastHost(appController = appController)
        }
    }

    // iOS `.errorAlert(error: appController.errorBinding())` 글로벌 다이얼로그.
    GlobalErrorDialogHost(appController = appController)

    // 글로벌 "로그인 필요" 다이얼로그
    if (appNavState.showNeededLoginAlert) {
        AlertDialog(
            onDismissRequest = { appNavViewModel.onEvent(AppNavigationUiEvent.DismissNeededLogin) },
            title = { Text(text = stringResource(R.string.rogeuin_pilyo)) },
            text = {
                Text(
                    text = stringResource(R.string.bogjabhan_gaib_eobsi_3comane_rogeuinhago_gineungeul_iyonghae),
                )
            },
            confirmButton = {
                TextButton(onClick = { appNavViewModel.onEvent(AppNavigationUiEvent.ConfirmGoLogin) }) {
                    Text(text = stringResource(R.string.rogeuinhareo_gagi))
                }
            },
            dismissButton = {
                TextButton(onClick = { appNavViewModel.onEvent(AppNavigationUiEvent.DismissNeededLogin) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }
}

/**
 * iOS `state.path.removeAll(); state.path.append(...)` 의 안드로이드 등가물.
 *
 * 백스택을 graph 의 startDestination (HOME) 까지 비우고 target 으로 push.
 * Notification deep link 등 사용자가 "임의의 위치에서" target 으로 점프해야 하는 시점에 사용해
 * 뒤로가기 동선이 비정상적이 되지 않도록 합니다.
 */
private fun androidx.navigation.NavHostController.navigateClearingStack(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

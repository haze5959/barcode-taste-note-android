package com.oq.barnote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.oq.barnote.core.data.notification.NotificationSchedulerImpl
import com.oq.barnote.core.data.notification.NotificationTapDispatch
import com.oq.barnote.core.designsystem.BarNoteTypography
import com.oq.barnote.core.designsystem.barNoteColorScheme
import com.oq.barnote.core.oqcore.utils.AppController
import com.oq.barnote.core.oqcore.views.OQParticleEmitterHost
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import com.oq.barnote.ui.login.startAuth0Login
import com.oq.barnote.ui.login.startAuth0Logout
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
import com.oq.barnote.ui.settings.SettingsPreferences
import com.oq.barnote.ui.theme.AppLanguageApplicator
import com.oq.barnote.ui.theme.AppThemeApplicator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQAlert
import com.oq.barnote.core.oqcore.views.OQAlertButton
import com.oq.barnote.core.oqcore.views.OQAlertButtonStyle

/**
 * 앱 진입점 Activity. iOS `AppNavigationView` 에 대응.
 *
 * - edge-to-edge + Scaffold (BottomBar + GlobalOverlay)
 * - 5탭 BottomNavBar (Home / Search / Barcode / MyPage / Settings)
 * - 글로벌 다이얼로그 (로그인 필요 alert)
 * - Deep link 처리: Intent.ACTION_VIEW data URI → AppNavigationViewModel
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appThemeApplicator: AppThemeApplicator

    @Inject
    lateinit var appLanguageApplicator: AppLanguageApplicator

    @Inject
    lateinit var appController: AppController

    @Inject
    lateinit var json: Json

    @Inject
    lateinit var settingsPreferences: SettingsPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // @AndroidEntryPoint 의 @Inject lateinit 필드는 super.onCreate() 시점에 주입됨.
        // → 주입 대상(appThemeApplicator 등)은 반드시 super.onCreate() 이후에 사용.
        appThemeApplicator.applyOnStartup()
        appLanguageApplicator.applyOnStartup()

        // iOS 처럼 마지막으로 보던 탭으로 바로 시작 — HOME 을 거치지 않아 HomeScreen 의 네트워크
        // (OnAppear)가 불필요하게 실행되지 않는다. (deep link 가 있으면 HOME 시작 후 그 위로 navigate)
        val deepLinkStr = intent?.dataString ?: intent?.getStringExtra(NotificationTapDispatch.EXTRA_DEEP_LINK) ?: intent?.getStringExtra(NotificationTapDispatch.EXTRA_DEEP_URL)
        val startDestination = resolveStartDestination(deepLinkStr)

        // 알림 탭 및 Deep Link 로 launch 된 경우 NotificationEvent 를 emit (iOS `didReceive response` 등가).
        // setIntent 대신 onCreate 의 intent 를 직접 dispatch — savedInstance 가 있어도 같은 intent 가 들어옴.
        // 여기서 intent.data 와 extras 를 consume 하므로 반드시 resolveStartDestination 이후에 호출.
        dispatchNotificationTap(intent)

        setContent {
            // Material 컴포넌트(AlertDialog/DropdownMenu/CircularProgressIndicator 등)는 색 미지정 시
            // ColorScheme 을 따른다. 기본값은 보라 primary + 라이트 surface 라 다크모드에서 다이얼로그/
            // 메뉴/인디케이터가 깨지므로, 디자인 시스템 토큰 기반 [barNoteColorScheme] 으로 교체한다.
            MaterialTheme(
                colorScheme = barNoteColorScheme(),
                typography = BarNoteTypography,
            ) {
                // 색을 명시하지 않은 Text/Icon 의 기본색을 onSurface(text_primary) 로 제공.
                // Material 기본 LocalContentColor 는 검정이라 다크모드에서 안 보이는 문제 방지.
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                ) {
                    AppRoot(
                        appController = appController,
                        startDestination = startDestination,
                    )
                }
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

    /**
     * 콜드 스타트 NavHost 시작 destination 결정. iOS 가 앱 실행 시 마지막 탭으로 바로 띄우는 것과 동등.
     *
     * HOME 을 startDestination 으로 두고 사후에 navigate 하면 HomeScreen 이 먼저 compose 되어
     * 불필요한 네트워크(`HomeUiEvent.OnAppear`)가 실행된다. 이를 피하려고 마지막 탭을 **곧바로**
     * startDestination 으로 지정한다.
     * - deep link 가 있으면 HOME (deep link 가 그 위로 navigate 하므로 마지막 탭 복원 생략)
     * - 저장된 탭이 없거나 / top-level 탭이 아니거나 / 바코드(모달 탭)면 HOME
     *
     * `runBlocking` 은 [AppThemeApplicator.applyOnStartup] 과 동일하게 메인 스레드 1회 짧은
     * DataStore read 라 허용 범위.
     */
    private fun resolveStartDestination(deepLink: String?): String {
        if (!deepLink.isNullOrBlank()) return Destinations.HOME
        val saved = runBlocking { settingsPreferences.readLastSelectedTab() }
            ?: return Destinations.HOME
        return when {
            saved == Destinations.BARCODE_SCANNER -> Destinations.HOME
            saved !in MainTab.routes -> Destinations.HOME
            else -> saved
        }
    }

    // 구독 상태 foreground 동기화는 [BarNoteApp.SubscriptionResumeObserver] (ProcessLifecycleOwner) 가
    // 처리합니다 — Activity 단위가 아닌 process 단위 lifecycle 이라 미래에 Activity 가 추가돼도 일관됩니다.
}

@Composable
private fun AppRoot(
    appController: AppController,
    startDestination: String = Destinations.HOME,
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
                AppNavigationNavEffect.StartWebLogin -> {
                    // iOS: "로그인 필요" alert 확인 → 전용 화면 없이 곧장 Auth0 webAuth.
                    (context as? Activity)?.let { activity ->
                        startAuth0Login(
                            activity = activity,
                            onStart = { appNavViewModel.onLoginStarted() },
                            onSuccess = { appNavViewModel.onLoginSuccess(it) },
                            onError = { appNavViewModel.onLoginError(it) },
                            onCancel = { appNavViewModel.onLoginCancelled() },
                        )
                    }
                }
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

    // iOS `Auth0.webAuth().clearSession()` — 로그아웃/탈퇴 시 Auth0 브라우저(SSO) 세션 종료.
    // WebAuthProvider.logout 은 Activity 컨텍스트가 필요해 여기(Activity 보유)서 collect 해 실행.
    LaunchedEffect(Unit) {
        appController.logoutWebSessionEvent.collect {
            (context as? Activity)?.let { activity -> startAuth0Logout(activity) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 시스템 바(특히 하단 nav바) 영역에 비치는 배경 — 앱 배경과 일치시켜, 콘텐츠 inset 아래로
        // 카메라/타 화면이 비치는 이질적 strip 을 없앤다. (기본값은 Material3 기본색이라 앱 배경과 달랐다.)
        containerColor = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            BarNoteNavHost(
                navController = navController,
                startDestination = startDestination,
                contentPadding = innerPadding,
            )
            // iOS 투명 탭바 방식 — Scaffold bottomBar 슬롯(콘텐츠를 바 위로 inset 해 뒤가 안 보임) 대신
            // 콘텐츠 위에 오버레이. NavHost 가 바 높이만큼 잘리지 않아 콘텐츠가 바 뒤로 스크롤되어 비친다.
            // 글로벌 로딩/토스트가 바까지 덮어 터치를 막도록, 바는 글로벌 오버레이보다 먼저(아래) 그린다.
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
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // 아래 글로벌 오버레이들은 바보다 위에 그려져, 활성 시 바를 포함한 전체 화면을 덮고 터치를 막는다.
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
        OQAlert(
            title = stringResource(R.string.rogeuin_pilyo),
            message = stringResource(R.string.bogjabhan_gaib_eobsi_3comane_rogeuinhago_gineungeul_iyonghae),
            primaryButton = OQAlertButton(
                title = stringResource(R.string.rogeuinhareo_gagi),
                style = OQAlertButtonStyle.Primary,
            ),
            tertiaryButton = OQAlertButton(
                title = stringResource(R.string.cwiso),
                style = OQAlertButtonStyle.Tertiary,
            ),
            onPrimary = { appNavViewModel.onEvent(AppNavigationUiEvent.ConfirmGoLogin) },
            onTertiary = { appNavViewModel.onEvent(AppNavigationUiEvent.DismissNeededLogin) },
            onDismissRequest = { appNavViewModel.onEvent(AppNavigationUiEvent.DismissNeededLogin) },
            palette = barNotePalette(),
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

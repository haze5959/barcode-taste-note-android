# Android BarNote Project Context & Rules

## 1. Project Overview
- **Goal**: 바코드 스캔 및 AI 이미지 인식 기반 테이스팅 노트 안드로이드 앱 (iOS BarNote 마이그레이션).
- **Role**: 당신은 **Senior Android Tech Lead**. **Clean Architecture** + **UDF**를 엄격히 준수하고, 코드는 가독성·모듈화·유지보수·최신 컨벤션을 만족해야 합니다.

## 2. Tech Stack & Environment
- **Platform**: minSdk 29 / compileSdk · targetSdk 35 (Android 15)
- **Language**: Kotlin 2.1.10 · AGP 8.7.3 · Gradle Version Catalogs (`libs.versions.toml`)
- **UI**: Jetpack Compose Material3 (Compose BOM 2024.12.01 → material3 1.3.1)
- **Architecture**: MVI/UDF (`@HiltViewModel` + `StateFlow` + `Channel<NavEffect>`)
- **DI**: Hilt 2.53.1
- **Async**: kotlinx.coroutines 1.9.0 · Flow
- **Serialization**: kotlinx.serialization 1.7.3

## 3. Project Structure & Architecture (CRITICAL)
```
app
 ├─ core:domain      (pure Kotlin, no Android deps)
 ├─ core:oqcore      (Android lib · Compose · Retrofit/OkHttp · `OQ` prefix UI)
 ├─ core:network     ← core:domain, core:oqcore (Retrofit `BarNoteApi`)
 ├─ core:data        ← core:domain, core:network, core:oqcore (Repository/Store 구현)
 ├─ core:designsystem ← core:domain, core:oqcore (Material3 atomic, `Dimens`, `Palettes`, `DomainIcons`)
 └─ feature:*        (core:* 만 의존, feature 간 직접 의존 X)
```
- 각 화면은 `@HiltViewModel` + `UiState` + `UiEvent` + `Channel<NavEffect>` (UDF).
- Compose 는 `collectAsStateWithLifecycle()` 로 상태 수집.
- Navigation Compose 로 NavHost 구성, 각 feature 가 자체 graph 와 delegate 이벤트 정의.

## 4. Global State & UI Guidelines
- **글로벌 상태**: `AppController` (Hilt `@Singleton`) — `globalLoading: StateFlow<Boolean>`, `toastEvent: SharedFlow<ToastEvent>`, `neededToRefresh: Volatile Boolean`, `showError(Throwable)`.
- **디자인 시스템**: 하드코딩된 dp/색상/문자열 금지. `MaterialTheme.*`, `Dimens.*`, `R.dimen/string/color` 사용.
- **다국어**: 모든 문자열은 `res/values*/strings.xml` (총 11 locale: ko/en/zh-rCN/zh-rTW/ja/fr/de/es/pt/it/ru). Compose 내 `stringResource(R.string.xxx)`. 신규 키 추가 시 11 locale 모두 보완 필요. 모듈별 strings.xml: `app/` (앱 UI), `core/oqcore/` (공용 / 시스템 알림 등). `core/data/` 같은 res-less 모듈에서 string 필요 시 `com.oq.barnote.core.oqcore.R.string.xxx` 참조.

## 5. Logic & Feature Implementation Rules
- **Auth**: Auth0 Android SDK (`Auth0AuthStore` + `SecureCredentialsManager`). 로그인 필요 시 `AppNavigationViewModel.ShowNeededLogin` → 글로벌 alert.
- **Concurrency**: Kotlin Coroutines + Flow 전용. UI 수집은 `collectAsStateWithLifecycle()`.
- **Subscriptions**: Google Play Billing (`BillingManager`). 기능 제한 검사는 UseCase / `UserStore.checkSubscriptionStatus()`.

## 6. Constraints & Code Preferences
- **Third-Party 의존성**: `libs.versions.toml` 에 먼저 등록 → 모듈 `build.gradle.kts` 에서 `libs.x.y` 참조. 직접 `implementation("group:name:version")` 금지.
- **언어**: 주석/사고 과정/계획은 한국어, 코드 심볼은 영어.

## 7. AI Session Guidelines

### 7.0 RULES.md 작성 규칙 (가장 중요)
이 문서는 **미래 AI 세션을 위한 영구 지침서**입니다. 새 내용을 추가하기 전 3가지를 자문:
1. **6개월 뒤에도 유효한가?** "X 라이브러리 추가됨", "Y 화면 작성 완료" 등 시점 의존 정보는 잡음.
2. **코드/카탈로그/manifest 만 보면 알 수 있는가?** 컴포넌트 목록, 추가 권한, destination 이름 등은 해당 파일이 권위. RULES 에는 **언제/어떻게 추가할지의 컨벤션**만.
3. **다음 세션이 비슷한 작업 시 도움이 되는가?** 일반화 가능한 패턴/함정/외부 설정만 남기기.

**RULES 에 두는 것** ✅: 영구 컨벤션 / iOS↔Android 매핑 / 잠재적 함정 / 미해결 TODO.
**RULES 에 두지 않는 것** ❌: 작업 결과 이력, 추가한 라이브러리·권한·destination 목록, 버전 변경 이력, "✅ 완료" TODO, 특정 화면의 구성/strings 키 목록.

### 7.1 iOS → Android 매핑 컨벤션

**리소스 / 다국어** (지원 11 locale: ko/en/zh-rCN/zh-rTW/ja/fr/de/es/pt/it/ru)
- iOS 한글 키 → 로마자+해시 기반 식별자 (매핑: `app/src/main/res/key_mapping.txt`). 새 string 은 동일 규칙으로 11개 locale 모두 채우기 (영어 폴백 회피).
- 포맷 placeholder: `%@` → `%s`, `%lld` → `%d`. 정수 2개 이상 시 positional (`%1$d`, `%2$d`).
- iOS `zh-Hans/zh-Hant` → Android `values-zh-rCN/zh-rTW`.

**도메인 모델 / 네트워크**
- iOS `Codable` → `data class` + `@Serializable` (snake_case 는 `@SerialName`).
- iOS `UUID` → `String` (서버 응답 그대로). iOS `Date` → `String` (ISO8601, 표시 시점 `Instant.parse`).
- iOS mixed-type JSON → 안드로이드 custom `KSerializer` + `JsonObject`.
- iOS `Result<T, CommonError>` → Kotlin `Result<T>`. Repository 메서드는 `suspend fun ...: Result<T>` + `safeCall { }` 헬퍼 (`BarNoteRepositoryImpl` 참조). 실패는 `Result.exceptionOrNull() as? CommonError`.
- iOS `NetworkClient` → Retrofit2 + OkHttp3, Hilt `NetworkModule`. 토큰은 `AuthInterceptor`.

**상태 / 동시성**
- iOS `@MainActor @Observable AppController.shared` → Hilt `@Singleton` + `StateFlow`/`SharedFlow`.
- iOS `actor` → Kotlin `@Singleton` + `Mutex.withLock`.
- iOS `@Dependency` (TCA) → Hilt `@Inject` 생성자. iOS `@Published`/`AsyncStream` → `Flow`/`StateFlow`.
- iOS `UserDefaults` → DataStore Preferences. iOS `Keychain` → `EncryptedSharedPreferences`.
- iOS TCA `.debounce(id:for:)` → ViewModel 안에서 `MutableSharedFlow + debounce(ms).filter{...}.collect{ fetch() }`. 즉시 `isLoading = true` 로 디바운스 중 Skeleton 표시.

**UI 매핑 (Compose)**
- iOS `task(id:)` + `@Dependency` 내부 호출 → ViewModel 에서 미리 계산해 Composable 에 파라미터로 전달.
- iOS `popover` → `AlertDialog` / `ModalBottomSheet`. iOS `sheet(detents:)` → `ModalBottomSheet(skipPartiallyExpanded=...)`. 풀스크린 sheet → `Dialog(usePlatformDefaultWidth = false)`.
- iOS `oqAlert` 3-button → 커스텀 `Dialog` + 세로 버튼 스택 (`ThreeButtonDialog` 패턴).
- iOS `Image(systemName:)` → Material Icons (`Icons.Filled.*`). 도메인 enum 매핑은 `core:designsystem/DomainIcons.kt`.
- iOS `OQHapticService.selection.run()` → `LocalHapticFeedback.current.performHapticFeedback(TextHandleMove)`.
- iOS `FlowLayout` → `FlowRow`. iOS `TabView(.page)` → `HorizontalPager` (foundation.pager). iOS `Picker/Tabs` → `TabRow` + `Tab`.
- iOS `.refreshable { }` → Material3 `PullToRefreshBox` + `rememberPullToRefreshState()`.
- iOS `UIPasteboard.string =` → `ClipboardManager.setPrimaryClip(...)`. iOS `UIApplication.open(url)` → `Intent(ACTION_VIEW, url.toUri())`.
- iOS `Date.formatted(date: .long)` → `Instant.parse → ZoneId.systemDefault().toLocalDate()` + `DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)`.
- iOS `Menu + checkmark` → Material3 `DropdownMenu` + `DropdownMenuItem(leadingIcon = Icons.Filled.Check)`.
- iOS `LazyVGrid(.adaptive(min:))` → `LazyVerticalGrid(GridCells.Adaptive(minSize=X.dp))`. List↔Grid 토글은 `GridCells.Fixed(1)`.
- iOS 무한 스크롤 → `Lazy*State.layoutInfo.visibleItemsInfo.lastOrNull()?.index` 를 `snapshotFlow + distinctUntilChanged` 로 감지.
- iOS `AttributedString` 부분 강조 → `buildAnnotatedString { withStyle(SpanStyle(...)) { append(match) } }`.
- iOS `TextField.submitLabel(.search).onSubmit{}` → `BasicTextField` + `KeyboardOptions(imeAction=Search)` + `KeyboardActions(onSearch={})`.
- iOS overlay 패널 (검색 자동완성 등) → `Box` + `Modifier.zIndex(...)` + `Alignment.BottomCenter`.
- iOS `.swipeActions` → `SwipeToDismissBox` (Material3 1.3+) 또는 휴지통 아이콘 + `clickable`.
- iOS `BindableAction.binding(\.field)` → ViewModel `onEvent(FieldChanged(...))` (UDF 단방향).
- iOS `List + Section header/footer` → `Column` + 커스텀 `SettingsSection` + `SettingsRow`.
- iOS `Toggle.tint(accent)` → Material3 `Switch` + `SwitchDefaults.colors(checkedTrackColor=accent)`.
- iOS `DatePicker(.hourAndMinute)` → Material3 `TimePicker` + `rememberTimePickerState(...)` + `Dialog`.
- iOS `Stepper(in: 1...N)` → `-/+` IconButton Row + `coerceIn(min, max)`.
- iOS `UIActivityViewController` → `Intent.ACTION_SEND` + `FileProvider.getUriForFile()` + `Intent.createChooser`. `FLAG_GRANT_READ_URI_PERMISSION` 필수.
- iOS `OQSafariView` → `Intent.ACTION_VIEW` 또는 Chrome Custom Tabs (`androidx.browser:browser`).
- iOS `@AppStorage(key)` enum 영속화 → DataStore `stringPreferencesKey(key)` 에 `enum.id` 저장, `Enum.fromId(...)` 복원.
- iOS `TipKit` → Material3 `TooltipBox` + `RichTooltip` 기반 `BarNoteTip(tip = ..., anchor = { ... })` (`app/ui/tip/`). 새 tip 추가는 `BarnoteTip` enum 에 항목 + titleRes/messageRes 만 정의. dismiss 상태는 `TipPreferences` DataStore 의 `stringSetPreferencesKey` 에 tip ID 누적.

**SDK / 시스템 통합**
- iOS Auth0 `CredentialsManager` → Auth0 Android `SecureCredentialsManager`.
- iOS `UNUserNotificationCenter + UNCalendarNotificationTrigger` → `NotificationManager` + `AlarmManager.setExactAndAllowWhileIdle`. SCHEDULE_EXACT_ALARM 31+ user-grantable, `canScheduleExactAlarms()` 체크 + inexact fallback.
- iOS `Messaging.delegate` → `FirebaseMessagingService.onNewToken`/`onMessageReceived`.
- iOS `StoreKit Transaction.updates` → Google Play `BillingClient` + `PurchasesUpdatedListener` (`BillingManager` wrap).
- iOS `SubscriptionStoreView` → 직접 UI 작성 + `BillingClient.launchBillingFlow(activity, ...)`.
- iOS `OQMediaAttachmentPicker` → `ActivityResultContracts.PickVisualMedia` (`app/ui/picker/`).
- **Activity 컨텍스트 필요한 SDK 호출** (Auth0 WebAuth, BillingClient.launchBillingFlow): Composable 에서 `LocalContext.current.findActivity()` (ContextWrapper 체인 추출) → ViewModel 의 `fun launchX(activity)` 에 전달. SDK 의존성도 필요하면 Hilt `@EntryPoint` + `EntryPointAccessors.fromApplication(...)`.
- **`enableEdgeToEdge()` + Scaffold inset**: Activity `onCreate` 에서 호출 → `Scaffold { innerPadding -> NavHost(contentPadding=innerPadding) }`.
- **AppCompatDelegate 적용**: `setDefaultNightMode`/`setApplicationLocales` 는 Activity 재생성을 유발. DataStore 저장은 ViewModel, 실제 적용은 Application/Activity scope 의 `Applicator` 클래스 (`@Singleton` collect 루프 + `applyOnStartup()`).
- **테마 전환 애니메이션(radial reveal) — 안드로이드 미지원 (의도적)**: iOS `ThemeTransitionManager` 의 원형 reveal 테마 전환은 안드로이드에서 **지원하지 않음**. 위 `setDefaultNightMode` 가 **Activity 재생성**을 유발해 in-place Compose 애니메이션이 성립하지 않기 때문 — 테마는 하드컷(재생성)으로 적용한다. 제대로 구현하려면 ① recomposition 기반 테마(`LocalConfiguration`/`LocalContext` override 로 재생성 제거) 또는 ② 재생성을 가로지르는 비트맵 스냅샷이 필요한데, 둘 다 앱 전역 색상 경로를 바꾸는 고위험 변경이라 ROI 대비 보류. `core/oqcore/.../util/ThemeTransitionManager.kt` 가 존재하나 **미연결이 정상 상태** — 위 제약을 먼저 해소하지 않고 wire-up 시도 금지.

**ML Kit / CameraX / Tasks**
- **ML Kit 번역**: `TranslatorOptions.Builder().setSource().setTarget()` + `Translation.getClient(...)`. 첫 호출 시 `downloadModelIfNeeded(DownloadConditions.Builder().build())` (~30MB/언어쌍).
- **ML Kit Barcode**: `BarcodeScannerOptions.Builder().setBarcodeFormats(...)` + `barcodeScanner.process(InputImage.fromMediaImage(image, rotationDegrees))`. Manifest 에 `<meta-data android:name="com.google.mlkit.vision.DEPENDENCIES" android:value="barcode" />` 추가 시 사전 다운로드.
- **CameraX + Compose**: `AndroidView(factory = { PreviewView(it) })` + `ProcessCameraProvider.getInstance(ctx).addListener({...}, ContextCompat.getMainExecutor(ctx))`. `ImageAnalysis` 는 `STRATEGY_KEEP_ONLY_LATEST`. `DisposableEffect` 로 `executor.shutdown()` + `analyzer.close()`. 캡처는 `ImageCapture.takePicture(executor, OnImageCapturedCallback)` + `imageProxy.planes[0].buffer` JPEG.
- **Google Tasks → suspend**: `kotlinx-coroutines-play-services` 없이 직접 wrapping:
  ```kotlin
  suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
      addOnSuccessListener { if (cont.isActive) cont.resume(it) }
      addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
  }
  ```

**자체 Canvas (외부 라이브러리 없이)**
- 막대 차트: `Canvas { drawRect(color, Offset.Zero, Size(width=size.width*ratio, height=size.height)) }`.
- 월별 캘린더: `Row { repeat(7) { ... } }` 7열. 첫 주 시작 = `firstOfMonth.dayOfWeek.value % 7`.

**글로벌 네비게이션 / 알림 / 딥링크 / 앱 init** (iOS `AppNavigationFeature` + `BarNoteApp` 대응)
- **글로벌 orchestrator**: `AppNavigationViewModel` 이 NotificationScheduler events / deep link / 로그인 필요 alert / requestAddNote 제한 통합 관리. Composable `AppRoot` 가 `navEffect` collect 해 `NavController.navigate(...)`.
- **NotificationScheduler.eventStream() 처리**: ViewModel `init { collect }`. TappedReservation → ProductDetail + requestAddNote, NewFollower → 팔로워 목록, NewNote → UserNoteList.
- **Deep link**: Manifest `<intent-filter><data scheme=... host=.../></intent-filter>` + Activity `onCreate(intent.dataString)` + `onNewIntent(intent)` 모두 캐치 → ViewModel 이 `Uri.pathSegments` 파싱.
- **FCM 토큰 자동 등록**: Singleton `FcmTokenObserver` 가 `FcmTokenProvider.tokenStream().distinctUntilChanged().collect` → 로그인 사용자 있으면 `repository.registerFCMToken`. `BarNoteApp.onCreate` 에서 `start()`.
- **글로벌 로딩/토스트**: `AppController.globalLoading` → `GlobalLoadingOverlay` 반투명 spinner. `AppController.toastEvent` → `GlobalToastHost.showSnackbar(...)`.
- **BottomNavBar special tab**: `MainBottomBar(onTabClick: (MainTab) -> Boolean)` 에서 true 반환 시 기본 navigate 차단 (예: Barcode 탭).
- **무료 사용자 제한**: `requestAddNote(product)` → `checkSubscriptionStatus()` + `noteCount.value >= FREE_NOTE_COUNT` → 미구독+초과면 `GoSubscription`, 그 외 `GoAddNote`.
- **앱 init 순서** (`BarNoteApp.onCreate` 모든 글로벌 observer / applicator / preload):
  1. `AuthSessionObserver.start()` (로그아웃 시 UserStore 자동 정리)
  2. `AppThemeApplicator.start()` + `AppLanguageApplicator.start()` (DataStore + AppCompatDelegate)
  3. `FcmTokenObserver.start()` (FCM 토큰 → 서버 자동 등록)
  4. `@Inject lateinit var notificationScheduler: NotificationScheduler` 명시 inject — `@Singleton init { ensureChannel() }` 보장 (iOS `_ = NotificationClient.liveValue` 패턴, 콜드 스타트 push 유실 방지)
  5. `userStore.startSubscriptionObservation()` (BillingClient 연결 + 캐시 예열)
- **Billing (Google Play vs iOS StoreKit2)**: iOS 가 OS 차원에서 자동 처리하는 것들이 안드로이드는 클라이언트 책임. 누락 시 환불 사고 가능.
  - **acknowledgePurchase 필수**: `OK + PURCHASED` 콜백 + `refreshSubscriptionStatus()` 시 `Purchase.isAcknowledged == false` 면 `BillingClient.acknowledgePurchase(AcknowledgePurchaseParams)` 호출. 3일 내 미호출 시 자동 환불.
  - **obfuscatedAccountId 필수**: `BillingFlowParams.Builder().setObfuscatedAccountId(userId)` — iOS `appAccountToken(userId)` 대응. 영수증에 user UUID 매칭.
  - **외부 갱신 동기화**: `PurchasesUpdatedListener` 는 이 클라이언트 내 결제만 받음. `MainActivity.onResume` 에서 `userStore.refreshSubscriptionStatus()` 호출해 다른 기기 결제 / Play Console 환불 / 자동 갱신을 동기화.
  - 서버 영수증 검증 / Real-time developer notifications 는 "사용자가 앱 열 때만 구독 상태 확인" 정책이면 불필요 (iOS 도 동일 정책).
- **NoteDetail 화면 패턴**: iOS `NoteDetailFeature` 와 1:1 매핑.
  - **3-section** 구조: `HeroSection` (이미지 + publicScope 태그 + image count + 제품명 + 별점) / `TastingSection` (본문 + 향미 + 상세 평가) / `MetaSection` (작성자 + 작성일 + 공개 범위). 단일 `NoteDetailRow` 평탄화 회피.
  - **공유 FAB**: `isEditable && info != null` 일 때만 우하단 floating. Hilt `@EntryPoint` (`ShareEntryPoint`) 로 `OQSNSShareManager` 를 Composable 컨텍스트에서 가져와 `OQSNSShareBottomSheet` 호출. ViewModel 책임 분리.
  - **번역 inline**: AlertDialog 대신 `translatedBody ?? originalBody` 로 본문 텍스트 자체 교체. 번역 적용 후 "원본 보기" 토글로 `DismissTranslation` → null 복원.
  - **풀스크린 이미지 뷰어**: `Dialog(usePlatformDefaultWidth=false)` + `HorizontalPager` (ProductDetail 과 동일 패턴, 추후 공용 컴포넌트 추출 후보).
  - **공유 URL 조립**: `NoteInfo.shareUrl` computed property 가 도메인에 부재하므로 (`Constants.S.WEB_BASE_URL` 의존 격리 의도). 호출부 (NoteDetailScreen.toShareData) 에서 `"${Constants.S.WEB_BASE_URL}/note/${note.id}"` 직접 조립. UserDetail / 기타 공유 진입점도 동일 패턴.
- **ProductDetail 화면 패턴**: iOS `ProductDetailFeature` 와 1:1 매핑된 풍부한 화면. 다음 12개 동작이 1개 ViewModel 에서 orchestrate.
  - **TastedBanner**: `AnimatedVisibility` + 그라디언트 배경. `info.myNoteIds?.isNotEmpty() == true` 일 때만 표시. 첫 myNote 의 rating 이 0 이면 unrated alert, 아니면 NoteDetail.
  - **하단 듀얼 CTA**: `isTastedProduct = false` → "마셔본 제품 등록" 1개. `true` → "노트 작성하기" + "나중에 작성하기" 2개. 두 번째는 `AnimatedVisibility` 로 등장/사라짐.
  - **알림 예약**: `SharedPreferences("product_detail_prefs")` 의 `HAS_CONFIRMED_RESERVATION_KEY` 가 false 면 confirm alert → true 면 바로 `confirmReservation` → `NotificationScheduler.requestAuthorization()` + `ReservationStore.scheduleReservation(product)` → 토스트.
  - **마셔본 등록**: 무료 사용자 + 노트 카운트 ≥ `Constants.N.FREE_NOTE_COUNT` 면 `GoSubscription` effect. 그 외에는 빈 NoteDraft (rating=0, body="", publicScope=Private) 로 `repository.submitNote()` → 성공 시 `info.myNoteIds` 갱신 + 탭 전환 + `userStore.setNeededReviewProduct(true)` + 토스트.
  - **번역**: `NoteTranslator` (ML Kit) 사용. `translateName` / `translateDesc` 두 트리거. iOS 의 trigger UUID 패턴 대신 boolean flag (`isTranslatingName/Desc`) + result string (`translatedName/Desc`).
  - **클립보드 복사**: `tappedProductName` → `ClipboardManager.setPrimaryClip` + 토스트. iOS `UIPasteboard` 대응.
  - **풀스크린 이미지 뷰어**: `Dialog(DialogProperties(usePlatformDefaultWidth = false))` + `HorizontalPager(rememberPagerState)`. iOS `fullScreenCover` 대응.
  - **무한 페이지네이션**: `LazyColumn` 의 마지막 itemIndex 에서 `LaunchedEffect(notes.size)` 으로 `FetchNotesNextPage` 이벤트. iOS 의 `if note == store.noteInfos.last { send(.fetchNotes) }` 대응. ratingCounts 는 **첫 페이지에 한해** 계산 (iOS 동일).
  - **InfoPopOver**: `core/designsystem/component/InfoPopOver` 가 anchor 옆 popover 대신 `AlertDialog` 로 단순화 (Compose 한계). IBU 안내, 풍미 상세 설명 등에 사용.
  - **detail rows**: `Product.details: Map<ProductDetailInfo, String>` 을 `style/grape/manufacturer/country/alcohol/ibu` 순으로 표시. `composeDetailDisplayValue` @Composable 헬퍼가 `ProductStyle.title()` / `GrapeVariety.title()` / `Country.display(code)` 매핑 (iOS `displayValue` 4-인자 시그니처 대응).
  - **Vivino 섹션**: `product.type == ProductType.Wine` 일 때만. `URLEncoder.encode(name)` + `Intent.ACTION_VIEW`.
  - **Report 사전 alert**: `tappedReport` → `showReportAlert = true` → confirm 시에만 `Report` NavEffect. 사용자 실수 방지.
- **Kakao SDK (카카오톡 공유)**: iOS `KakaoSDKShare.ShareApi.shareCustom` 동등 흐름.
  - **의존성**: `com.kakao.sdk:v2-share` (libs `kakao-share`). 모듈 위치는 `core/oqcore` (OQSNSShare 가 거기).
  - **초기화**: `BarNoteApp.onCreate` 의 0-1단계에서 `KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)`. nativeAppKey 는 `local.properties` 의 `kakao.nativeAppKey` 에서 BuildConfig + manifestPlaceholder 로 주입.
  - **Manifest**: `<queries><package android:name="com.kakao.talk" /></queries>` (Android 11+ 패키지 가시성), `kakao{nativeAppKey}://oauth` intent-filter (콜백).
  - **shareCustom 호출**: `ShareClient.instance.isKakaoTalkSharingAvailable(context)` 분기 → `ShareClient.shareCustom(context, templateId, templateArgs)` 콜백에서 `result.intent` 으로 `startActivity` (FLAG_ACTIVITY_NEW_TASK 필수, Application context 사용 시).
  - **fallback**: 카카오톡 미설치 시 `WebSharerClient.instance.makeCustomUrl(templateId, templateArgs)` → `Intent.ACTION_VIEW` 으로 브라우저.
  - **이미지 캐싱**: iOS 의 `ShareApi.imageUpload(image:)` → Android `ShareClient.scrapImage(url)` (외부 URL → 카카오 서버 캐싱 1-step). result `infos.original.url` 사용.
  - **templateId 분기**: iOS 와 동일 — 이미지 0~1개 = 131000, 2개 = 131001, 3개+ = 130706. templateArgs key: `TITLE / DESC / NICK / PROFILE / IMG1 / IMG2 / IMG3 / SHARE_URL / PATH` + shareUrl 의 query params.
- **Instagram Stories 공유**: iOS `shareToInstagramStory` 동등 흐름 (`OQSNSShare`, `core/oqcore`).
  - **인텐트**: `com.instagram.share.ADD_TO_STORY`. 배경 = 첫 이미지 다운로드 → FileProvider URI 를 `setDataAndType(uri, "image/jpeg")` (없으면 `top/bottom_background_color` 그라디언트 `#231557`→`#FF1361`). 스티커(브랜드 카드 비트맵) = `interactive_asset_uri`. `source_application` = `packageName`.
  - **인프라**: 임시 파일은 `cacheDir/share/` (`file_paths.xml` `<cache-path name="share">`), `FLAG_GRANT_READ_URI_PERMISSION` + `grantUriPermission("com.instagram.android", …)`. Manifest `<queries><package com.instagram.android></queries>` (설치 조회/실행).
  - **스티커 렌더**: `buildStickerBitmap` 가 Canvas 로 어두운 라운드 카드(프로필 원형 + 닉/앱이름 + 제목 + 설명) 그림 (iOS `OQInstagramStickerView` 대응). 생성 실패해도 `runCatching`→null 로 공유는 진행(graceful).
  - **fallback**: 미설치(`ActivityNotFoundException`) → Play 스토어.
  - ⚠️ **source_application 한계**: iOS 는 bundleID. Android Instagram 은 정식 attribution 에 **Facebook App ID** 를 기대 — 미등록이면 출처 표시가 제한될 수 있음(기본 공유 동작에는 보통 영향 없음). 필요 시 FB App ID 등록 후 그 값으로 교체.
- **Firebase Crashlytics / Analytics 통합**: iOS 와 동등하게 자동 수집만 사용 (명시 이벤트 호출 없음).
  - **OQLog → Crashlytics 어댑터**: `core/oqcore` 가 Firebase 의존하지 않도록 `OQLog.exceptionLogger: ExceptionLogger?` (fun interface) 콜백만 노출. app 모듈의 `CrashlyticsInstaller.install()` 가 lambda 로 어댑팅. `BarNoteApp.onCreate` 의 0번째 단계에서 호출 (이후 단계의 에러도 보고되도록).
  - **OQLog.e 시그니처**: `e(message, throwable, prefix, saveLog)` — throwable 함께 전달하면 Crashlytics issue 로 묶임. message 만 있으면 stack trace 가 없어 추적 어려우므로 throwable 우선 전달.
  - **자동 screen view 비활성**: `AndroidManifest <meta-data android:name="google_analytics_automatic_screen_reporting_enabled" android:value="false" />` — iOS `FirebaseAutomaticScreenReportingEnabled: false` 대응. 화면 추적은 명시 호출 시에만.
  - **user id 매칭**: `AuthSessionObserver.AnalyticsBridge` (fun interface) → `CrashlyticsInstaller.setUserId(userId)`. 로그인/로그아웃 시 자동.
  - **Gradle plugin**: root `apply false` + app `alias(libs.plugins.google.services)` + `alias(libs.plugins.firebase.crashlytics)`. `google-services.json` 없으면 plugin 자체가 빌드 실패 (사용자 작업 필요).
- **Auth0 (안드로이드 SDK)**: iOS `AuthStore` + `NetworkClient` 의 자동 처리들을 명시 구현.
  - **scope 명시 필수**: `WebAuthProvider.login(auth0).withScope("openid profile offline_access")` — `offline_access` 없으면 refresh token 미발급 → `SecureCredentialsManager` 자동 갱신 동작 안 함. iOS `Auth0.webAuth().scope(...)` 대응.
  - **401 자동 처리**: OkHttp `Authenticator` 인터페이스로 별도 구현 (`TokenAuthenticator`). 401 응답 시 `AuthStore.forceRefreshCredentials()` → refresh 성공 시 자동 retry, 실패 시 `AuthStore.clear()` → `AuthSessionObserver` 가 UserStore 정리. `responseCount >= 2` 가드로 무한 루프 방지. iOS `NetworkClient.failure(.unauthorized)` 분기 대응.
  - **forceRefreshCredentials**: `SecureCredentialsManager.getCredentials(minTtl = 1년)` 으로 SDK 가 무조건 refresh token 사용. `currentCredentials()` 의 60초 만료 마진보다 강력.
  - **Interceptor vs Authenticator 역할 분리**: `AuthInterceptor` 는 every request 헤더 첨부 (SDK 캐시 hit 이라 빠름). `TokenAuthenticator` 는 401 응답 시에만 호출. 둘 다 OkHttp worker thread 의 `runBlocking` 이지만 dispatcher 블로킹은 아님 (worker pool 별도).
  - **HeadersProvider / TokenRefresher 패턴**: `core/oqcore` 모듈에 인터페이스만 두고 `core/data` 에 `AuthStore` 어댑팅. core/oqcore 가 core/domain 의존을 피함.
- **Firebase 초기화**: `com.google.gms.google-services` plugin 이 자동. 명시 호출 불필요. plugin 비활성 시 Firebase 의존 기능 자동 비활성.
- **콜드 스타트 push 유실 방지**: `MutableSharedFlow(extraBufferCapacity = N)` 으로 collect 전 emit 도 buffer 보존. `replay = 0` 유지 (중복 실행 방지). `companion object` static SharedFlow 는 process scope 유지.

### 7.2 잠재적 함정 / 작업 시 누락 주의
- **새 모듈**: `kotlin-kapt` + `hilt.android` + `kapt(libs.hilt.compiler)` 셋트. 빠지면 Hilt 주입 조용히 누락.
- **새 `@Serializable`**: 모듈에 `kotlin-serialization` plugin 적용 확인 (`core:domain/network/data/oqcore`).
- **새 Composable**: 모듈 `build.gradle.kts` 에 `buildFeatures { compose = true }`.
- **`core:domain` 추가 시**: `androidx.*`/`android.*` import 금지. UI 텍스트 매핑은 `app/extension/DomainStrings.kt`.
- **Receiver / Service**: `@AndroidEntryPoint` + `AndroidManifest.xml` 등록 둘 다 필요. 하나라도 빠지면 NPE.
- **새 라이브러리**: `libs.versions.toml` `[versions]` + `[libraries]` 등록 → `libs.x.y` 참조. 직접 표기 금지.
- **새 화면**: `BarNoteNavHost` + `Destinations.kt` 양쪽 등록. 미구현은 `PlaceholderScreen`.
- **iOS 중앙 게이트 → Android 진입점별 적용**: iOS 는 child feature 의 `.delegate(.showAddNote)` 등을 부모 reducer(`requestAddNote`)가 가로채 구독/제한 게이트를 한 곳에 둔다. Android 는 화면마다 독립 `NavEffect`+`navController.navigate` 라 **각 진입점이 게이트를 우회**하기 쉽다 (예: 노트작성은 ProductDetail `TappedAddNote` / NoteList `WriteUnratedNote` / 알림예약 등 여러 곳). 구독/로그인/한도 등 "공통 관문" 로직은 새 진입점마다 직접 적용했는지 확인 — 노트작성 게이트는 `checkSubscriptionStatus()` + `noteCount >= Constants.N.FREE_NOTE_COUNT` → `GoSubscription`.
- **ViewModel 에서 string resource**: `@Inject constructor(@ApplicationContext context: Context)` → `context.getString(R.string.xxx)`. Composable 의 `stringResource` 사용 불가.
- **새 BTN domain-dependent 컴포넌트**: `app/ui/component/` 에 둡니다 (Constants/strings 의존). 도메인 의존 없는 atomic 컴포넌트만 `core:designsystem/component/` 에 둡니다.
- **suspend IO/long-running**: `withContext(Dispatchers.IO)` 또는 `Mutex.withLock`. UI 스레드 차단 회피.
- **이미지/파일 업로드 후 정리**: 업로드된 imageId 는 노트/제품에 연결. 취소 시 ViewModel 이 `repository.deleteImage(id)` 정리 책임.
- **callback 시그니처 매핑**: `(a, b) -> Unit` 람다에 인자 무시 시 `{ _, _ -> ... }`. 인자 0개 람다 직접 전달은 type mismatch.
- **`Modifier` fully-qualified path 금지**: `Modifier.androidx.compose...width(...)` invalid. import 후 `Modifier.width(...)`. 이름 충돌 시 `import ... as Xxx`.
- **`R` 클래스 충돌**: 한 파일에서 `com.oq.barnote.R` (앱 strings) 과 `com.oq.barnote.core.designsystem.R` (color/dimen) 동시 필요 시 — 한쪽만 `import` 하고 다른 쪽은 fully-qualified (`com.oq.barnote.R.string.xxx`) 로 인라인 참조. `import ... as` 별칭도 가능.
- **Multipart 업로드의 client-generated id**: iOS `NetworkClient.upload` 는 `id` form-data part 를 항상 함께 전송. Retrofit 도 `@Part("id") id: RequestBody, @Part image: MultipartBody.Part` 둘 다 명시 필요. `MediaAttachment.id` 의 `UUID.randomUUID().toString()` 가 서버 영수증 매칭 / 중복 차단에 활용됨. 빠지면 optimistic update 불가.
- **인증 분기는 `currentCredentials() != null`**: `authStore.hasCredentials()` 는 만료 직전 토큰 갱신을 트리거하지 않음. iOS 와 동일하게 `authStore.currentCredentials() != null` 호출해 SDK 의 60초 마진 자동 refresh 를 활용해야 race 회피 (예: `BarNoteRepositoryImpl.authedPath`, `uploadImage`).
- **다단계 form step 변경 시 입력값 보존**: ViewModel `UiState` 가 모든 입력값 보관 → `step` 만 갱신. Composable `remember { }` 로 step-local state 두면 초기화 위험.
- **`FileProvider` 임시 파일 공유**: ① Manifest `<provider>` 등록 ② `res/xml/file_paths.xml` 정의 ③ `FLAG_GRANT_READ_URI_PERMISSION`. 셋 중 하나라도 빠지면 SecurityException.
- **DataStore 파일 분리**: 화면별 `preferencesDataStore(name=...)` 격리. 같은 키 (예: `IS_NOTIFICATION_ENABLED_KEY`) 는 반드시 같은 파일에서만 read/write.

### 7.3 미해결 TODO
- ⏳ Play Console 실제 구독 productId/basePlanId 를 `Constants.S.SUBSCRIPTION_PRODUCT_ID` 에 반영 (현재 placeholder `"barnote_premium"`).
- ⏳ Play Store 배포 키 등록: 업로드 키스토어(`release.*`) + Play 서비스계정 JSON(`play.serviceAccountFile`)을 `local.properties` 에 등록 (`deploy-playstore.command` 동작 조건).

### 7.4 자발적 업데이트 허용
영구 지침 (컨벤션 / 함정 / 매핑 패턴) 발견 시 사용자 요청 없이도 RULES.md 추가 가능. 단 **§7.0 작성 규칙을 엄격히** — 이력성/시점 의존 정보는 절대 추가하지 마세요.

---

## 8. Quick Reference

### 8.1 새 코드 위치 가이드
| 추가할 코드 | 위치 |
|---|---|
| 새 도메인 모델 (`data class`) / enum (rawValue+emoji 등 정적 데이터) | `core:domain` |
| 도메인 enum → `@StringRes` 매핑 | `app/extension/DomainStrings.kt` |
| 도메인 enum → `ImageVector` 매핑 | `core:designsystem/DomainIcons.kt` |
| Repository 인터페이스 | `core:domain` |
| Repository 구현체 + Hilt `@Binds` | `core:data` (+ `core:data/di/`) |
| 새 Retrofit endpoint | `core:network/BarNoteApi.kt` |
| 단일 화면용 ViewModel | feature 모듈 또는 `app` 임시 |
| 공용 atomic UI 컴포넌트 (텍스트 외부 주입) | `core:designsystem/component/` |
| 도메인 모델 직접 받는 UI 컴포넌트 | `app/ui/component/` |
| `OQ` prefix UI 컴포넌트 (앱 무관) | `core:oqcore/ui/component/` 또는 `views/` |
| 권한 / Picker 등 Activity 의존 헬퍼 | `app/ui/permission/`, `app/ui/picker/` |
| 새 Hilt 모듈 | 같은 모듈의 `di/` 패키지 |

### 8.2 Hilt 컨벤션
- **`@HiltAndroidApp`** → `BarNoteApp`.
- **`@AndroidEntryPoint`** → Activity / Fragment / BroadcastReceiver / Service. Receiver/Service 는 Manifest 등록 필수.
- **`@Binds`** (인터페이스↔구현 바인딩) / **`@Provides`** (SDK 객체 생성) 패턴 → 모듈은 같은 모듈의 `di/` 에.
- **`@Named` qualifier** → 동일 타입 구분 (예: `@Named("auth0Domain")`).
- **`@ApplicationScope`** → 앱 lifecycle CoroutineScope (`core:data/di/CoroutineScopeModule`).
- **`@ApplicationContext`** → Application Context (Hilt 기본).

### 8.3 빌드 시스템 특이사항
- `buildConfig = true`: `app/build.gradle.kts` 에서 활성화 (Auth0 `BuildConfig.AUTH0_*` 사용).
- `kotlinx-serialization` plugin: 새 `@Serializable` 클래스 추가 시 모듈 적용 여부 확인.
- `compose = true`: `app`, `core:oqcore`, `core:designsystem` 활성화. 다른 모듈에서 Composable 작성 시 활성화 필요.
- `kotlin-kapt`: Hilt 사용 모듈마다 + `kapt(libs.hilt.compiler)`.
- `libs.versions.toml` 모든 의존성 등록 의무. 직접 `implementation("...")` 금지.
- `local.properties` VCS 제외, Auth0 키 보관.

### 8.4 SDK 제약 / 주의
- **`minSdk = 29`**: `java.time` desugaring 없이 직접 사용. `Locale("ko")` OK.
- **`targetSdk = 35`** (Android 15): Edge-to-edge 기본 활성화 — `enableEdgeToEdge()` + Scaffold inset 패턴 사용.
- **POST_NOTIFICATIONS** (33+): 런타임 권한. `rememberNotificationPermission()` 헬퍼.
- **SCHEDULE_EXACT_ALARM** (31+): user-grantable. `canScheduleExactAlarms()` 체크 + inexact fallback (`NotificationSchedulerImpl` 처리).
- **Android Photo Picker** (33+): 시스템 picker. `READ_MEDIA_IMAGES` 별도 권한 불필요 (Photo Picker 자체 우회).

### 8.5 자주 참고하는 파일
- **String 매핑**: `app/src/main/res/key_mapping.txt` (한글 ↔ romanized)
- **도메인 enum 정적 데이터**: `core/domain/src/main/kotlin/com/oq/barnote/core/domain/`
- **enum → 표시 텍스트 매핑**: `app/src/main/java/com/oq/barnote/extension/DomainStrings.kt`
- **컬러 토큰**: `core/designsystem/src/main/res/values{,-night}/colors.xml`
- **dp 토큰**: `core/designsystem/src/main/java/com/oq/barnote/core/designsystem/Dimens.kt`
- **앱 상수 (URL/PrefsKey)**: `app/src/main/java/com/oq/barnote/Constants.kt`
- **NavHost / Destinations**: `app/src/main/java/com/oq/barnote/ui/navigation/`

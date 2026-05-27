# Android BarNote Project Context & Rules

## 1. Project Overview
- **Goal**: 바코드 스캔 및 AI 이미지 인식을 통해 사용자가 빠르고 쉽게 테이스팅 노트를 작성하고 관리할 수 있는 안드로이드 애플리케이션입니다. (iOS BarNote 앱의 안드로이드 버전)
- **Role**: 당신은 **Senior Android Tech Lead**입니다. **Clean Architecture**와 **UDF(Unidirectional Data Flow)** 원칙을 엄격하게 준수해야 합니다. 코드는 가독성이 높고, 모듈화되어 있어야 하며, 유지보수하기 쉽고, 커뮤니티의 최신 컨벤션에 맞아야 합니다.

## 2. Tech Stack & Environment
- **Platform**: Android 10 (API 29) 이상
- **Language**: Kotlin 2.2+ (최신 안정화 버전)
- **Architecture**: MVI / UDF (ViewModel + StateFlow) 기반 Clean Architecture
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Build Tool**: Gradle (Kotlin DSL), Version Catalogs (`libs.versions.toml`)
- **Dependency Injection**: Hilt
- **Asynchronous**: Coroutines & Flow

## 3. Project Structure & Architecture (CRITICAL)
최신 안드로이드 모듈화 트렌드(Now in Android 권장 구조)를 따릅니다.
- **`app`**: 앱 엔트리 포인트, Hilt 컴포넌트 루트, 최상위 네비게이션(`NavHost`, `AppState`).
- **`feature:*` (UI & Feature Layer)**:
  - 하위 도메인별 모듈: `feature:home`, `feature:mypage`, `feature:search`, `feature:settings` 등.
  - **MVI/UDF 컨벤션**: 각 화면은 `@HiltViewModel`로 주입받는 `ViewModel`을 가집니다. `UiState`와 `UiEvent(Action)`을 통해 단방향으로 상태를 관리하며 `StateFlow`를 통해 Compose에 노출합니다.
  - **네비게이션**: Jetpack Navigation Compose (또는 Type-Safe Navigation)를 사용하며, 각 피쳐는 자체 네비게이션 그래프를 정의하고 루트로 이벤트를 전달합니다.
- **`core:domain` (Business Logic Layer)**:
  - 핵심 엔티티(`Models`)와 Repository 프로토콜(Interface) 및 `UseCase` 정의. Android 프레임워크 종속성(`android.*`)을 가지지 않는 순수 Kotlin 모듈입니다.
- **`core:data` (Data Layer)**:
  - **Repositories**: API 구현, DTO를 Domain 모델로 매핑. Offline-first를 위한 로컬 캐싱 등을 처리합니다.
- **`core:network` / `core:database` / `core:datastore`**:
  - API 통신(Retrofit/OkHttp 또는 Ktor), 로컬 DB(Room), 설정(DataStore) 관련 세부 구현체.
- **`core:designsystem`**:
  - 프로젝트 전반에 사용되는 공통 컴포넌트, 컬러, 타이포그래피(`MaterialTheme`), 커스텀 UI(Skeleton 등)를 정의합니다.

## 4. Global State & UI Guidelines
- **Global App State (`AppState`)**:
  - 앱 전체에 영향을 미치는 상태(네트워크 상태, 글로벌 로딩, 에러 스낵바 처리 등)는 최상위 네비게이션 레벨의 `AppState` 객체나 전역 `ViewModel`을 통해 관리합니다.
- **Design System**:
  - 하드코딩된 크기, 여백(padding), 색상, 문자열을 절대 사용하지 마세요. `core:designsystem`에서 정의한 테마 변수(`MaterialTheme.colorScheme`, `MaterialTheme.typography`) 및 리소스(`R.dimen`, `R.string`)를 사용합니다.
- **Localization**:
  - 모든 문자열은 `res/values/strings.xml`을 통해 관리 및 다국어 처리되어야 합니다. Compose UI 내에서는 반드시 `stringResource(id = R.string.XXX)`를 사용합니다.

## 5. Logic & Feature Implementation Rules
- **Authentication**:
  - 사용자 인증은 **Auth0 모듈(Auth0 Android SDK)**을 사용하여 구현합니다.
  - 사용자 인증 상태 및 토큰 갱신 로직은 Auth0 연동을 기반으로 구성하며, 필요한 정보는 DataStore와 Network Interceptor를 활용해 관리합니다.
  - 로그인 필요 시 이벤트를 발생시켜 최상위 네비게이션에서 Auth0의 Web Auth 기반 로그인 화면으로 라우팅되도록 합니다.
- **Concurrency & Streams**:
  - 비동기 처리와 데이터 스트림은 **Kotlin Coroutines** 및 **Flow**를 독점적으로 사용합니다.
  - Compose UI 레이어에서는 상태를 수집할 때 반드시 `collectAsStateWithLifecycle()`을 사용하여 생명주기를 고려한 Flow 수집을 해야 합니다.
- **Subscriptions**:
  - 무료/유료 구독 모델에 따른 기능 제한 등은 Domain 로직 내(`UseCase`)에서 처리하여 일관성을 유지합니다.

## 6. Constraints & Code Preferences
- **Third-Party Dependencies**: 
  - 새로운 라이브러리 추가는 반드시 루트의 `gradle/libs.versions.toml` (Version Catalog)에 먼저 정의한 뒤, 각 모듈의 `build.gradle.kts`에 추가해야 합니다. 승인 없이 임의로 구방식의 의존성을 추가하지 마세요.
- **Language Requirements**: 
  - 모든 코드 주석, 사고 과정, 계획 설명 등 커뮤니케이션은 **한국어(Korean)**로 작성합니다.
  - 반면, 모든 코드 심볼(변수, 메서드, 클래스, 인터페이스, 모듈명 등)은 표준 **English**로 작성합니다.

## 7. AI Session Guidelines (AI 세션 지침)
새로운 AI 세션이 시작될 때 다음 지침을 숙지하고 작업을 시작하세요:
- **iOS 마이그레이션 컨텍스트**: iOS의 `Localizable.xcstrings` 등을 안드로이드 `strings.xml`로 변환할 때, 한글 키 등에서 충돌이 발생하지 않도록 로마자 변환(Romanization) 및 해시 기반의 고유 식별자를 사용하여 매핑했습니다 (`key_mapping.txt` 참조).
- **네트워크 구조 (iOS vs Android)**: iOS의 커스텀 `NetworkClient` 패턴은 안드로이드 표준인 **Retrofit2 + OkHttp3** 조합으로 대체되었습니다. Hilt를 통해 `NetworkModule`에서 전역적으로 주입되며 `AuthInterceptor`를 통해 토큰 헤더를 관리합니다.
- **데이터 모델링**: iOS의 `Codable` 및 `SwiftData` 모델들은 안드로이드에서 `data class`와 `kotlinx.serialization`(`@Serializable`)을 활용하여 매핑합니다.
- **모듈 추가 규칙**: 새로운 모듈 생성 시 반드시 `kotlin-kapt` 플러그인과 `hilt.android` 의존성을 `build.gradle.kts`에 선언하여 Hilt 의존성 주입이 누락되지 않도록 주의해야 합니다.
- **리소스 마이그레이션 결과** (2026-05-27):
  - **컬러**: iOS `Assets.xcassets`의 `*.colorset`(srgb 0~1 float)을 `core:designsystem`의 `res/values/colors.xml` + `res/values-night/colors.xml`로 변환했습니다. 다크모드 대응 포함 (10개 토큰: `accent_color`, `accent_secondary`, `background_primary`, `disabled_button`, `disabled_text`, `divider`, `surface_primary`, `surface_secondary`, `text_primary`, `text_secondary`).
  - **이미지**: iOS `imageset`은 `app/src/main/res/drawable-xxhdpi/`로(iOS @3x 매핑), JPEG 형식이었던 `onboarding_02/03`은 `.jpg`로 확장자 정정. AppIcon master(1024)는 `drawable-nodpi/app_icon.png`에 보관(런처 아이콘은 추후 Android Studio Image Asset Studio로 생성).
  - **포맷 placeholder**: iOS의 `%@`는 `%s`, `%lld`는 `%d`로 변환됨. 같은 string에 정수 placeholder가 2개 이상 있을 때는 positional(`%1$d`, `%2$d`)을 사용합니다 (예: `image_viewer_page_count`).
  - **다국어**: iOS `zh-Hans` → Android `values-zh-rCN`, `zh-Hant` → `values-zh-rTW`로 매핑. 모든 locale에서 키 셋 일관성을 유지(누락 시 영어 폴백 방지 차원에서 default 또는 빈 값으로라도 채워둘 것).
- **Firebase 설정**: iOS의 `GoogleService-Info.plist`는 Android에서 직접 변환 불가. Firebase Console에서 Android 앱(`com.oq.barnote`)을 별도 등록해 받은 `google-services.json`을 `app/` 디렉토리에 배치해야 합니다. `.gitignore`에 이미 등록되어 있습니다.
- **Auth0 설정** (✅ 완료): `local.properties` (`auth0.domain` / `auth0.clientId` / `auth0.scheme`) → `app/build.gradle.kts` 의 `buildConfigField` + `manifestPlaceholders` 로 주입. `BuildConfig.AUTH0_*` 상수를 통해 Hilt `@Named` qualifier 로 [Auth0AuthStore] 에 전달. 기존 `app/res/values/auth0.xml` 은 제거됨. 콜백 intent-filter (`<data android:host="${auth0Domain}" .../>`) 는 `AndroidManifest.xml` 의 MainActivity 에 등록되어 있음.
- **Constants 마이그레이션 결과** (iOS `Constants.swift` 대응):
  - **`app/Constants.kt`**: URL(`BASE_URL`, `WEB_BASE_URL` 등), SharedPreferences/DataStore 키, 페이징 수치 등 단순 상수 (`Constants.S`, `Constants.N`).
  - **`core:designsystem/Dimens.kt`**: spacing/padding/icon size 등 dp 상수 (iOS `C.V` 대응).
  - **`core:designsystem/Palettes.kt`**: `barNotePalette()` Composable 함수 (iOS `Palette.btnPalette` 대응). `colors.xml` 의 컬러를 `core:oqcore` 의 `Palette` data class 로 묶음.
  - **`core:domain`** (순수 Kotlin): 모든 도메인 enum 을 `kotlinx-serialization` 기반으로 정의 (`ProductType`, `ProductStyle`, `GrapeVariety`, `ProductDetailInfo`, `PublicScope`, `Flavor`, `NoteDetail` + `Feeling`, `ProductOrderByKey`, `NoteOrderByKey`). 각 enum 은 `rawValue: Int`/`String` 과 `fromRaw()` 폴백 메서드, 이모지 등 정적 데이터만 포함합니다. **UI 표시용 텍스트는 도메인에 두지 않습니다.**
  - **`app/extension/DomainStrings.kt`**: 위 enum 들에 대한 `@StringRes Int` 매핑 (`titleRes()`, `detailRes()` 등) + `@Composable` 헬퍼(`title()`, `detail()`). 향후 feature 모듈이 enum 표시 텍스트를 공유해야 한다면 string resource 와 함께 `core:designsystem` 으로 이전을 고려하세요. 현재는 strings.xml 이 app 모듈에 있어 cross-module 참조 제약 때문에 app 에 둔 상태입니다.
  - **`core:oqcore/util/Country.kt`**: ISO alpha-2 코드 → 국기 이모지 + 지역명 변환 유틸 (iOS `Country` 대응). `Locale` 을 인자로 받는 형태로 변경. AppLanguage → Locale 변환은 `AppLanguage.toLocale()` 사용.
  - **`core:oqcore/models/OQTypes.kt`** 의 `AppLanguage` 에 `USER_DEFAULTS_KEY` 상수와 `toLocale()` 변환 함수를 추가했습니다.
- **enum → @StringRes 매핑 위치 컨벤션**: 도메인 enum 의 `title`/`detail` 같은 표시용 텍스트는 **순수 Kotlin 모듈인 `core:domain` 에 두지 않습니다.** 표시 텍스트는 안드로이드 리소스 의존이므로 app 또는 designsystem 의 extension 으로 분리합니다.
- **도메인 모델 & Repository 마이그레이션 결과** (iOS `App/Sources/Domain` + `App/Sources/Data` 대응):
  - **모델 (`core:domain`)**: `User`, `UserInfo`, `Product` (+ `ProductDetailsMap`), `ProductInfo` (+ `TastedProductInfo`), `Note`, `NoteInfo` (+ `UnratedNoteAlert`), `HomeInfo`, `MyPageInfo`, `NoteDraft`, `ProductDraft`, `NoteReservation`, `Report`. 모두 `data class` + `@Serializable`, snake_case 필드는 `@SerialName` 으로 매핑.
  - **iOS `UUID` → Android `String`**: 서버에서 UUID 문자열로 받기 때문에 그대로 String 으로 보관. 필요 시 `java.util.UUID.fromString()` 으로 변환.
  - **iOS `Date` → Android `String` (ISO8601)**: 도메인은 raw 문자열을 유지하고, 화면 표시 시점에 `Instant.parse` 등으로 변환.
  - **`ProductDetailsMap`**: iOS 의 mixed-type (String/Int/Double/null) 값을 모두 String 으로 정규화하던 custom `Codable` 을 `KSerializer` + `JsonObject` 기반으로 동일하게 구현.
  - **Repository 인터페이스 (`core:domain.BarNoteRepository`)**: iOS `BTNRepository` 프로토콜의 모든 메서드를 `suspend fun ...: Result<T>` 시그니처로 정의. 에러는 Kotlin 표준 `Result<T>` 의 `Throwable` 로 표현하며, 실제 인스턴스는 OQCore 의 `CommonError` (Network / ApiError / Decoding 등).
  - **Repository 구현체 (`core:data.BarNoteRepositoryImpl`)**: iOS `BTNRepositoryLive` 와 동일한 로직. 인증 여부에 따른 `/api/` prefix 분기는 `AuthStore.hasCredentials()` 로 결정, Retrofit 의 `@Url` 동적 URL 로 전달합니다. `APIResponse<T>` 언래핑, `safeCall { }` 헬퍼로 throw 패턴을 `Result<T>` 로 wrap.
  - **API 서비스 (`core:network.BarNoteApi`)**: Retrofit + kotlinx-serialization. snake_case body 는 `Map<String, Any?>` 로 전달. multipart 업로드는 `MultipartBody.Part` 사용.
  - **`AuthStore` (`core:domain`)**: 인증 토큰 헤더 추상화. 현재는 `NoOpAuthStore` stub 으로 바인딩되어 있고, Auth0 SDK 통합 시점에 실제 구현체로 교체 필요. `AuthStoreHeadersProvider` 가 OQCore 의 `AuthInterceptor.HeadersProvider` 로 어댑팅.
  - **NetworkModule 수정**: 잘못된 `https://api.barnote.com/` → `https://api.barnote.net/` 로 정정. JakeWharton 컨버터 import 를 retrofit 2.11+ 공식 `retrofit2.converter.kotlinx.serialization.asConverterFactory` 로 교체.
  - **`Report` 위치 정정**: iOS 는 `App/Domain/Models/Report.swift` 인데 Android 에서는 `core:oqcore` 에 있었음. `core:domain` 으로 이전했고 OQCore 에서는 삭제.
  - **`BarNoteApp`**: Hilt 진입점 Application 클래스 (`@HiltAndroidApp`) 추가, `AndroidManifest.xml` 에 등록.
- **Repository 에러 처리 패턴**: 도메인 Repository 메서드는 `suspend fun ...: Result<T>` 형태. 실패의 cause 는 `Result.exceptionOrNull() as? CommonError` 로 캐스팅해 분류합니다 (`CommonError.Network`, `CommonError.ApiError`, `CommonError.Decoding` 등).
- **AuthStore 구현 완료** (✅): `Auth0AuthStore` 가 Auth0 Android SDK 의 `SecureCredentialsManager` + `WebAuthProvider` 기반으로 동작합니다. `NoOpAuthStore` 는 더 이상 바인딩되지 않지만 (테스트/preview 용으로) 클래스 자체는 보존. 로그인 플로우 (`WebAuthProvider.login(...).start(...)`) 를 위해서는 별도 ViewModel 에서 SDK 호출 후 `Auth0AuthStore.saveAuth0Credentials(creds)` 호출.
- **Dependencies 마이그레이션 결과** (iOS `App/Sources/Dependencies/*` 대응):
  - **`AppController`** (`core:oqcore.util`): iOS `AppController.shared` 싱글톤 → Hilt `@Singleton` + Kotlin `StateFlow`/`SharedFlow` 로 변경. `globalLoading`, `toastEvent` 채널 제공.
  - **`BlockedUsersStore`** (`core:domain` 인터페이스 + `core:data.blocked.BlockedUsersStoreImpl`): iOS `UserDefaults` JSON 저장 → Android `DataStore Preferences` `stringSetPreferencesKey` 로 단순화. 차단 추가/해제/조회/Flow 제공.
  - **`FcmTokenProvider`** (`core:domain` 인터페이스 + `core:data.fcm.FcmTokenProviderImpl`): iOS `Messaging.delegate` → Android `FirebaseMessaging.getInstance().token` + `FirebaseMessagingService.onNewToken` 콜백. `BarNoteMessagingService` 가 Hilt 주입으로 토큰을 `FcmTokenProvider.onTokenRefresh()` 에 전달.
  - **`MediaAttachmentPicker`** (`core:domain` 인터페이스만): 구현체는 Activity 컨텍스트 의존이라 feature 레이어에서 `ActivityResultContracts.PickVisualMedia` 기반으로 작성 예정. Options/Type 모델만 도메인에 정의.
  - **`NotificationScheduler`** (`core:domain` 인터페이스 + `core:data.notification.NotificationSchedulerImpl`): iOS `UNUserNotificationCenter` + `UNCalendarNotificationTrigger` → Android `NotificationManager` + `AlarmManager.setExactAndAllowWhileIdle`. 알림 탭 이벤트는 `NotificationAlarmReceiver` + `NotificationTapReceiver` (BroadcastReceiver, `@AndroidEntryPoint`) 가 처리해 `SharedFlow<NotificationEvent>` 로 broadcast. 이벤트 타입은 `TappedReservation`/`TappedRemotePush(NewFollower/NewNote)` 로 iOS 와 1:1.
  - **`RepositoryDependency`/`AuthStoreDependency`**: 이전 작업에서 이미 Hilt `@Binds` 로 처리됨.
- **추가된 라이브러리**:
  - `androidx.datastore:datastore-preferences` — BlockedUsersStore
  - `com.google.firebase:firebase-bom` + `firebase-messaging-ktx` — FCM
  - `com.google.gms.google-services` plugin (카탈로그만 등록, 실 적용은 `google-services.json` 추가 후)
- **AndroidManifest 추가 항목**:
  - 권한: `POST_NOTIFICATIONS` (13+), `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` (12+), `c2dm.permission.RECEIVE`
  - 서비스: `BarNoteMessagingService` (`com.google.firebase.MESSAGING_EVENT` filter)
  - 리시버: `NotificationAlarmReceiver`, `NotificationTapReceiver`
- **Dependencies 마이그레이션 TODO 진행 상황**:
  1. ⏳ `google-services.json` 추가 후 `app/build.gradle.kts` 에 `id("com.google.gms.google-services")` plugin 활성화 (사용자 직접 작업 필요).
  2. ✅ `MediaAttachmentPicker` 구현: `app/ui/picker/PhotoPicker.kt` (`rememberPhotoPicker`) + `ComposeMediaAttachmentPicker.kt` (`rememberComposeMediaAttachmentPicker` 가 도메인 인터페이스를 충족).
  3. ✅ FCM data payload 파싱: `BarNoteMessagingService.onMessageReceived` 에서 `new_follower`/`new_note` 처리 후 `NotificationSchedulerImpl.emitEvent` 호출.
  4. ✅ `NotificationScheduler.requestAuthorization` 권한 요청: `app/ui/permission/NotificationPermission.kt` (`rememberNotificationPermission`) 가 Accompanist Permissions 로 13+ 권한 요청 헬퍼 제공.
  5. ✅ Auth0 SDK 통합: `Auth0AuthStore` 구현 완료.
- **Data/Stores 마이그레이션 결과** (iOS `App/Sources/Data/Stores/*` 대응):
  - **`AuthStore` 인터페이스 확장**: 기존 `hasCredentials`/`authorizationHeaders` 만 있던 인터페이스를 iOS 와 동일하게 `currentCredentials`/`save`/`clear(clearWebSession)` + `isLoggedIn: StateFlow<Boolean>` 까지 확장. iOS `appController.isLogin` 은 `AuthStore.isLoggedIn` StateFlow 로 대체.
  - **`Credentials` 모델** (`core:domain`): Auth0 SDK 의 `Credentials` 와 동등. `accessToken`/`refreshToken`/`idToken`/`expiresAt`(epoch millis)/`scope`/`tokenType`.
  - **`NoOpAuthStore` 갱신**: 메모리 보관 + isLoggedIn StateFlow 발행. 영속화/만료 검증 없음. Auth0 SDK 통합 후 교체 필요.
  - **`ReservationStore`** (`core:domain` 인터페이스 + `core:data.reservation.ReservationStoreImpl`):
    - iOS `UserDefaults` + JSON → DataStore Preferences 의 `stringPreferencesKey` 에 JSON-encoded `List<NoteReservation>` 저장.
    - 기본 시간은 `LocalTime` ("HH:mm" 문자열) 로 저장, 기본값 10:00.
    - `scheduleReservation(product)` 가 기존 중복 취소 → 다음날 기본 시간 산출 → 알림 등록 → 저장까지 한 번에 처리 (iOS 와 동일 흐름).
    - `saveDefaultTime` 시 기존 예약들도 새 시간으로 재스케줄링 (날짜는 유지, 시/분만 교체).
  - **`UserStore`** (`core:domain` 인터페이스 + `core:data.user.UserStoreImpl`):
    - iOS `actor` → Kotlin `@Singleton` + `Mutex.withLock` 으로 동시성 보호.
    - 사용자 / 노트 카운트 / 즐겨찾기 ID Set / 팔로워 카운트 / 리뷰 필요 상품 캐시.
    - `noteCount`, `neededReviewProduct`, `followerCount` 는 `StateFlow` 로 UI 가 즉시 반영 가능.
    - 구독 상태 (iOS StoreKit `Transaction.updates`) 는 현재 stub. Google Play Billing Library 통합 후 교체.
    - `renewUser` 는 `repository.getMyPage()` 를 호출해 사용자/카운트/즐겨찾기 ID 를 한 번에 갱신.
- **Stores 마이그레이션 TODO 완료**:
  1. ✅ AuthStore/UserStore 자동 연동: `core:data.di.CoroutineScopeModule` 에서 `@ApplicationScope CoroutineScope` 를 제공하고, `core:data.auth.AuthSessionObserver` 가 `AuthStore.isLoggedIn` 을 구독해 false 전환 시 `UserStore.clear()` 호출. `BarNoteApp.onCreate()` 에서 `start()` 호출.
  2. ✅ Google Play Billing: `core:data.billing.BillingManager` 가 `BillingClient` + `PurchasesUpdatedListener` 를 wrap. `UserStoreImpl` 의 구독 stub 들은 BillingManager 호출로 교체됨. 결제 화면에서 `BillingManager.purchasesUpdates` Flow 를 collect 해 결과 처리.
  3. ✅ `Auth0AuthStore` 구현 완료 (위 항목 참조).
- **Presentation/Shared UI 컴포넌트 마이그레이션 결과** (iOS `App/Sources/Presentation/Shared/*` 대응):
  - **`core:oqcore/ui/component`** (OQ prefix 의존 컴포넌트):
    - `OQImageView`: Coil `SubcomposeAsyncImage` 기반. 로딩 중 `SkeletonView`, 실패 시 fallback ImageVector.
    - `OQGridImagesView`: 1/2/3/4+ 장 분기 그리드. 4+ 의 경우 `+N` 오버레이.
    - `InfoTagView`: 작은 정보 태그. `Normal`/`Material`/`Accent` 3가지 스타일.
  - **`core:oqcore/util/RelativeTime`**: iOS `Date.formattedByNow` 와 동일한 "방금 전 / 5분 전" 등 상대 시간 포맷터. `Instant.parse(ISO8601)` 사용. OQCore `strings.xml` 의 `date_*` 키 사용.
  - **`core:designsystem/component`** (도메인 의존 없는 atomic):
    - `RatingView` / `RatingInputView`: 5개 별 + 드래그 입력 (1~10 raw, 0.5 단위). HapticFeedback 통합.
    - `PlaceholderPage`: 빈 화면용 (icon + title + message).
    - `ViewAllButton`: "View All" + 우측 chevron 원형 배경.
    - `InfoPopOver`: iOS popover → Android `AlertDialog` 로 단순화. title + (title, detail) 리스트.
  - **`app/ui/component`** (BTN 도메인 의존):
    - `BTNImage` / `BTNGridImages`: `Constants.S.IMAGE_BASE_URL` prefix 자동 부착 wrapper.
    - `FlavorSummaryChips` / `FlavorCountChips`: 향미 칩 묶음 (FlowRow 기반).
    - `NoteProductInfoSection`, `NotePublicToggleSection`, `NoteRatingSelectorSection`, `NoteAttachmentSection`, `NoteFeelingGrid`, `NoteDetailSlider`, `NoteDetailExpandable`, `NoteFlavorSelector`: 노트 작성 화면용 8종 입력 컴포넌트.
    - `NoteDetailSummary`: 노트 디테일 평가 요약 (막대 그래프).
    - `UserRow` (+ `UserRowSkeleton`), `ProductRow`, `ProductListRow`, `NoteRow`, `NoteListRow`, `NoteDetailRow`: 6종 row 컴포넌트.
    - `ProductTypeFilter`: 가로 스크롤 칩 필터 ("전체" + 모든 `ProductType`).
- **UI 컴포넌트 매핑 패턴**:
  - **iOS `task(id:)` + `@Dependency` 호출**: ViewModel 레이어에서 미리 계산한 값을 파라미터로 전달 (예: `NoteRow(info, isBlocked = ...)`).
  - **iOS `popover`**: Compose 에 등가물이 없어 `AlertDialog` 또는 `ModalBottomSheet` 로 대체.
  - **iOS `OQHapticService.selection.run()`**: Compose `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`.
  - **iOS `Image(systemName:)`**: SF Symbol → Material Icons (`Icons.Filled.*`). `core:designsystem/DomainIcons.kt` 에 매핑 모음.
  - **iOS `FlowLayout`**: `androidx.compose.foundation.layout.FlowRow` (실험 → 안정화).
  - **iOS `@MainActor @Observable AppController.shared`**: Hilt `@Singleton AppController` + `StateFlow`/`SharedFlow`.
- **UI 컴포넌트 TODO 진행 상황**:
  1. ✅ `OQThumbnailsView` 다중 이미지 + add/remove: `core:oqcore/ui/component/OQThumbnailsView.kt` 작성. `LazyRow` + Coil `AsyncImage` 기반. `NoteAttachmentSection` 이 이를 직접 사용.
  2. ⏳ `TipContainer` (iOS `TipKit`) — 안드로이드 등가물 없어 보류. 필요 시 자체 `TipPopup` 구현 또는 외부 라이브러리(예: `compose-tooltip`) 도입.
  3. `ThumbnailsView.swift` 는 iOS 에서도 빈 파일이라 마이그레이션 안 함.
  4. ⏳ BTN 컴포넌트 위치 (`app/ui/component`) → `core:designsystem` 이전: strings.xml 키 cross-module 정리 필요해 별도 작업.
  5. ⏳ 한글 라벨 하드코딩 → `stringResource()`: 다국어 일관성을 위해 별도 작업.
- **추가 라이브러리** (이번 작업에서 도입):
  - `com.auth0.android:auth0` (이미 카탈로그) — Auth0 인증 SDK
  - `androidx.security:security-crypto` (이미 카탈로그) — EncryptedSharedPreferences (Auth0 토큰 저장)
  - `com.android.billingclient:billing-ktx:7.1.1` — Google Play Billing
  - `com.google.accompanist:accompanist-permissions:0.34.0` — Compose 권한 헬퍼
- **AndroidManifest 추가 권한**: `com.android.vending.BILLING` (구독), Auth0 콜백 intent-filter (`<data android:host="${auth0Domain}" .../>` 형식, MainActivity 에 등록).
- **남은 TODO** (사용자 작업 필요 또는 별도 작업):
  - `google-services.json` 추가 + `google-services` plugin 활성화 (사용자가 Firebase Console 에서 다운로드).
  - 로그인 화면 ViewModel: `WebAuthProvider.login(auth0).start(...)` 호출 후 받은 `Credentials` 를 `Auth0AuthStore.saveAuth0Credentials(creds)` 로 저장.
  - 결제 화면 ViewModel: `BillingManager.purchasesUpdates` Flow 를 collect 해 구매 완료/실패 처리.
  - 다국어 + designsystem 이전 (strings.xml 재배치 + 컴포넌트 위치 정리).
- **SF Symbol → Material Icon 매핑**: `core:designsystem/DomainIcons.kt` 에서 도메인 enum 의 `icon()` extension 으로 제공합니다. `material-icons-extended` 라이브러리 의존성을 `core:designsystem` 에 추가했습니다. 매핑 요약:
  - `ProductType`: `wineglass.fill` → `Icons.Filled.WineBar` / `mug.fill` → `Icons.Filled.SportsBar`
  - `PublicScope`: `lock.fill` → `Lock` / `person.2.badge.key.fill` → `Group` / `person.3.fill` → `Groups`
  - `ProductDetailInfo`: `tag.fill` → `Sell` / `leaf` → `Eco` / `building.2.fill` → `Business` / `globe` → `Public` / `drop.fill` → `WaterDrop` / `gauge.medium` → `Speed`
  - 1:1 대응되지 않는 아이콘(예: `person.2.badge.key.fill` 의 사람+키 조합)은 의미상 가장 가까운 Material Icon 으로 치환했습니다.
- **규칙의 자발적 업데이트 허용**: AI는 세션을 진행하면서 미래의 AI 세션이나 프로젝트 협업에 도움이 될 만한 유용한 지침(아키텍처 패턴, 트러블슈팅 결과, 새로운 컨벤션 등)을 발견한다면, **사용자의 명시적 요청이 없더라도 언제든지 자발적으로 이 `RULES.md` 파일에 해당 지침을 추가 및 업데이트**할 권한과 의무가 있습니다.

---

## 8. Quick Reference for New AI Sessions

새 세션이 시작될 때 빠르게 프로젝트 구조를 파악하기 위한 cheatsheet.

### 8.1 모듈 의존성 트리

```
app
 ├─ core:domain      (pure Kotlin, no Android deps)
 ├─ core:oqcore      (Android library, Compose, Retrofit/OkHttp/Json)
 ├─ core:network     ← core:domain, core:oqcore
 ├─ core:data        ← core:domain, core:network, core:oqcore
 ├─ core:designsystem ← core:domain, core:oqcore
 └─ feature:*        (아직 미작성. core:* 만 의존, feature 간 직접 의존 X)
```

- **`core:domain`**: 의존성 없음 (kotlinx-coroutines + kotlinx-serialization-json 만). `androidx.*` import 금지.
- **`core:oqcore`**: `OQ` prefix 클래스 + 네트워크 베이스(`NetworkModule`/`AuthInterceptor`) + 공용 UI 컴포넌트.
- **`core:designsystem`**: Material 3 + Compose. 도메인 enum → `@StringRes` extension, `Dimens.kt`, `Palettes.kt`, `DomainIcons.kt`.
- **`core:network`**: Retrofit `BarNoteApi` 인터페이스 + Hilt `ApiModule`.
- **`core:data`**: Repository / Store 구현체 + Hilt 바인딩 모듈.

### 8.2 새 코드를 어디에 둘지 결정하는 가이드

| 추가할 코드 | 위치 |
|---|---|
| 새 도메인 모델 (`data class`) | `core:domain` |
| 새 enum (rawValue + emoji 등 정적 데이터) | `core:domain` |
| 도메인 enum → `@StringRes` 매핑 | `app/extension/DomainStrings.kt` |
| 도메인 enum → `ImageVector` 매핑 | `core:designsystem/DomainIcons.kt` |
| Repository 인터페이스 | `core:domain` |
| Repository 구현체 + Hilt `@Binds` | `core:data` (+ `core:data/di/`) |
| 새 Retrofit endpoint | `core:network/BarNoteApi.kt` |
| 단일 화면용 ViewModel | feature 모듈 (작성 시) 또는 `app` 임시 |
| 공용 atomic UI 컴포넌트 (텍스트 외부 주입) | `core:designsystem/component/` |
| 도메인 모델을 직접 받는 UI 컴포넌트 | `app/ui/component/` (추후 designsystem 이전 후보) |
| `OQ` prefix UI 컴포넌트 (앱 무관) | `core:oqcore/ui/component/` 또는 `views/` |
| 권한/Picker 등 Activity 의존 헬퍼 | `app/ui/permission/`, `app/ui/picker/` |
| 새 Hilt 모듈 | 같은 모듈의 `di/` 패키지 |

### 8.3 iOS → Android 매핑 cheatsheet

| iOS | Android |
|---|---|
| `UUID` | `String` (서버 응답 그대로) |
| `Date` | `String` (ISO8601). 표시 시 `Instant.parse(...)` |
| `Codable` + custom `init(from:)` | `@Serializable` + custom `KSerializer` (fallback 처리) |
| `Result<T, CommonError>` | Kotlin `Result<T>` (실패는 `CommonError` throw → `Result.failure` wrap) |
| `actor` (Swift) | `@Singleton` + `Mutex.withLock` |
| `@Dependency(\.x)` (TCA) | Hilt `@Inject` constructor |
| `@MainActor @Observable` 싱글톤 | Hilt `@Singleton` + `StateFlow`/`SharedFlow` |
| `@Published` 프로퍼티 | `MutableStateFlow` / `StateFlow` |
| `AsyncStream<T>` | `Flow<T>` / `SharedFlow<T>` |
| `UserDefaults` | `DataStore Preferences` (값) 또는 `EncryptedSharedPreferences` (비밀) |
| `Keychain` | `EncryptedSharedPreferences` (`androidx.security:security-crypto`) |
| `.localized` String | `stringResource(R.string.xxx)` (key 매핑은 `app/res/key_mapping.txt`) |
| SF Symbol `Image(systemName:)` | Material Icons `Icons.Filled.*` (`material-icons-extended`) |
| SwiftUI `popover` | `AlertDialog` / `ModalBottomSheet` |
| SwiftUI `FlowLayout` | `androidx.compose.foundation.layout.FlowRow` |
| `OQHapticService.selection.run()` | `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` |
| iOS `task(id:) { @Dependency... }` | ViewModel 에서 미리 계산 → Composable 파라미터 |
| `UNUserNotificationCenter` + `UNCalendarNotificationTrigger` | `NotificationManager` + `AlarmManager.setExactAndAllowWhileIdle` |
| `Messaging.delegate` (FCM) | `FirebaseMessagingService.onNewToken` / `onMessageReceived` |
| `StoreKit Transaction.updates` | Google Play `BillingClient` + `PurchasesUpdatedListener` |
| Auth0 iOS `CredentialsManager` | Auth0 Android `SecureCredentialsManager` |

### 8.4 Hilt 관련 컨벤션

- **`@HiltAndroidApp`**: `BarNoteApp` (앱 진입점) — 이미 적용됨.
- **`@AndroidEntryPoint`**: Activity, Fragment, BroadcastReceiver, Service. 현재 적용된 곳:
  - `BarNoteMessagingService` (FCM)
  - `NotificationAlarmReceiver`, `NotificationTapReceiver`
- **`@Binds` 패턴**: 인터페이스 구현체 바인딩. `core:data/di/` 의 모듈들 참조 (`RepositoryModule`, `AuthModule`, `Auth0BindingModule`, `DependenciesModule`, `StoresModule`).
- **`@Provides` 패턴**: SDK 객체 (Retrofit, OkHttp, Auth0, Json 등) 생성.
- **`@Named` qualifier**: Auth0 도메인/클라이언트ID 처럼 동일 타입을 구분 (`@Named("auth0Domain")`).
- **`@ApplicationScope`**: 앱 lifecycle 동안 살아있는 `CoroutineScope` (커스텀 qualifier, `core:data/di/CoroutineScopeModule`).
- **`@ApplicationContext`**: Application Context 주입 (Hilt 기본 제공).

### 8.5 빌드 시스템 특이사항

- **`buildConfig = true`**: `app/build.gradle.kts` 에서 활성화됨. `Auth0AuthStore` 가 `BuildConfig.AUTH0_*` 사용.
- **`kotlinx-serialization` plugin**: `core:domain`, `core:network`, `core:data`, `core:oqcore` 에 적용됨. 새 `@Serializable` 클래스 추가 시 plugin 적용 모듈인지 확인.
- **`compose = true`**: `app`, `core:oqcore`, `core:designsystem` 에 활성화. 다른 모듈에서 Composable 작성 시 활성화 필요.
- **`kotlin-kapt`**: Hilt 사용 모듈마다 적용. Hilt 의존성이 있으면 `kapt(libs.hilt.compiler)` 도 함께.
- **`libs.versions.toml`**: 모든 의존성은 여기에 등록 후 사용. 직접 `implementation("...")` 형태 금지.
- **`local.properties`**: VCS 제외. Auth0 키 (`auth0.domain`, `auth0.clientId`, `auth0.scheme`) 보관.

### 8.6 알아두면 좋은 SDK 제약

- **`minSdk = 29`**: `java.time` (`Instant`, `LocalTime`, `Duration`) desugaring 없이 직접 사용 가능. `Locale("ko")` 형태 OK.
- **`compileSdk = 34`**: Predictive back, `Build.VERSION_CODES.UPSIDE_DOWN_CAKE` 사용 가능.
- **Notification 권한 (`POST_NOTIFICATIONS`)**: 33+ 에서 런타임 권한 필요. `rememberNotificationPermission()` 헬퍼 사용.
- **Exact alarm 권한 (`SCHEDULE_EXACT_ALARM`)**: 31+ 에서 user-grantable. `alarmManager.canScheduleExactAlarms()` 로 체크 필요. `NotificationSchedulerImpl` 이 자동으로 inexact fallback.
- **Android Photo Picker**: 33+ 시스템 picker, 그 이전은 자동으로 호환 모드. `READ_MEDIA_IMAGES` 권한 별도로 안 받아도 됨 (Photo Picker 자체가 권한 우회).

### 8.7 자주 참고하는 파일

- **String 매핑 검증**: `app/src/main/res/key_mapping.txt` (한글 ↔ romanized key)
- **모든 enum 정적 데이터**: `core/domain/src/main/kotlin/com/oq/barnote/core/domain/`
- **모든 도메인 enum → 표시 매핑**: `app/src/main/java/com/oq/barnote/extension/DomainStrings.kt`
- **컬러 토큰**: `core/designsystem/src/main/res/values/colors.xml` + `values-night/colors.xml`
- **dp 토큰**: `core/designsystem/src/main/java/com/oq/barnote/core/designsystem/Dimens.kt`
- **앱 상수 (URL/PrefsKey)**: `app/src/main/java/com/oq/barnote/Constants.kt`

### 8.8 자주 빠뜨리는 실수 방지 체크리스트

- [ ] 새 `@Serializable` 모델 추가 시: 해당 모듈에 `kotlin-serialization` plugin 적용됐는지 확인.
- [ ] 새 Composable 추가 시: 모듈에 `compose = true` 활성화됐는지 확인.
- [ ] `R.string.xxx` 참조 시: 해당 모듈 또는 의존 모듈의 strings.xml 에 키가 있는지 확인 (현재 대부분은 `app/res`).
- [ ] `core:domain` 에 코드 추가 시: `androidx.*`, `android.*` import 금지 (순수 Kotlin 모듈 원칙).
- [ ] BroadcastReceiver / Service 작성 시: `@AndroidEntryPoint` + AndroidManifest 등록 둘 다 필요.
- [ ] suspend 함수가 메인 스레드에서 안전한지: `Mutex`, `withContext(Dispatchers.IO)` 적절히 사용.
- [ ] Repository 메서드는 `Result<T>` 반환. throw 대신 `safeCall { }` 헬퍼 사용 (`BarNoteRepositoryImpl` 참조).

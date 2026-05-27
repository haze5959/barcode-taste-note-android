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
- **규칙의 자발적 업데이트 허용**: AI는 세션을 진행하면서 미래의 AI 세션이나 프로젝트 협업에 도움이 될 만한 유용한 지침(아키텍처 패턴, 트러블슈팅 결과, 새로운 컨벤션 등)을 발견한다면, **사용자의 명시적 요청이 없더라도 언제든지 자발적으로 이 `RULES.md` 파일에 해당 지침을 추가 및 업데이트**할 권한과 의무가 있습니다.

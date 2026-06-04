# BarNote (Android)

최신 안드로이드 생태계 트렌드(Jetpack Compose, Clean Architecture, UDF, 멀티 모듈)를 적극 반영하여 구축된 프로젝트입니다.
iOS 앱(BarNote)의 기능과 구조를 안드로이드 환경에 최적화하여 구현하는 것을 목표로 합니다.

## Requirements

- **IDE**: Android Studio (최신 안정화 버전)
- **Java**: JDK 17 이상
- **Kotlin**: 2.2+ (Compose Compiler 포함)

## AI 에이전트 사용법
AI 어시스턴트에게 프로젝트 작업을 지시할 때는 다음 한마디로 시작하세요:
> "RULES.md를 먼저 읽고 시작해줘"

## 배포 자동화 (Play Store)

`deploy-playstore.command` 한 번 실행으로 **버전 증가 → 서명된 AAB 빌드 → Play Console 업로드**(Gradle Play Publisher)를 자동 수행합니다. (iOS `deploy-appstore.command` 의 Android 대응)

### 1. 필요한 파일 및 키

비밀 값은 모두 `local.properties`(git 제외)에, 키 파일은 프로젝트 루트에 둡니다.
**하나라도 없으면 스크립트가 무엇이 빠졌는지 안내하고 안전하게 중단**하므로, 준비되기 전에는 일반 빌드/실행에 전혀 영향이 없습니다.

| 항목 | 위치 | 설명 |
|---|---|---|
| 업로드 키스토어 | `upload-keystore.jks` (루트) | AAB 서명용 (`*.jks` 는 gitignore) |
| 서비스 계정 JSON | `play-service-account.json` (루트) | Play Console 업로드 인증 (gitignore) |
| `google-services.json` | `app/` | Firebase (이미 존재) |

`local.properties` 에 추가할 키:

```properties
# 릴리스 서명 (업로드 키스토어)
release.storeFile=upload-keystore.jks
release.storePassword=<키스토어 비밀번호>
release.keyAlias=upload
release.keyPassword=<키 비밀번호>

# Play Console 업로드
play.serviceAccountFile=play-service-account.json
play.track=internal        # internal / alpha / beta / production (생략 시 internal)
```

### 2. 최초 1회 준비

1. **업로드 키스토어 생성** (안전한 곳에 백업 — 분실 시 앱 업데이트 불가):
   ```bash
   keytool -genkeypair -v -keystore upload-keystore.jks \
       -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Play Console 앱 생성** — 패키지명 `com.oq.barnote`.
3. **서비스 계정 연결** — Play Console → 설정 → API 액세스 → 서비스 계정 생성/연결 후 **앱 출시 권한**(예: 출시 관리자) 부여 → Google Cloud Console 에서 해당 계정의 **JSON 키**를 발급받아 루트에 `play-service-account.json` 으로 저장.
4. **첫 AAB 1회 수동 업로드** — 새 앱은 Play Console 에서 AAB 를 한 번 직접 올려야 트랙이 열립니다. 이후부터 스크립트로 자동 업로드됩니다.

### 3. 실행

```bash
./deploy-playstore.command            # 패치 버전 +1 (기본)
./deploy-playstore.command --minor    # 마이너 +1
./deploy-playstore.command --major    # 메이저 +1
./deploy-playstore.command --none     # 버전 유지 (빌드/업로드 재시도용)
```

- Finder 에서 **더블클릭**으로도 실행됩니다 (`.command` 파일).
- `versionName` / `versionCode` 가 `app/build.gradle.kts` 에서 자동 증가합니다 (Play 는 동일 `versionCode` 재업로드를 거부하므로 `--none` 은 재시도 전용).
- 업로드 트랙은 `play.track`(기본 `internal`).

### 참고

- 프로젝트에 Gradle Wrapper(`gradlew`)가 없으면 시스템 `gradle` 을 사용합니다. `gradle wrapper --gradle-version 8.9` 로 래퍼를 생성해 두면 재현성이 좋습니다.
- 배포 설정의 자세한 내부 동작은 `RULES.md` §7.3 참고. 향후 GitHub Actions(CI) 연동 시 동일한 Gradle Play Publisher 태스크(`publishReleaseBundle`)를 재사용할 수 있습니다.
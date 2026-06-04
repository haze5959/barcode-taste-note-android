import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
    // Firebase: google-services.json 처리 + Crashlytics mapping/symbol 자동 업로드.
    // google-services.json 누락 시 plugin 자체가 빌드 실패 → 사용자 작업 필요 (RULES §7.4).
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    // Gradle Play Publisher (Play Console 자동 업로드). 서비스계정 JSON 이 있을 때만 적용(하단 조건부 apply)
    // → 키 등록 전에는 일반 빌드/실행에 전혀 영향 없음.
    alias(libs.plugins.play.publisher) apply false
}

// local.properties 에서 비밀값 로드. 키가 없으면 빈 문자열 폴백.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProp(key: String): String = localProps.getProperty(key, "")

// ─── Release 서명 (Play 업로드용 AAB) ───
// local.properties 의 release.* 키로 업로드 키스토어를 지정. 미설정 시 release 는 unsigned (현행 유지).
//   release.storeFile / release.storePassword / release.keyAlias / release.keyPassword
val releaseStoreFilePath = localProp("release.storeFile")
val releaseKeystoreFile = releaseStoreFilePath.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }

android {
    namespace = "com.oq.barnote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oq.barnote"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // ─── Auth0 ───
        // local.properties 에서 읽어 BuildConfig + manifestPlaceholders 에 동시 주입.
        val auth0Domain = localProp("auth0.domain")
        val auth0ClientId = localProp("auth0.clientId")
        val auth0Scheme = localProp("auth0.scheme").ifEmpty { "https" }

        buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
        buildConfigField("String", "AUTH0_SCHEME", "\"$auth0Scheme\"")

        // Auth0 SDK 의 WebAuth callback intent-filter 가 사용하는 placeholder.
        manifestPlaceholders["auth0Domain"] = auth0Domain
        manifestPlaceholders["auth0Scheme"] = auth0Scheme

        // ─── Kakao SDK ───
        // local.properties 의 kakao.nativeAppKey 를 BuildConfig + manifestPlaceholder 에 주입.
        // iOS `KakaoSDK.initSDK(appKey:)` 의 native app key 와 동일 값.
        val kakaoNativeAppKey = localProp("kakao.nativeAppKey")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        // intent-filter scheme `kakao{nativeAppKey}` 으로 사용 (카카오 SDK 콜백 표준).
        manifestPlaceholders["kakaoNativeAppKey"] = kakaoNativeAppKey
    }

    signingConfigs {
        // 업로드 키스토어 파일이 실제로 있을 때만 release 서명 설정 생성 (없으면 unsigned 유지).
        if (releaseKeystoreFile != null && releaseKeystoreFile.exists()) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = localProp("release.storePassword")
                keyAlias = localProp("release.keyAlias")
                keyPassword = localProp("release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 서명 설정이 있으면 적용 (Play 업로드용 서명된 AAB), 없으면 unsigned.
            signingConfig = signingConfigs.findByName("release")
            // R8 코드 축소/난독화 + 리소스 축소 활성화 (앱 크기/보안/성능).
            // keep 규칙은 proguard-rules.pro 참조 (Auth0 / kotlinx.serialization / Hilt / Retrofit /
            // Firebase / Kakao / ML Kit 등 리플렉션 사용 라이브러리).
            // ⚠️ 출시 전 release(minified) 빌드로 로그인·시리얼라이즈·결제·FCM 스모크 테스트 필수.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ─── Gradle Play Publisher (Play Console 자동 업로드) ───
// play-service-account.json (또는 local.properties 의 play.serviceAccountFile 경로)이 있을 때만 GPP 적용.
// → 키 등록 전에는 일반 빌드/실행에 전혀 영향 없음. 등록 후 deploy-playstore.command 또는:
//     gradlew publishReleaseBundle   (서명된 AAB 를 Play 트랙[기본 internal]에 업로드)
val playServiceAccountFile =
    rootProject.file(localProp("play.serviceAccountFile").ifEmpty { "play-service-account.json" })
if (playServiceAccountFile.exists()) {
    apply(plugin = "com.github.triplet.play")
    configure<com.github.triplet.gradle.play.PlayPublisherExtension> {
        serviceAccountCredentials.set(playServiceAccountFile)
        track.set(localProp("play.track").ifEmpty { "internal" })
        defaultToAppBundles.set(true)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(project(":core:domain"))
    implementation(project(":core:oqcore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))

    // Kakao SDK 초기화(KakaoSdk.init)를 app(BarNoteApp)에서 직접 호출 → v2-common 직접 의존.
    // (카카오톡 공유 ShareClient 는 oqcore 가 v2-share 로 implementation)
    implementation(libs.kakao.common)

    // Auth0(로그인 webAuth/Credentials)·Billing(구독)은 app 화면/VM 이 직접 사용 → 직접 의존.
    // (core:data 도 동일 SDK 를 implementation 으로 쓰지만 transitive 는 app 에 비노출)
    implementation(libs.auth0.android)
    implementation(libs.billing.ktx)

    // Navigation / Lifecycle Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // 도메인 enum icon() extension
    implementation(libs.androidx.material.icons.extended)

    // DataStore (Onboarding flag)
    implementation(libs.androidx.datastore.preferences)

    // ML Kit Translate (on-device 번역) + Language ID (원문 언어 자동 감지)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    // ML Kit Barcode + CameraX (바코드 스캐너)
    implementation(libs.mlkit.barcode)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Firebase Crashlytics + Analytics (iOS 와 동등하게 자동 수집만 사용, 명시 이벤트 호출 없음)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // Google Play In-App Review (iOS AppStore.requestReview 대응)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Chrome Custom Tabs (iOS SFSafariViewController 대응)
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

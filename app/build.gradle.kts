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
}

// local.properties 에서 비밀값 로드. 키가 없으면 빈 문자열 폴백.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProp(key: String): String = localProps.getProperty(key, "")

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

    buildTypes {
        release {
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

    // Navigation / Lifecycle Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // 도메인 enum icon() extension
    implementation(libs.androidx.material.icons.extended)

    // DataStore (Onboarding flag)
    implementation(libs.androidx.datastore.preferences)

    // ML Kit Translate (on-device 번역)
    implementation(libs.mlkit.translate)

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

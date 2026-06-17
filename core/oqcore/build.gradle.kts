plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.oq.barnote.core.oqcore"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)

    // Chrome Custom Tabs (OQSafariView). iOS `SFSafariViewController` 대응.
    implementation(libs.androidx.browser)

    // 온보딩 등 루프 영상 재생(OQLoopingVideoView) — MediaPlayer+TextureView 직접 렌더 깨짐 방지용 ExoPlayer.
    implementation(libs.media3.exoplayer)

    // Kakao SDK (카카오톡 공유). iOS `KakaoSDKShare` 대응.
    implementation(libs.kakao.share)

    // ML Kit Face Detection (OQBlurFace — 이미지 편집기의 얼굴 가우시안 블러). 온디바이스 번들 모델.
    implementation(libs.mlkit.face.detection)
}

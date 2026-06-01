plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.oq.barnote.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // OkHttp Multipart 사용
    implementation(libs.retrofit.core)
    implementation(libs.okhttp.logging)

    // DataStore (BlockedUsersStore)
    implementation(libs.androidx.datastore.preferences)

    // Firebase Messaging (FcmTokenProvider)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Auth0 (Auth0AuthStore)
    implementation(libs.auth0.android)
    implementation(libs.androidx.security.crypto)

    // Google Play Billing (구독 상태)
    implementation(libs.billing.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:oqcore"))
}

# ===========================================================================
# BarNote R8 / ProGuard keep rules
#
# isMinifyEnabled = true (release) 에서 적용. 리플렉션/직렬화/코드생성을 쓰는 라이브러리들이
# R8 축소·난독화에 깨지지 않도록 보수적으로 keep 합니다.
#
# ⚠️ 출시 전 release(minified) 빌드로 다음을 반드시 스모크 테스트:
#    로그인(Auth0) → 마이페이지 로드(직렬화) → 검색/상세(API) → 결제(Billing) → FCM 수신/탭 → 카카오 공유
# ===========================================================================

# --- 일반 ----------------------------------------------------------------
# 디버깅용 라인 넘버 보존 (Crashlytics 역난독화에 유용) + 원본 파일명 숨김
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod

# enum values()/valueOf — 이름 기반 직렬화(AppLanguage/AppTheme rawValue 등) 대비
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===========================================================================
# kotlinx.serialization
#   런타임 아티팩트가 consumer 규칙을 일부 제공하지만, 모델/Companion serializer 를 확실히 보존.
# ===========================================================================
-keepclassmembers class **$$serializer { *; }

# @Serializable 클래스의 Companion + serializer() 보존
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# @Serializable object 의 INSTANCE.serializer()
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# BarNote 도메인 / oqcore 모델 — @Serializable + 커스텀 serializer 전부 보존 (보수적)
-keep class com.oq.barnote.core.domain.** { *; }
-keep class com.oq.barnote.core.oqcore.models.** { *; }
-keep,includedescriptorclasses class com.oq.barnote.**$$serializer { *; }

# ===========================================================================
# Retrofit / OkHttp (공식 R8 규칙 + 우리 API 인터페이스)
# ===========================================================================
# Retrofit 은 인터페이스의 제네릭 시그니처/애너테이션이 필요
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.oq.barnote.core.network.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ===========================================================================
# Auth0 Android SDK (Gson + 리플렉션으로 Credentials / JWT / UserProfile 처리)
# ===========================================================================
-keep class com.auth0.android.** { *; }
-keep interface com.auth0.android.** { *; }
-dontwarn com.auth0.android.**
# Auth0 가 사용하는 Gson 모델 필드 보존
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===========================================================================
# Hilt / Dagger (대부분 생성 코드 + consumer 규칙으로 처리되나 EntryPoint 는 명시 보존)
# ===========================================================================
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# ===========================================================================
# Firebase / Google Play Services (대부분 consumer 규칙 제공, 경고만 억제)
# ===========================================================================
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ===========================================================================
# Kakao SDK (model 필드 보존 — 카카오 공식 권장)
# ===========================================================================
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-dontwarn com.kakao.sdk.**

# ===========================================================================
# ML Kit (barcode / translate — 대부분 consumer 규칙 제공)
# ===========================================================================
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# ===========================================================================
# Kotlin coroutines / 기타
# ===========================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose 는 R8 + compose compiler 가 처리 — 별도 규칙 불필요.

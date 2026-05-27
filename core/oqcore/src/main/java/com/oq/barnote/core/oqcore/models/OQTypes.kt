package com.oq.barnote.core.oqcore.models

import kotlinx.serialization.Serializable

@Serializable
enum class APIError(val code: Int) {
    InternalServerError(100),
    InternalDBError(101),
    AuthValidationFail(102),
    DuplicatedError(103),
    JwksFetchError(104),
    RecordNotFound(105),
    InvalidParameter(106),
    ExceedMaxCount(107),
    FailedToAnalyzeImage(108),
    BlockedQuery(109),
    Unknown(255)
}

@Serializable
data class APIResponse<T>(
    val result: Boolean,
    val data: T? = null,
    val error: APIError? = null
)

@Serializable
data class APIResponseWithEmptyData(
    val result: Boolean,
    val error: APIError? = null
)

sealed class CommonError : Exception() {
    data class Network(val error: com.oq.barnote.core.oqcore.network.NetworkError) : CommonError()
    data class ApiError(val error: APIError) : CommonError()
    object Decoding : CommonError()
    object AuthorizationFailed : CommonError()
    object AVCaptureDenied : CommonError()
    object ImageLoadFailed : CommonError()
    data class ModuleError(val desc: String) : CommonError()
    object TranslationFailed : CommonError()
    
    val isRecordNotFound: Boolean
        get() = this is ApiError && this.error == APIError.RecordNotFound
}

enum class MediaType {
    Photo, Video
}

enum class AppLanguage(val id: String) {
    System("system"),
    English("en"),
    Korean("ko"),
    Japanese("ja"),
    ChineseSimplified("zh-Hans"),
    ChineseTraditional("zh-Hant"),
    French("fr"),
    German("de"),
    Spanish("es"),
    Italian("it"),
    Portuguese("pt"),
    Russian("ru");

    companion object {
        /** SharedPreferences / DataStore 에서 사용하는 표준 키. iOS `AppLanguage.userDefaultsKey` 와 동일. */
        const val USER_DEFAULTS_KEY: String = "selected_language"

        fun fromId(id: String): AppLanguage? = values().firstOrNull { it.id == id }
    }

    /**
     * iOS `AppLanguage.locale` 에 대응.
     * `System` 은 시스템 기본 Locale 을 사용하라는 의미이므로 `null` 을 반환합니다.
     */
    fun toLocale(): java.util.Locale? = when (this) {
        System -> null
        English -> java.util.Locale.ENGLISH
        Korean -> java.util.Locale.KOREAN
        Japanese -> java.util.Locale.JAPANESE
        ChineseSimplified -> java.util.Locale.SIMPLIFIED_CHINESE
        ChineseTraditional -> java.util.Locale.TRADITIONAL_CHINESE
        French -> java.util.Locale.FRENCH
        German -> java.util.Locale.GERMAN
        Spanish -> java.util.Locale("es")
        Italian -> java.util.Locale.ITALIAN
        Portuguese -> java.util.Locale("pt")
        Russian -> java.util.Locale("ru")
    }
}

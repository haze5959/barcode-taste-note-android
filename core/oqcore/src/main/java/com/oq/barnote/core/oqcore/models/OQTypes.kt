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
    Russian("ru")
}

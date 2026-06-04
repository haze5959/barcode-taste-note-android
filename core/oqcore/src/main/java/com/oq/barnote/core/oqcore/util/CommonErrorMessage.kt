package com.oq.barnote.core.oqcore.util

import android.content.Context
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.models.APIError
import com.oq.barnote.core.oqcore.models.CommonError
import com.oq.barnote.core.oqcore.network.NetworkError

/**
 * [CommonError] → 사용자에게 보여줄 localized 메시지.
 * iOS `View.errorAlert(error:)` 의 케이스별 문구 매핑과 1:1 로 동일합니다.
 *
 * (기존엔 다이얼로그가 `throwable.message ?: simpleName` 을 써서 `CommonError.Network` 가
 *  "Network" 로만 표시되던 문제를 해결.)
 */
fun CommonError.localizedMessage(context: Context): String = when (this) {
    is CommonError.Network -> when (val e = error) {
        NetworkError.InvalidResponse -> context.getString(R.string.error_network_invalid_response)
        NetworkError.Unauthorized -> context.getString(R.string.error_network_unauthorized)
        NetworkError.InvalidURL -> context.getString(R.string.error_network_invalid_url)
        is NetworkError.UnacceptableStatusCode ->
            context.getString(R.string.error_network_status_code_lld, e.code)
        is NetworkError.Transport, NetworkError.EncodingFailed ->
            context.getString(R.string.error_network_transport_failed)
    }

    is CommonError.ApiError -> context.getString(
        when (error) {
            APIError.RecordNotFound -> R.string.error_api_not_found
            APIError.AuthValidationFail -> R.string.error_api_auth_invalid
            APIError.InvalidParameter -> R.string.error_api_invalid_param
            APIError.DuplicatedError -> R.string.error_api_duplicated
            APIError.ExceedMaxCount -> R.string.error_api_exceed_max
            APIError.FailedToAnalyzeImage -> R.string.error_api_analyze_failed
            APIError.BlockedQuery -> R.string.error_api_blocked_query
            APIError.InternalServerError, APIError.InternalDBError,
            APIError.JwksFetchError, APIError.Unknown -> R.string.error_api_server_error
        },
    )

    CommonError.Decoding -> context.getString(R.string.error_decoding)
    CommonError.AuthorizationFailed -> context.getString(R.string.error_authorization_failed)
    CommonError.AVCaptureDenied -> context.getString(R.string.media_picker_error_permission)
    CommonError.ImageLoadFailed -> context.getString(R.string.error_image_load_failed)
    is CommonError.ModuleError -> context.getString(R.string.error_module_error)
    CommonError.TranslationFailed -> context.getString(R.string.error_translation_failed)
}

/**
 * 글로벌 에러 다이얼로그용 표시 메시지. [CommonError] 는 위 매핑을, 그 외 Throwable 은
 * message(있으면) 또는 기본 안내 문구로 폴백. iOS 는 CommonError 만 다루지만 안전망으로 분기.
 */
fun Throwable.toDisplayMessage(context: Context): String = when (this) {
    is CommonError -> localizedMessage(context)
    else -> message?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.error_alert_message_default)
}

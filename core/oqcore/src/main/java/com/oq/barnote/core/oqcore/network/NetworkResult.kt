package com.oq.barnote.core.oqcore.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()
}

sealed class NetworkError : Exception() {
    object InvalidURL : NetworkError()
    object InvalidResponse : NetworkError()
    object Unauthorized : NetworkError()
    data class UnacceptableStatusCode(val code: Int) : NetworkError()
    data class Transport(val throwable: Throwable) : NetworkError()
    object EncodingFailed : NetworkError()
}

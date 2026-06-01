package com.oq.barnote.core.oqcore.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 매 요청에 Authorization 헤더를 첨부하는 Interceptor.
 *
 * 토큰 만료 직전 자동 갱신 (60초 마진) 은 [HeadersProvider] 구현체 안에서 처리되며,
 * 일반적으로는 SDK 내부 메모리 캐시 hit 이라 매우 빠르게 끝납니다. 만료가 임박한 단발성
 * refresh 만 약간의 네트워크 호출 — OkHttp dispatcher 의 worker thread 블로킹은 일반적인
 * 사용 패턴에서 발생하지 않습니다.
 *
 * 401 응답 후의 토큰 갱신 + 자동 재시도는 별도 [TokenAuthenticator] 가 담당하므로 본 Interceptor
 * 는 응답 코드를 검사하지 않습니다.
 */
class AuthInterceptor @Inject constructor(
    private val headersProvider: HeadersProvider,
) : Interceptor {

    interface HeadersProvider {
        suspend fun getHeaders(path: String): Map<String, String>
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val path = chain.request().url.encodedPath

        // SDK 캐시 hit 일 때는 즉시 반환. 만료 임박 시에만 약간의 네트워크 호출.
        val headers = runBlocking { headersProvider.getHeaders(path) }

        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }

        return chain.proceed(requestBuilder.build())
    }
}

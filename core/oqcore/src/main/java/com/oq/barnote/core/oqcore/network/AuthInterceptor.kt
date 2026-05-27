package com.oq.barnote.core.oqcore.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val headersProvider: HeadersProvider
) : Interceptor {

    interface HeadersProvider {
        suspend fun getHeaders(path: String): Map<String, String>
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val path = chain.request().url.encodedPath
        
        // Use runBlocking to call suspend function in Interceptor
        val headers = runBlocking { headersProvider.getHeaders(path) }
        
        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }
        
        return chain.proceed(requestBuilder.build())
    }
}

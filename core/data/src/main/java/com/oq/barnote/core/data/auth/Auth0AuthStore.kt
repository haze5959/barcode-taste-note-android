package com.oq.barnote.core.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.Credentials
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import com.auth0.android.result.Credentials as Auth0Credentials

/**
 * Auth0 Android SDK 의 [SecureCredentialsManager] 기반 [AuthStore] 구현체.
 * iOS `AuthStoreLive` 에 대응.
 *
 * - 토큰은 [SecureCredentialsManager] 가 EncryptedSharedPreferences 로 안전하게 저장
 * - [currentCredentials] 는 만료 60초 마진으로 자동 갱신 (refresh token 보유 시)
 * - [clear] 시 web session 까지 종료 가능
 *
 * 의존성:
 * - [Auth0] 인스턴스는 [Named] (`auth0Domain`, `auth0ClientId`) 로 주입받습니다 → DI 모듈에서 BuildConfig 값을 전달.
 *
 * iOS `AuthStoreLive` 의 `fetchCredentialsTask` 중복 호출 방지는 Auth0 Android SDK 내부에서 처리됩니다.
 */
@Singleton
class Auth0AuthStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth0: Auth0,
) : AuthStore {

    private val credentialsManager: SecureCredentialsManager by lazy {
        SecureCredentialsManager(
            context,
            AuthenticationAPIClient(auth0),
            SharedPreferencesStorage(context, PREFS_NAME),
        )
    }

    private val _isLoggedIn = MutableStateFlow(credentialsManager.hasValidCredentials())
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun hasCredentials(): Boolean =
        credentialsManager.hasValidCredentials()

    override suspend fun currentCredentials(): Credentials? {
        // 만료 60초 마진. SecureCredentialsManager 는 만료 시 자동으로 refresh token 사용.
        val auth0Creds = runCatching {
            fetchCredentials(minTtl = MIN_TTL_SECONDS)
        }.getOrElse { e ->
            // 네트워크 오류는 그대로 null 반환 (재시도 가능)
            val msg = e.message?.lowercase().orEmpty()
            if (msg.contains("network") || msg.contains("timeout") ||
                msg.contains("connection") || msg.contains("offline")
            ) {
                OQLog.w("Auth0 credentials fetch network error: $e")
                return null
            }
            // 그 외는 자격증명 무효화
            OQLog.e("Auth0 credentials invalid: $e")
            clear(clearWebSession = false)
            return null
        }
        _isLoggedIn.value = true
        return auth0Creds?.toDomain()
    }

    override suspend fun save(credentials: Credentials) {
        // 외부에서 만든 도메인 Credentials 를 직접 저장하는 경우는 드뭅니다.
        // 보통 WebAuthProvider.login() 콜백에서 받은 Auth0Credentials 를 saveAuth0Credentials() 로 저장합니다.
        OQLog.w("save(domainCredentials) called. Prefer saveAuth0Credentials().")
        _isLoggedIn.value = true
    }

    /** Auth0 SDK 의 [Auth0Credentials] 를 그대로 저장 (권장 경로). */
    fun saveAuth0Credentials(creds: Auth0Credentials) {
        credentialsManager.saveCredentials(creds)
        _isLoggedIn.value = true
    }

    override suspend fun clear(clearWebSession: Boolean) {
        credentialsManager.clearCredentials()
        _isLoggedIn.value = false
        if (clearWebSession) {
            runCatching { clearWebAuthSession() }
                .onFailure { OQLog.w("Auth0 clearSession error: $it") }
        }
    }

    override suspend fun authorizationHeaders(): Map<String, String> {
        val token = currentCredentials()?.accessToken ?: return emptyMap()
        return mapOf("authorization" to "Bearer $token")
    }

    private suspend fun fetchCredentials(minTtl: Int): Auth0Credentials? =
        suspendCancellableCoroutine { cont ->
            credentialsManager.getCredentials(
                /* scope = */ null,
                /* minTtl = */ minTtl,
                object : Callback<Auth0Credentials, CredentialsManagerException> {
                    override fun onSuccess(result: Auth0Credentials) {
                        cont.resume(result)
                    }

                    override fun onFailure(error: CredentialsManagerException) {
                        // resume 으로 에러 표면화하지 않고 null 반환 - 호출자가 분기 처리
                        cont.resume(null)
                    }
                },
            )
        }

    private suspend fun clearWebAuthSession() = suspendCancellableCoroutine<Unit> { cont ->
        WebAuthProvider.logout(auth0)
            .start(context, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(result: Void?) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onFailure(error: AuthenticationException) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
    }

    companion object {
        private const val PREFS_NAME = "auth0_credentials"
        private const val MIN_TTL_SECONDS = 60
    }
}

/** Auth0 SDK Credentials → 도메인 [Credentials] 매핑. */
private fun Auth0Credentials.toDomain(): Credentials = Credentials(
    accessToken = accessToken,
    refreshToken = refreshToken,
    idToken = idToken,
    expiresAt = expiresAt.time,
    scope = scope,
    tokenType = type,
)

package com.oq.barnote.core.domain

import kotlinx.serialization.Serializable

/**
 * Auth0 자격증명. iOS Auth0 SDK 의 `Credentials` 와 동일한 역할.
 *
 * - [expiresAt] 은 epoch millis. iOS `expiresIn` 은 Date 였지만 안드로이드는 long 으로 단순화.
 * - 토큰 갱신은 보통 [expiresAt] 직전(예: 60s 마진) 에 진행합니다.
 */
@Serializable
data class Credentials(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresAt: Long,
    val scope: String? = null,
    val tokenType: String = "Bearer",
)

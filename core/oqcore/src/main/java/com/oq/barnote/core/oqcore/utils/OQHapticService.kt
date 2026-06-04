package com.oq.barnote.core.oqcore.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS `OQHapticService` 의 안드로이드 등가물.
 *
 * iOS 의 enum 케이스 (`impact(.light)`, `notification(.success)`, `selection`) 에 1:1 대응하는
 * 메서드를 제공합니다. 모두 idempotent 하고 권한 없이도 안전 (vibrator 없으면 무동작).
 *
 * iOS 표준 매핑:
 * - `impact(.light)`   → [lightImpact]    — 짧고 약한 터치 (chip tap, tab 전환 등)
 * - `impact(.medium)`  → [mediumImpact]   — 중간 강도 (보다 강한 confirm)
 * - `impact(.heavy)`   → [heavyImpact]    — 강한 (특수 상황만)
 * - `notification(.success)` → [success]   — 작업 완료
 * - `notification(.warning)` → [warning]   — 경고
 * - `notification(.error)`   → [error]     — 에러
 * - `selection` → [selection] — 슬라이더 / 별점 등 연속 선택 변경
 */
@Singleton
class OQHapticService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // region: iOS-aligned API (recommended)

    /** iOS `impact(.light)` 대응. 짧고 약한 진동. */
    fun lightImpact() = vibratePredefined(VibrationEffect.EFFECT_TICK, fallbackMillis = 12L)

    /** iOS `impact(.medium)` 대응. 중간 강도 진동. */
    fun mediumImpact() = vibratePredefined(VibrationEffect.EFFECT_CLICK, fallbackMillis = 30L)

    /** iOS `impact(.heavy)` 대응. 강한 진동. */
    fun heavyImpact() = vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMillis = 50L)

    /** iOS `selection` 대응. 슬라이더/별점 등 연속 변경에서의 짧은 tick. */
    fun selection() = vibratePredefined(VibrationEffect.EFFECT_TICK, fallbackMillis = 10L)

    /** iOS `notification(.success)` 대응. */
    fun success() = vibratePredefined(VibrationEffect.EFFECT_CLICK, fallbackMillis = 50L)

    /** iOS `notification(.warning)` 대응. 두 번 짧은 진동. */
    fun warning() {
        if (!vibrator.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 30, 80, 30), -1),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 80, 30), -1)
            }
        }
    }

    /** iOS `notification(.error)` 대응. 짧은 두 번 진동 (warning 보다 강하게). */
    fun error() = vibratePredefined(VibrationEffect.EFFECT_DOUBLE_CLICK, fallbackMillis = 50L)

    // endregion

    // region: Legacy aliases (기존 호출 호환 — 새 코드는 위 iOS-aligned 메서드 사용)

    fun playLightImpact() = lightImpact()
    fun playSuccess() = success()
    fun playError() = error()

    // endregion

    private fun vibratePredefined(effectId: Int, fallbackMillis: Long) {
        if (!vibrator.hasVibrator()) return
        // 햅틱은 단순 UI 피드백 — 권한 누락/기기 이슈로 vibrate 가 throw 해도 앱이 죽지 않도록 방어.
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(fallbackMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackMillis)
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface HapticEntryPoint {
        fun hapticService(): OQHapticService
    }
}

/**
 * Composable 에서 [OQHapticService] 를 가져오는 헬퍼.
 *
 * 모든 호출은 동일한 `@Singleton` 인스턴스를 가리킵니다. ViewModel 의 `@Inject hapticService:`
 * 와 동등한 인스턴스라 강도/타이밍이 앱 전체에서 일관됩니다.
 *
 * 사용:
 * ```
 * val haptic = rememberOQHaptic()
 * Slider(onValueChange = { v ->
 *     haptic.selection()  // 슬라이더 tick
 *     onChange(v)
 * })
 * ```
 */
@Composable
fun rememberOQHaptic(): OQHapticService {
    val context = LocalContext.current
    return EntryPointAccessors
        .fromApplication(context.applicationContext, OQHapticService.HapticEntryPoint::class.java)
        .hapticService()
}

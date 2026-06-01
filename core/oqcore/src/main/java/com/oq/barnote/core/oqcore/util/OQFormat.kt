package com.oq.barnote.core.oqcore.util

import java.text.NumberFormat
import java.util.Locale

/**
 * 숫자/문자 표시용 포맷 헬퍼 모음. 도메인 무관 — 어떤 앱에서도 재사용 가능.
 */

/** 천 단위 콤마 포맷. iOS `Int.formatted()` 등가. 예: 12480 → "12,480". 현재 로케일 그룹 구분자 사용. */
fun Int.formatThousands(): String = NumberFormat.getInstance().format(this)

/** 소수 [decimals] 자리 고정 포맷 (로케일 인지). 예: (4.0f).format1Decimal() → "4.0". */
fun Float.formatDecimal(decimals: Int = 1): String =
    String.format(Locale.getDefault(), "%.${decimals}f", this)

/** [Double] 버전. */
fun Double.formatDecimal(decimals: Int = 1): String =
    String.format(Locale.getDefault(), "%.${decimals}f", this)

package com.oq.barnote.extension

import com.oq.barnote.core.oqcore.util.formatDecimal

/**
 * 별점 칩 텍스트 — "⭐️ 4.5" 형태. BarNote 의 여러 row 컴포넌트가 동일하게 쓰던
 * `"⭐️ %.1f".format(rating)` 를 통합. ⭐️ 이모지는 BarNote 표시 컨벤션이라 app 에 두고,
 * 소수 포맷은 oqcore `formatDecimal` 재사용.
 *
 * @receiver 이미 5점 척도로 환산된 별점 (서버 raw / 2 = `getRating()` 결과).
 */
fun Float.ratingStarText(): String = "⭐️ ${formatDecimal(1)}"

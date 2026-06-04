package com.oq.barnote.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

/**
 * BarNote 전역 타이포그래피. iOS = source of truth.
 *
 * Compose Material3 기본 Typography 는 iOS Dynamic Type(Large) 보다 본문/타이틀이 전역적으로 작아
 * (iOS `.body` 17pt → `bodyMedium` 14sp, `.title3` 20pt → `titleMedium` 16sp) UI 전반이 더 작고
 * 빽빽하게 읽힙니다. 코드 전반이 사용하는 시맨틱 슬롯을 iOS pt 에 맞춰 재정의해 −3~4sp 축소를
 * 일괄 해소합니다. (커스텀 폰트는 양쪽 다 없음 — SF↔Roboto 는 플랫폼 본질 차이라 갭 아님.)
 *
 * 실제 호출처 기준 iOS ↔ Material 슬롯 매핑:
 * - iOS `.body`     17pt → `bodyLarge` / `bodyMedium`
 * - iOS `.title3`   20pt → `titleMedium`
 * - iOS `.headline` 17pt → `titleSmall` (NoteDetail in-page 섹션 헤더 등)
 *
 * 변경하지 않은 슬롯(`bodySmall`/`label*`/`titleLarge`)은 Material 기본값 유지 — iOS 와 이미
 * 일치하거나(`.title2` 22pt ↔ titleLarge 22sp), 안드로이드가 의도적으로 더 큰(바텀 탭 라벨) 경우라
 * 무분별한 확대는 오히려 새 divergence 를 만들기 때문입니다.
 *
 * weight/letterSpacing/fontFamily 는 Material 기본값을 그대로 상속(`.copy` 로 size·lineHeight 만 조정).
 */
private val Default = Typography()

val BarNoteTypography: Typography = Default.copy(
    // iOS `.title3` (20pt) — 홈 카드 제목, MyPage 섹션 헤더 등
    titleMedium = Default.titleMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
    // iOS `.headline` (17pt) — NoteDetail in-page 섹션 헤더 등 (기존 14sp 에서 −3sp 해소)
    titleSmall = Default.titleSmall.copy(fontSize = 17.sp, lineHeight = 22.sp),
    // iOS `.body` (17pt)
    bodyLarge = Default.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = Default.bodyMedium.copy(fontSize = 17.sp, lineHeight = 24.sp),
)

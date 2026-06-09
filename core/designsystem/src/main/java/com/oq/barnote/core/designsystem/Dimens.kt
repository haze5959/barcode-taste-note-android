package com.oq.barnote.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * 디자인 시스템 dp 상수.
 * iOS `Constants.swift` 의 `C.V` 에 대응.
 *
 * iOS 의 `CGFloat` 값을 안드로이드 Compose `Dp` 로 1:1 매핑합니다.
 */
object Dimens {
    // Spacing & Padding
    val Spacing = 15.dp
    val Padding = 8.dp
    val BtnPadding = 18.dp

    // Corner radius
    val Radius = 12.dp

    // Section / View spacing
    val SectionSpacing = 28.dp
    val ViewSpacing = 40.dp
    val UnavailableViewSpacing = 120.dp
    val HeroSectionHSize = 240.dp

    // Icon / Card sizes
    val MiniIconSize = 18.dp
    val IconSize = 28.dp
    val CardSize = 40.dp
    val LargeIconSize = 60.dp
    val LargeCardSize = 80.dp
    val FabHSize = 48.dp

    // Row sizes
    val SmallRowWSize = 100.dp
    val RowWSize = 180.dp
    val RowHSize = 210.dp
    val GridMinWSize = 150.dp
}

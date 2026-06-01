package com.oq.barnote.core.oqcore.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS `RoundedRectangle(cornerRadius:).strokeBorder(color, style: StrokeStyle(lineWidth:, dash: [..]))`
 * 의 안드로이드 Compose 등가물 — 점선 테두리.
 *
 * Compose 의 `Modifier.border` 는 점선(dash)을 지원하지 않으므로 [drawBehind] 에서 직접
 * `Stroke(pathEffect = dashPathEffect(...))` 로 그립니다. iOS `.strokeBorder` 와 동일하게 stroke 가
 * 경계 **안쪽** 으로 그려지도록 stroke 폭의 절반만큼 inset 합니다.
 *
 * @param color 선 색상
 * @param width 선 두께 (iOS lineWidth)
 * @param cornerRadius 모서리 반경
 * @param dashOn 점선의 "그려지는" 길이 (iOS dash 배열 첫 값)
 * @param dashOff 점선의 "비는" 길이 (iOS dash 배열 둘째 값)
 */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    dashOn: Dp = 4.dp,
    dashOff: Dp = 3.dp,
): Modifier = this.drawBehind {
    val strokeWidthPx = width.toPx()
    val inset = strokeWidthPx / 2f
    val cornerPx = cornerRadius.toPx()
    val pathEffect = PathEffect.dashPathEffect(
        floatArrayOf(dashOn.toPx(), dashOff.toPx()),
        0f,
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = strokeWidthPx, pathEffect = pathEffect),
    )
}

package com.oq.barnote.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * iOS `Text.lineLimit(maxLines).minimumScaleFactor(minScaleFactor)` 의 Compose 등가물.
 *
 * Compose 1.8+ 의 `BasicText(autoSize = ...)` 가 현재 BOM(1.7.x)엔 없어 수동 구현:
 * [onTextLayout] 으로 [maxLines] 줄에 넘치는지(`hasVisualOverflow`)를 감지해 폰트를 10% 씩 줄이고,
 * 기준 크기의 [minScaleFactor] 배까지 축소합니다. 확정 크기를 찾기 전엔 [drawWithContent] 로
 * 그리지 않아 깜빡임을 방지합니다. [text]/기준 크기가 바뀌면 다시 측정을 시작합니다.
 *
 * - [maxLines] == 1 : 한 줄 고정(softWrap off) 후 넘치면 축소 — 칩/배지/라벨.
 * - [maxLines]  > 1 : 줄바꿈 허용 후 그래도 넘치면 축소 — NoteDetail hero 제품명(2줄) 등.
 *
 * 텍스트 색/굵기 등은 [style] 에 실어 전달합니다(`color` 별도 파라미터 없음).
 */
@Composable
fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minScaleFactor: Float = 0.5f,
) {
    val baseSizeSp = style.fontSize.value
    val minSizeSp = baseSizeSp * minScaleFactor

    var scaledSizeSp by remember(text, baseSizeSp) { mutableStateOf(baseSizeSp) }
    var readyToDraw by remember(text, baseSizeSp) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = scaledSizeSp.sp),
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scaledSizeSp > minSizeSp) {
                // [maxLines] 줄에 안 들어가면 10% 씩 축소 (최소 크기 하한).
                scaledSizeSp = (scaledSizeSp * 0.9f).coerceAtLeast(minSizeSp)
            } else {
                readyToDraw = true
            }
        },
    )
}

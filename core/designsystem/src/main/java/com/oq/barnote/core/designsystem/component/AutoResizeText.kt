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
 * - [shrinkBeforeWrap] = true ([maxLines] > 1 와 함께): 먼저 1 줄로 [minScaleFactor] 까지 축소해 보고,
 *   그래도 넘칠 때만 줄 수를 [maxLines] 까지 한 줄씩 늘린다(늘릴 때 폰트는 원복 후 재축소).
 *   "조금 길면 폰트만 살짝 축소(1 줄), 많이 길면 줄바꿈" — MainBottomBar 탭 라벨 같은 곳에 사용.
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
    shrinkBeforeWrap: Boolean = false,
) {
    val baseSizeSp = style.fontSize.value
    val minSizeSp = baseSizeSp * minScaleFactor

    var scaledSizeSp by remember(text, baseSizeSp) { mutableStateOf(baseSizeSp) }
    // shrinkBeforeWrap=true 면 1 줄부터 시작해 minScale 까지 줄여보고, 그래도 넘치면 줄 수를 늘린다.
    // false(기존 동작)면 처음부터 maxLines 줄로 두고 거기에 맞춰 축소만 한다.
    var lineCount by remember(text, baseSizeSp) {
        mutableStateOf(if (shrinkBeforeWrap) 1 else maxLines)
    }
    var readyToDraw by remember(text, baseSizeSp) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = scaledSizeSp.sp),
        textAlign = textAlign,
        maxLines = lineCount,
        softWrap = lineCount > 1,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            when {
                !result.hasVisualOverflow -> readyToDraw = true
                // 현재 줄 수에서 아직 줄일 여지가 있으면 10% 씩 축소(최소 크기 하한).
                scaledSizeSp > minSizeSp ->
                    scaledSizeSp = (scaledSizeSp * 0.9f).coerceAtLeast(minSizeSp)
                // 최소 크기로도 안 들어가면 줄 수를 늘리고 폰트를 원복해 다시 시도.
                lineCount < maxLines -> {
                    lineCount += 1
                    scaledSizeSp = baseSizeSp
                }
                // 최대 줄 수 + 최소 크기까지 갔으면 그대로 확정(넘치면 clip).
                else -> readyToDraw = true
            }
        },
    )
}

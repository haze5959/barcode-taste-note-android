package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.oq.barnote.core.oqcore.models.Palette

/**
 * iOS `InfoPopOver` 의 안드로이드 Compose 포팅 — anchor 옆에 풍선(말풍선) 형태로 떠 있는 팝오버.
 *
 * Material3 의 `TooltipBox` (이미 일부 화면에서 사용) 는 hover/long-press 용이라
 * "탭 → 안내 카드 노출 → 외부 탭 / 자동 dismiss" 패턴에는 [InfoPopOver] 가 더 적합합니다.
 *
 * 사용:
 * ```
 * Box {
 *     IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, null) }
 *     if (showInfo) {
 *         InfoPopOver(
 *             title = "안내",
 *             message = "이 항목은 ...",
 *             onDismiss = { showInfo = false },
 *         )
 *     }
 * }
 * ```
 *
 * iOS popover 가 anchor 의 위/아래에 자동 배치되는 것과 유사하게, [Popup] 의
 * `popupPositionProvider` 가 화면 경계 안에서 위치 계산을 자동으로 해 줍니다.
 */
@Composable
fun InfoPopOver(
    title: String?,
    message: String,
    onDismiss: () -> Unit,
    palette: Palette = Palette(),
    modifier: Modifier = Modifier,
) {
    Popup(
        popupPositionProvider = AnchorBelowPositionProvider(offsetY = 8),
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true),
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = modifier
                .widthIn(min = 200.dp, max = 300.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfacePrimary)
                .clickable(onClick = onDismiss) // tap-to-dismiss
                .padding(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Popup 을 anchor 의 바로 아래에 배치 (수평 가운데 정렬). 화면 하단을 넘으면 자동으로 anchor 위쪽에 배치.
 */
private class AnchorBelowPositionProvider(
    private val offsetY: Int = 8,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset {
        // 수평: anchor 중심 정렬, 화면 경계 안으로 클램프.
        val centerX = anchorBounds.left + anchorBounds.width / 2
        val xRaw = centerX - popupContentSize.width / 2
        val x = xRaw.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        // 수직: 우선 anchor 아래, 공간 부족하면 위로 fallback.
        val below = anchorBounds.bottom + offsetY
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height - offsetY).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

// 사용처 컴파일 편의 — Suppress 안 쓰는 wildcard 회피용 dummy. 실제 import 는 위에서 했지만,
// `Color` 가 사용되지 않으면 IDE 경고가 뜰 수 있어 잠재적 미래 사용처를 위해 보관.
@Suppress("unused")
private val UnusedReferenceColor = Color.Transparent

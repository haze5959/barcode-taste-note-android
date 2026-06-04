package com.oq.barnote.ui.tip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import kotlinx.coroutines.launch

/**
 * iOS `TipView(_:)` (인라인) 등가물 — 오버레이/팝오버가 아니라 **레이아웃에 끼워지는** 안내 카드.
 *
 * MyPage 처럼 두 뷰 사이에 삽입했다가, 사용자가 닫으면 자연스럽게 빠지는 방식.
 * (anchor 위에 풍선을 띄우는 [BarNoteTip] 과 달리, 이 컴포저블 자체가 레이아웃 한 자리를 차지한다.)
 * dismiss 정책([TipPreferences]: 한 번 닫으면 영구 숨김)은 [BarNoteTip] 과 공유한다.
 *
 * - tip 이 한 번이라도 dismiss 되면 아무것도 렌더하지 않음 (높이 0 → 위/아래 뷰가 붙는다).
 * - 그 외엔 [AnimatedVisibility] 로 펼쳐지며 카드 표시 (아이콘 + 제목/메시지 + 닫기 X).
 * - 닫기 누르면 [TipPreferences] 에 영속 dismiss → shrink 애니메이션과 함께 빠진다.
 */
@Composable
fun BarNoteInlineTip(
    tip: BarnoteTip,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Bolt, // iOS NeededNoteProductTip 의 bolt.fill 대응 (기본값).
) {
    val context = LocalContext.current
    val prefs = remember { tipEntryPoint(context).tipPreferences() }
    val dismissedIds by prefs.dismissedTipIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    AnimatedVisibility(
        visible = tip.id !in dismissedIds,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfaceSecondary)
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(Dimens.Radius))
                .padding(Dimens.Padding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.IconSize),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(tip.titleRes),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                tip.messageRes?.let { msgRes ->
                    Text(
                        text = stringResource(msgRes),
                        style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "dismiss tip",
                tint = textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { scope.launch { prefs.dismiss(tip.id) } },
            )
        }
    }
}

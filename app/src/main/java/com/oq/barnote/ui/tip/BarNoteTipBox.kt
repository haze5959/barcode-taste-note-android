package com.oq.barnote.ui.tip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * iOS `TipView(_:)` 안드로이드 등가물. Material3 [TooltipBox] + [RichTooltip] 기반.
 *
 * - [tip] 이 한 번이라도 dismiss 되었으면 [anchor] 만 렌더링하고 tooltip 표시 안 함.
 * - 그 외에는 자동으로 anchor 옆에 풍선 표시 (Material3 가 위치 계산).
 * - 풍선의 X 버튼 또는 anchor 외부 탭으로 dismiss 시 [TipPreferences] 에 영속.
 *
 * 사용 예:
 * ```
 * BarNoteTip(tip = BarnoteTip.NeededNoteProduct) {
 *     DashboardRow(...) // anchor 컨텐츠
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarNoteTip(
    tip: BarnoteTip,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { tipEntryPoint(context).tipPreferences() }
    val dismissedIds by prefs.dismissedTipIds.collectAsState(initial = emptySet())
    val isDismissed = tip.id in dismissedIds

    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    // 화면 진입 시 자동으로 한 번 show.
    LaunchedEffect(isDismissed) {
        if (!isDismissed) tooltipState.show()
    }

    if (isDismissed) {
        Box(modifier = modifier) { anchor() }
        return
    }

    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(text = stringResource(tip.titleRes)) },
                action = {
                    IconButton(onClick = {
                        scope.launch {
                            prefs.dismiss(tip.id)
                            tooltipState.dismiss()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = accent,
                        )
                    }
                },
                colors = TooltipDefaults.richTooltipColors(),
            ) {
                Text(
                    text = stringResource(tip.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        state = tooltipState,
        modifier = modifier,
    ) {
        anchor()
    }
}

/**
 * 모든 dismissed tip 을 reset. Settings 의 "안내 메시지 초기화" 액션에서 호출.
 */
suspend fun resetAllTips(context: android.content.Context) {
    tipEntryPoint(context).tipPreferences().resetAll()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TipEntryPoint {
    fun tipPreferences(): TipPreferences
}

private fun tipEntryPoint(context: android.content.Context): TipEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        TipEntryPoint::class.java,
    )

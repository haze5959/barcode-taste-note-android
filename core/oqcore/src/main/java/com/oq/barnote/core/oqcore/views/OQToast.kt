package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.oq.barnote.core.oqcore.models.Palette
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * iOS `OQToast` 의 안드로이드 Compose 포팅.
 *
 * 기능:
 * - 제목 / 서브타이틀 / 상단 info 텍스트 / leading icon / trailing 버튼
 * - 4가지 스타일: None / Kakao / LongText / Simple
 * - 위/아래 position 선택
 * - 위/아래 swipe 으로 drag-to-dismiss
 * - 자동 dismiss [OQToastConfig.durationMs] (기본 3초)
 *
 * 사용 — Host 를 한 번만 띄우고 새 토스트마다 [current] 를 업데이트:
 * ```
 * OQToastHost(current = currentToast, onDismiss = { currentToast = null })
 * ```
 */
data class OQToastConfig(
    val title: String,
    val subTitle: String? = null,
    val info: String? = null,
    val style: OQToastStyle = OQToastStyle.None,
    val accentColor: Color? = null,
    val icon: ImageVector? = null,
    val position: OQToastPosition = OQToastPosition.Bottom,
    val button: OQToastButton? = null,
    val durationMs: Long? = 3000L,
)

data class OQToastButton(
    val title: String,
    val onClick: () -> Unit,
)

enum class OQToastStyle { None, Kakao, LongText, Simple }
enum class OQToastPosition { Top, Bottom }

@Composable
fun OQToastHost(
    current: OQToastConfig?,
    onDismiss: () -> Unit,
    palette: Palette = Palette(),
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().zIndex(100f)) {
        val cfg = current
        val isTop = cfg?.position == OQToastPosition.Top
        AnimatedVisibility(
            visible = cfg != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { if (isTop) -it else it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { if (isTop) -it else it }),
            modifier = Modifier.align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter),
        ) {
            if (cfg != null) {
                ToastCard(cfg = cfg, palette = palette, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun ToastCard(
    cfg: OQToastConfig,
    palette: Palette,
    onDismiss: () -> Unit,
) {
    var dragY by remember(cfg) { mutableStateOf(0f) }
    val animatedY by animateFloatAsState(targetValue = dragY, label = "toast-drag")

    LaunchedEffect(cfg) {
        val duration = cfg.durationMs ?: return@LaunchedEffect
        delay(duration)
        onDismiss()
    }

    val accent = cfg.accentColor ?: palette.accent
    val bg = when (cfg.style) {
        OQToastStyle.None, OQToastStyle.LongText -> palette.surfacePrimary
        OQToastStyle.Kakao -> Color(0xFFFEE500)
        OQToastStyle.Simple -> Color.Black.copy(alpha = 0.85f)
    }
    val fg = when (cfg.style) {
        OQToastStyle.None, OQToastStyle.LongText -> palette.textPrimary
        OQToastStyle.Kakao -> Color.Black
        OQToastStyle.Simple -> Color.White
    }
    val subFg = fg.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .offset { IntOffset(0, animatedY.roundToInt()) }
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            // 위/아래 drag 로 dismiss; 임계값 60dp 이상.
            .pointerInput(cfg) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY.absoluteValue > 60f) onDismiss() else dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                ) { _, drag -> dragY += drag }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cfg.style == OQToastStyle.Simple) {
            Text(text = cfg.title, color = fg, style = MaterialTheme.typography.bodyMedium)
            return@Row
        }
        if (cfg.icon != null) {
            Icon(
                imageVector = cfg.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp).padding(end = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (cfg.info != null) {
                Text(text = cfg.info, color = subFg, style = MaterialTheme.typography.labelSmall)
            }
            Text(text = cfg.title, color = fg, style = MaterialTheme.typography.titleSmall)
            if (cfg.subTitle != null) {
                Text(text = cfg.subTitle, color = subFg, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (cfg.button != null) {
            Spacer(modifier = Modifier.size(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent)
                    .clickable { cfg.button.onClick(); onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = cfg.button.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

package com.oq.barnote.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.oq.barnote.core.oqcore.utils.rememberOQHaptic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import kotlin.math.ceil

/**
 * 노트 디테일 슬라이더 (1~5 단계 막대 그래프). iOS `NoteDetailSlider` 에 대응.
 *
 * 막대를 탭하거나 드래그하면 해당 위치의 값으로 설정됩니다.
 */
@Composable
fun NoteDetailSlider(
    title: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val haptic = rememberOQHaptic()

    var width by remember { mutableStateOf(0f) }

    fun update(x: Float) {
        if (width <= 0f) return
        val segmentWidth = width / 5f
        // iOS 는 x<0(트랙 왼쪽 바깥)일 때만 0 으로 본다. 그러나 안드로이드는 좌측 화면 엣지
        // 밖으로의 드래그가 시스템 뒤로가기 제스처와 충돌해 0 에 닿기 어렵다. 그래서 트랙 안에서도
        // 0 에 도달할 수 있도록 첫 구간의 절반 이내로 드래그/탭하면 0 으로 처리한다.
        val raw = if (x < segmentWidth * 0.5f) 0 else ceil(x / segmentWidth).toInt()
        val clamped = raw.coerceIn(0, 5)
        // iOS NoteDetailSlider 와 동일: 단계 변경 시 selection haptic.
        if (clamped != value) {
            haptic.selection()
            onValueChanged(clamped)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (value > 0) accent else secondary,
            )
            // 값이 입력된 경우에만 초기화(✕) 노출 — 오탭으로 들어간 값을 한 번에 해제.
            if (value > 0 && onReset != null) {
                Spacer(modifier = Modifier.width(Dimens.Padding))
                Icon(
                    imageVector = Icons.Filled.Close,
                    // a11y 라벨 — 기존 "초기화"/"Reset" 문자열(11 locale 보유) 재사용.
                    contentDescription = stringResource(com.oq.barnote.core.oqcore.R.string.image_editor_reset),
                    tint = secondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            haptic.selection()
                            onReset()
                        }
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .onSizeChanged { size: IntSize -> width = size.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> update(offset.x) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> update(change.position.x) }
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (i in 1..5) {
                val isActive = i <= value
                val intensity = 0.4f + (0.6f * (i / 5f))
                val targetHeight = (12 + i * 6).dp
                val animatedHeight by animateDpAsState(
                    targetValue = targetHeight,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "BarHeight$i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(animatedHeight)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isActive) accent.copy(alpha = intensity) else surfaceSecondary,
                        ),
                )
            }
        }
    }
}

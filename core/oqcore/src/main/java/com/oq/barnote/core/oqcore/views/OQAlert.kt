package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oq.barnote.core.oqcore.models.Palette

/**
 * [OQAlert] 버튼 스타일. iOS `OQAlertButton.Style` 대응.
 *
 * - [Primary]   : accent 채움 + 흰 글씨 (강조 액션)
 * - [Secondary] : accent 외곽선 + accent 글씨 (대안 액션)
 * - [Tertiary]  : 외곽선/배경 없는 회색 글씨 (취소/닫기)
 */
enum class OQAlertButtonStyle { Primary, Secondary, Tertiary }

/** [OQAlert] 의 버튼 1개. iOS `OQAlertButton` 대응. */
data class OQAlertButton(
    val title: String,
    val style: OQAlertButtonStyle,
)

/**
 * 커스텀 디자인 알럿. iOS `OQCore/OQAlert`(oqAlert modifier) 의 안드로이드 포팅 —
 * 시스템 `AlertDialog` 대신 둥근 카드 + 세로 버튼 스택으로, **최대 3개 버튼**
 * (primary / secondary / tertiary)을 지원합니다.
 *
 * oqcore 는 designsystem 에 의존하지 않으므로 색은 [Palette] 로 주입합니다 (앱은 `barNotePalette()` 전달).
 *
 * 동작: 모든 버튼 탭은 각 콜백 실행 후 [onDismissRequest] 로 알럿을 닫습니다 (iOS 와 동일).
 * 바깥 탭 / 뒤로가기도 [onDismissRequest] (= 취소). 닫기를 막으려면 [dismissOnClickOutside]=false.
 *
 * @param primaryButton   필수 — 강조 액션.
 * @param secondaryButton 선택 — null 이면 표시 안 함.
 * @param tertiaryButton  선택 — null 이면 표시 안 함.
 */
@Composable
fun OQAlert(
    title: String,
    message: String,
    primaryButton: OQAlertButton,
    onPrimary: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButton: OQAlertButton? = null,
    tertiaryButton: OQAlertButton? = null,
    onSecondary: () -> Unit = {},
    onTertiary: () -> Unit = {},
    palette: Palette = Palette(),
    dismissOnClickOutside: Boolean = true,
    // false 면 버튼 탭이 onDismissRequest 를 자동 호출하지 않는다 — 버튼 콜백 자체가 닫기를 책임지고,
    // onDismissRequest(바깥 탭/뒤로)에는 별도 취소 부수효과를 둘 때 사용(예: 이미지 소스 선택 시트).
    dismissOnButtonClick: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = dismissOnClickOutside),
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfacePrimary)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 제목 + 메시지 (각각 비어 있으면 생략 — 제목 없는 정보 알럿/메시지 없는 선택 알럿 지원)
            if (title.isNotBlank() || message.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.textSecondary,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // 버튼 (세로 스택, 풀폭)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AlertButton(
                    button = primaryButton,
                    palette = palette,
                    onClick = { onPrimary(); if (dismissOnButtonClick) onDismissRequest() },
                )
                secondaryButton?.let {
                    AlertButton(
                        button = it,
                        palette = palette,
                        onClick = { onSecondary(); if (dismissOnButtonClick) onDismissRequest() },
                    )
                }
                tertiaryButton?.let {
                    AlertButton(
                        button = it,
                        palette = palette,
                        onClick = { onTertiary(); if (dismissOnButtonClick) onDismissRequest() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertButton(
    button: OQAlertButton,
    palette: Palette,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 눌림 피드백 — 살짝 축소(스프링) + 배경 변화.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "oqAlertButtonScale",
    )
    // Primary 는 눌리면 accent 를 살짝 어둡게, Secondary/Tertiary 는 평소 투명 → 눌리면 옅은 틴트.
    val targetBackground = when (button.style) {
        OQAlertButtonStyle.Primary ->
            if (pressed) lerp(palette.accent, Color.Black, 0.12f) else palette.accent
        OQAlertButtonStyle.Secondary ->
            if (pressed) palette.accent.copy(alpha = 0.12f) else Color.Transparent
        OQAlertButtonStyle.Tertiary ->
            if (pressed) palette.textSecondary.copy(alpha = 0.12f) else Color.Transparent
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        label = "oqAlertButtonBackground",
    )
    val textColor = when (button.style) {
        OQAlertButtonStyle.Primary -> Color.White
        OQAlertButtonStyle.Secondary -> palette.accent
        OQAlertButtonStyle.Tertiary -> palette.textSecondary
    }

    Text(
        text = button.title,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(
                // Secondary 만 accent 외곽선 유지 (press 시에도 테두리는 그대로).
                if (button.style == OQAlertButtonStyle.Secondary) {
                    Modifier.border(BorderStroke(1.dp, palette.accent), shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
    )
}

package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.models.Palette
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView

/**
 * 단순 single-line text field. iOS `OQTFStyle` 만 적용된 raw input 에 대응.
 *
 * title/카운터/에러 등 전체 frame 이 필요하면 [OQTF] 를 사용.
 */
@Composable
fun OQTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    radius: Float = 16f,
    minLines: Int = 1,
    placeholder: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFocusedOrFilled = value.isNotEmpty() || isFocused
    val borderColor = if (isFocusedOrFilled) palette.accent.copy(alpha = 0.4f) else palette.divider
    val borderWidth = if (isFocusedOrFilled) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (minLines > 1) 160.dp else 48.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(palette.bgPrimary)
            .border(borderWidth, borderColor, RoundedCornerShape(radius.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(color = palette.textPrimary),
            cursorBrush = SolidColor(palette.accent),
            minLines = minLines,
        )
        if (value.isEmpty() && placeholder != null) {
            Text(
                text = placeholder,
                style = TextStyle(color = palette.textSecondary.copy(alpha = 0.6f)),
            )
        }
    }
}

/**
 * iOS `OQTF` 의 안드로이드 Compose 포팅 — 풀 버전.
 *
 * 구성:
 * - 상단 Row: title (bold) + (옵션) "옵션" 태그 + (Spacer) + 글자수 카운터 (`n` 또는 `n/max`)
 * - 입력: [OQTextField] 한 줄
 * - 하단: minLength 미달 시 빨간 에러 메시지 (slide-in/fade 애니메이션)
 *
 * - [maxLength] 가 있으면 입력 시 초과분은 잘려 들어가지 않음 (iOS 와 동일).
 * - [minLength] 가 있고 입력값이 채워졌지만 부족하면 빨간 에러.
 */
@Composable
fun OQTF(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    placeholder: String? = null,
    isOption: Boolean = false,
    minLength: Int? = null,
    maxLength: Int? = null,
    palette: Palette = Palette(),
    radius: Float = 16f,
    modifier: Modifier = Modifier,
) {
    val isInvalid = minLength != null && value.isNotEmpty() && value.length < minLength
    val countColor = if (isInvalid) Color(0xFFEF4444) else palette.textSecondary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (isOption) {
                Spacer(modifier = Modifier.padding(start = 8.dp))
                InfoTagView(
                    text = stringResource(R.string.tf_option),
                    style = InfoTagStyle.Normal,
                    palette = palette,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (maxLength != null) "${value.length}/$maxLength" else "${value.length}",
                style = MaterialTheme.typography.labelSmall.copy(color = countColor),
            )
        }

        OQTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(if (maxLength != null && new.length > maxLength) new.take(maxLength) else new)
            },
            palette = palette,
            radius = radius,
            placeholder = placeholder,
        )

        AnimatedVisibility(
            visible = isInvalid && minLength != null,
            enter = fadeIn(animationSpec = spring(stiffness = 600f)) +
                slideInVertically(animationSpec = tween(180), initialOffsetY = { -it }),
            exit = fadeOut(animationSpec = tween(120)) +
                slideOutVertically(animationSpec = tween(120), targetOffsetY = { -it }),
        ) {
            Text(
                text = stringResource(R.string.tf_min_length_error, minLength ?: 0),
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF4444)),
            )
        }
    }
}

/**
 * iOS `OQTE` 의 안드로이드 Compose 포팅 — 다중 라인 풀 버전.
 *
 * [OQTF] 와 동일한 헤더/카운터 frame + [OQTextField] (`minLines = 4`) 로 multi-line 입력.
 * minLength 에러는 제외 (iOS 도 동일 — TE 는 free-form 본문용).
 */
@Composable
fun OQTE(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    placeholder: String? = null,
    isOption: Boolean = false,
    maxLength: Int? = null,
    palette: Palette = Palette(),
    radius: Float = 16f,
    minLines: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (isOption) {
                Spacer(modifier = Modifier.padding(start = 8.dp))
                InfoTagView(
                    text = stringResource(R.string.tf_option),
                    style = InfoTagStyle.Normal,
                    palette = palette,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (maxLength != null) "${value.length}/$maxLength" else "${value.length}",
                style = MaterialTheme.typography.labelSmall.copy(color = palette.textSecondary),
            )
        }

        OQTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(if (maxLength != null && new.length > maxLength) new.take(maxLength) else new)
            },
            palette = palette,
            radius = radius,
            minLines = minLines,
            placeholder = placeholder,
        )
    }
}

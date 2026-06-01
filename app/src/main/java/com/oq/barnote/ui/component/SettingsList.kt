package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R

/**
 * 설정 섹션 컨테이너. 헤더 + 흰 카드 + 옵션 footer.
 * iOS `Section { ... } header: { ... } footer: { ... }` 에 대응.
 */
@Composable
fun SettingsSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(start = Dimens.Padding, top = Dimens.Padding),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfacePrimary),
        ) {
            content()
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
                modifier = Modifier.padding(horizontal = Dimens.Padding),
            )
        }
    }
}

/** SettingsSection 안에서 항목 사이 구분선. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Dimens.BtnPadding * 2),
        color = colorResource(R.color.divider).copy(alpha = 0.4f),
    )
}

/**
 * 단일 설정 행. 좌측 아이콘(색 박스) + 제목 + 우측 슬롯(value/switch/chevron).
 * iOS `HStack { Image; Text; Spacer; ... }` 패턴에 대응.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Spacing - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Dimens.IconSize - 4.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            modifier = Modifier.weight(1f),
        )
        if (valueText != null) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            )
        }
        if (trailing != null) {
            trailing()
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(Dimens.MiniIconSize - 4.dp),
            )
        }
    }
}

/** 설정 행 우측의 토글 스위치. accent color 사용. */
@Composable
fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val accent = colorResource(R.color.accent_color)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val divider = colorResource(R.color.divider)

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = surfacePrimary,
            checkedTrackColor = accent,
            uncheckedThumbColor = surfacePrimary,
            uncheckedTrackColor = divider,
            uncheckedBorderColor = divider,
        ),
    )
}

/** 행 우측에 작은 그룹 (Box) 형태로 trailing 슬롯을 채울 때 사용. */
@Composable
fun TrailingValue(text: String) {
    Box(contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colorResource(R.color.text_secondary),
            ),
        )
    }
}

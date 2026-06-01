package com.oq.barnote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.oqcore.models.AppLanguage

/**
 * 언어 선택 모달 바텀시트. iOS `LanguageSelectionSheet` 에 대응.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.BtnPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Header(onDismiss = onDismiss)
            HorizontalDivider(
                color = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
                    .copy(alpha = 0.4f),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(AppLanguage.values().toList()) { lang ->
                    LanguageRow(
                        language = lang,
                        isSelected = lang == selected,
                        onClick = { onSelect(lang) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = Dimens.BtnPadding),
                        color = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
                            .copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.eoneo_seoljeong),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.dadgi),
            style = MaterialTheme.typography.labelLarge.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(Dimens.Padding),
        )
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayName(language),
            style = MaterialTheme.typography.bodyLarge.copy(color = textPrimary),
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.IconSize),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Dimens.IconSize)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = divider, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun displayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.System -> stringResource(R.string.siseutem)
        AppLanguage.Korean -> stringResource(R.string.hangugeo)
        else -> {
            val locale = language.toLocale()
            // 각 locale 의 자국어 표시명을 사용해 사용자가 모국어로 식별할 수 있도록.
            locale?.getDisplayName(locale) ?: language.id
        }
    }
}

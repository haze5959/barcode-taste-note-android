package com.oq.barnote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens

/**
 * 데이터 내보내기 시 가져올 페이지 번호를 고르는 시트. iOS `Stepper` 시트에 대응.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPageSheet(
    page: Int,
    onPageChange: (Int) -> Unit,
    onSubmit: () -> Unit,
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
            Header(onCancel = onDismiss, onSubmit = onSubmit)
            HorizontalDivider(
                color = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
                    .copy(alpha = 0.4f),
            )

            SectionHeader(
                text = stringResource(
                    R.string.gajyeool_peiji_1peijidang_dgae,
                    Constants.N.EXPORT_NOTE_COUNT,
                ),
            )
            PageStepper(
                page = page,
                onPageChange = onPageChange,
                min = 1,
                max = 1000,
            )
        }
    }
}

@Composable
private fun Header(onCancel: () -> Unit, onSubmit: () -> Unit) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.cwiso),
            style = MaterialTheme.typography.labelLarge.copy(color = textSecondary),
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(Dimens.Padding),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.peiji_seontaeg),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.naebonaegi),
            style = MaterialTheme.typography.labelLarge.copy(
                color = accent,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .clickable(onClick = onSubmit)
                .padding(Dimens.Padding),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary),
        ),
        modifier = Modifier.padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
    )
}

@Composable
private fun PageStepper(
    page: Int,
    onPageChange: (Int) -> Unit,
    min: Int,
    max: Int,
) {
    val surfacePrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .padding(Dimens.Spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.d_peiji, page),
            style = MaterialTheme.typography.bodyLarge.copy(color = textPrimary),
            modifier = Modifier.weight(1f),
        )

        StepperButton(
            icon = Icons.Filled.Remove,
            enabled = page > min,
            accent = accent,
            divider = divider,
            onClick = { onPageChange((page - 1).coerceAtLeast(min)) },
        )
        Spacer(modifier = Modifier.size(Dimens.Padding))
        StepperButton(
            icon = Icons.Filled.Add,
            enabled = page < max,
            accent = accent,
            divider = divider,
            onClick = { onPageChange((page + 1).coerceAtMost(max)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    divider: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.IconSize)
            .clip(CircleShape)
            .background(if (enabled) accent.copy(alpha = 0.12f) else divider.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) accent else divider,
            modifier = Modifier.size(Dimens.MiniIconSize),
        )
    }
}

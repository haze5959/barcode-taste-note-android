package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R

/**
 * 3개 버튼 (primary / secondary / tertiary cancel) 다이얼로그.
 * iOS `oqAlert` 의 tertiaryButton 까지 지원하는 변형에 대응.
 *
 * Material3 `AlertDialog` 의 confirm/dismiss 슬롯이 2개뿐이라 커스텀 `Dialog` 로 작성.
 */
@Composable
fun ThreeButtonDialog(
    title: String,
    message: String,
    primaryText: String,
    secondaryText: String,
    cancelText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = colorResource(R.color.accent_color)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val divider = colorResource(R.color.divider)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius + 4.dp))
                .background(surfacePrimary)
                .padding(Dimens.BtnPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = Dimens.Padding),
            )

            DialogButton(
                text = primaryText,
                onClick = {
                    onPrimary()
                    onDismiss()
                },
                background = accent,
                textColor = surfacePrimary,
            )
            DialogButton(
                text = secondaryText,
                onClick = {
                    onSecondary()
                    onDismiss()
                },
                background = surfacePrimary,
                textColor = textPrimary,
                borderColor = divider,
            )
            DialogButton(
                text = cancelText,
                onClick = onDismiss,
                background = surfacePrimary,
                textColor = textSecondary,
            )
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .let {
                if (borderColor != null) {
                    it.then(
                        Modifier.border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(Dimens.Radius),
                        ),
                    )
                } else it
            }
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Spacing - 2.dp),
    )
}

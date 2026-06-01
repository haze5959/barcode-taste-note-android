package com.oq.barnote.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
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
import com.oq.barnote.core.designsystem.Dimens

/**
 * 임시 빈 화면. 실제 feature 화면이 구현되기 전까지 NavHost destination 자리를 채웁니다.
 *
 * 추후 각 feature 모듈이 만들어지면 [com.oq.barnote.ui.navigation.BarNoteNavHost] 에서
 * 이 placeholder 를 실제 화면으로 교체하면 됩니다.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Dimens.BtnPadding)
                    .clickable(onClick = onBack),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(Dimens.BtnPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f))
                    .padding(Dimens.BtnPadding),
            ) {
                Icon(
                    imageVector = Icons.Filled.Construction,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Dimens.IconSize),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

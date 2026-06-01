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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R

/**
 * 마이페이지용 가로형 대시보드 행. iOS `MyPageView.dashboardRow(...)` 에 대응.
 * 좌측 원형 아이콘 + 제목/값 + 우측 chevron.
 */
@Composable
fun DashboardRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    hasNewBadge: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = colorResource(R.color.accent_color)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(Dimens.Radius))
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .clickable(onClick = onClick)
            .padding(Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.CardSize)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.MiniIconSize + 2.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                )
                if (hasNewBadge) {
                    Text(
                        text = "NEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(Dimens.MiniIconSize - 2.dp),
        )
    }
}

/**
 * 마이페이지용 세로형 대시보드 카드. iOS `MyPageView.dashboardCard(...)` 에 대응.
 * 좌측 정렬 — 아이콘 / 제목 / 큰 값.
 */
@Composable
fun DashboardCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = colorResource(R.color.accent_color)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(Dimens.Radius))
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .clickable(onClick = onClick)
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.CardSize)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.MiniIconSize + 2.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

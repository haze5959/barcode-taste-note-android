package com.oq.barnote.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens

/**
 * MyPage 상단에 노출되는 프리미엄 구독 프로모션 행. iOS `subscriptionPromotionRow` 대응.
 * accent 그라데이션 배경 + 런처 아이콘 + 카피 + "Get Started" 캡슐.
 */
@Composable
fun SubscriptionPromotionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Dimens.Radius,
                shape = RoundedCornerShape(Dimens.Radius),
                ambientColor = accent.copy(alpha = 0.4f),
                spotColor = accent.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.75f)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Image(
            painter = painterResource(R.drawable.launch_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(Dimens.CardSize)
                .clip(RoundedCornerShape(Dimens.Radius)),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.premium_subscription_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.85f),
                ),
            )
            Text(
                text = stringResource(R.string.premium_subscription_description),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
        }

        Text(
            text = stringResource(R.string.premium_subscription_cta),
            style = MaterialTheme.typography.labelMedium.copy(
                color = accent,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .padding(horizontal = Dimens.Spacing, vertical = Dimens.Padding),
        )
    }
}

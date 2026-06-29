package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.designsystem.icon
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.extension.ratingStarText
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import com.oq.barnote.core.oqcore.views.SkeletonView

/**
 * 제품 카드 행 (배경 이미지 + 오버레이 태그). iOS `ProductRowView` 에 대응.
 * 가로 스크롤 또는 그리드 셀로 사용. 높이는 [Dimens.RowHSize] 고정.
 */
@Composable
fun ProductRow(
    info: ProductInfo?,
    modifier: Modifier = Modifier,
) {
    val divider = colorResource(R.color.divider)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val palette = barNotePalette()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.RowHSize)
            .shadow(4.dp, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary),
    ) {
        if (info != null) {
            // 배경 이미지 (이미지 없을 때 iOS 처럼 제품 타입 심볼을 fallback 으로 표시, B14)
            BTNImage(
                path = info.displayImageIds.firstOrNull(),
                modifier = Modifier.fillMaxSize(),
                cornerRadius = Dimens.Radius,
                fallbackIcon = info.product.type.icon(),
            )

            // 하단 그라데이션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.RowHSize * 0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                            ),
                        ),
                    ),
            )

            // 상단 태그
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
                    info.getRating()?.let { rating ->
                        InfoTagView(
                            text = rating.ratingStarText(),
                            style = InfoTagStyle.Material,
                            palette = palette,
                        )
                    }
                    if (info.getNoteCount() > 0) {
                        InfoTagView(
                            text = "📝 ${info.getNoteCount()}",
                            style = InfoTagStyle.Material,
                            palette = palette,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    info.favoriteCount?.takeIf { it > 0 }?.let { fav ->
                        InfoTagView(text = "❤️ $fav", style = InfoTagStyle.Material, palette = palette)
                    }
                    InfoTagView(text = info.product.type.emoji, style = InfoTagStyle.Material, palette = palette)
                }
            }

            // 제품명 (하단)
            Text(
                text = info.product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 4f),
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Dimens.Padding),
            )
        } else {
            SkeletonView(
                modifier = Modifier.fillMaxSize().padding(Dimens.Padding),
                cornerRadius = Dimens.Radius,
            )
        }
    }
}

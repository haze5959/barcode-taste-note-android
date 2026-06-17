package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.oq.barnote.core.designsystem.component.AutoResizeText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.component.RatingView
import com.oq.barnote.core.designsystem.icon
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.oqcore.utils.RelativeTime
import com.oq.barnote.core.oqcore.views.SkeletonView

/**
 * 노트 카드 (정사각형, 상단 이미지 + 본문). iOS `NoteRowView` 에 대응.
 *
 * 차단된 사용자 여부는 [rememberIsUserBlocked] 가 내부에서 자동 조회합니다 (iOS `.task(id: userId)`
 * 등가). 호출자 ViewModel 이 미리 계산해서 넘길 필요가 없으며, N+1 으로 호출자가 잊어 차단 효과가
 * 적용되지 않는 버그가 발생하지 않습니다.
 */
@Composable
fun NoteRow(
    info: NoteInfo?,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val secondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val isBlocked = rememberIsUserBlocked(info?.user?.id)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.RowHSize)
            .shadow(4.dp, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .border(1.dp, divider.copy(alpha = 0.4f), RoundedCornerShape(Dimens.Radius)),
    ) {
        when {
            info == null -> SkeletonView(
                modifier = Modifier.fillMaxSize().padding(Dimens.Padding),
                cornerRadius = Dimens.Radius,
            )

            isBlocked -> BlockedView(secondary = secondary)

            else -> Column(modifier = Modifier.fillMaxSize()) {
                // 상단 이미지 + 그라데이션 + 제품/유저 정보 오버레이
                val headerHeight = Dimens.RowHSize / 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight),
                ) {
                    if (info.displayImageIds.isNotEmpty()) {
                        BTNGridImages(
                            paths = info.displayImageIds,
                            modifier = Modifier.fillMaxSize(),
                            fallbackIcon = info.product.type.icon(),
                        )
                    } else {
                        BTNImage(
                            path = null,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 0.dp,
                            fallbackIcon = info.product.type.icon(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f),
                                    ),
                                    startY = headerHeight.value * 0.5f,
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Dimens.Padding),
                    ) {
                        AutoResizeText(
                            text = info.product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                            maxLines = 1,
                        )
                        info.user?.let { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (user.profileImageId != null) {
                                    BTNImage(
                                        path = user.profileImageId,
                                        modifier = Modifier
                                            .size(Dimens.MiniIconSize)
                                            .clip(CircleShape),
                                        cornerRadius = 999.dp,
                                        fallbackIcon = Icons.Filled.AccountCircle,
                                    )
                                }
                                Text(
                                    text = user.nickName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                    }
                }

                // 본문 (body + flavors + footer)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    Text(
                        text = info.note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                        // iOS: lineLimit(2) + fixedSize(vertical) → 2줄 높이를 항상 예약 (B10).
                        minLines = 2,
                        maxLines = 2,
                    )
                    info.flavors?.takeIf { it.isNotEmpty() }?.let { flavors ->
                        FlavorSummaryChips(flavors = flavors, isMini = true)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RatingView(value = info.note.rating, size = 16.dp, color = accent)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = RelativeTime.formattedByNow(info.note.registered),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedView(secondary: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = secondary.copy(alpha = 0.4f),
        )
        Text(
            text = stringResource(com.oq.barnote.R.string.cadandoen_sayongjaibnida),
            style = MaterialTheme.typography.labelSmall,
            color = secondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

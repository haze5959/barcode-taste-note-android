package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import com.oq.barnote.core.oqcore.utils.RelativeTime
import com.oq.barnote.core.oqcore.views.SkeletonView
import com.oq.barnote.extension.title

/**
 * 노트 상세 행 (좌측 이미지 + 우측 본문). iOS `NoteDetailRowView` 에 대응.
 *
 * 차단된 사용자 여부는 [rememberIsUserBlocked] 가 내부에서 자동 조회합니다 (iOS `.task(id: userId)`
 * 등가). 호출자 ViewModel 이 미리 계산해서 넘길 필요가 없으며, N+1 으로 호출자가 잊어 차단 효과가
 * 적용되지 않는 버그가 발생하지 않습니다.
 */
@Composable
fun NoteDetailRow(
    info: NoteInfo?,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val secondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)
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

            isBlocked -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonOff,
                    contentDescription = null,
                    tint = secondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(Dimens.Spacing))
                Text(
                    text = stringResource(com.oq.barnote.R.string.cadandoen_sayongjaibnida),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondary.copy(alpha = 0.6f),
                )
            }

            else -> Row(modifier = Modifier.fillMaxSize()) {
                // 좌측 이미지 + 좌상단 태그
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    BTNGridImages(
                        paths = info.displayImageIds,
                        modifier = Modifier.fillMaxSize(),
                        fallbackIcon = info.product.type.icon(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.Padding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (info.note.publicScope != PublicScope.Public) {
                            // iOS NoteDetailRowView 의 InfoTagView(text: publicScope.title, systemName: ...)
                            // 와 동등 — Material 변종 + 다국어 title + leadingIcon (Material icon).
                            InfoTagView(
                                text = info.note.publicScope.title(),
                                style = InfoTagStyle.Material,
                                leadingIcon = info.note.publicScope.icon(),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        InfoTagView(
                            text = info.product.type.emoji,
                            style = InfoTagStyle.Material,
                        )
                    }
                }

                // 우측 본문
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    AutoResizeText(
                        text = info.product.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        ),
                        maxLines = 2,
                    )

                    if (info.note.rating == 0) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.Radius))
                                .background(accent.copy(alpha = 0.1f))
                                .padding(Dimens.Spacing),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                        ) {
                            Text(
                                text = stringResource(com.oq.barnote.R.string.teiseuting_noteu_mijagseong),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = accent,
                            )
                            Text(
                                text = stringResource(com.oq.barnote.R.string.i_jepume_daehan_gyeongheomeul_giroghae_boseyo),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary,
                            )
                        }
                    } else {
                        info.user?.let { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (user.profileImageId != null) {
                                    BTNImage(
                                        path = user.profileImageId,
                                        modifier = Modifier
                                            .size(Dimens.IconSize)
                                            .clip(CircleShape),
                                        cornerRadius = 999.dp,
                                        fallbackIcon = Icons.Filled.AccountCircle,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = null,
                                        tint = accent.copy(alpha = 0.4f),
                                        modifier = Modifier.size(Dimens.IconSize),
                                    )
                                }
                                Text(
                                    text = user.nickName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textPrimary,
                                )
                            }
                        }
                        if (info.note.body.isNotEmpty()) {
                            Text(
                                text = info.note.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondary,
                                maxLines = 4,
                            )
                        }
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
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}


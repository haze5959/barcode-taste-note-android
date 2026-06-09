package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.oq.barnote.core.designsystem.component.AutoResizeText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.icon
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.oqcore.util.RelativeTime
import com.oq.barnote.core.oqcore.views.SkeletonView
import com.oq.barnote.extension.ratingStarText

/**
 * 노트 리스트 행. iOS `NoteListRowView` 에 대응.
 */
@Composable
fun NoteListRow(
    info: NoteInfo?,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val secondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .border(1.dp, divider.copy(alpha = 0.3f), RoundedCornerShape(Dimens.Radius))
            .padding(Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        if (info != null) {
            BTNImage(
                path = info.displayImageIds.firstOrNull(),
                modifier = Modifier.size(Dimens.LargeIconSize),
                cornerRadius = Dimens.Radius,
                fallbackIcon = info.product.type.icon(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                AutoResizeText(
                    text = info.product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                    ),
                    maxLines = 2,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            // designsystem 의 PublicScope.icon() 사용 — iOS systemName(SF Symbol) 매핑
                            imageVector = info.note.publicScope.icon(),
                            contentDescription = null,
                            tint = secondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = RelativeTime.formattedByNow(info.note.registered),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = secondary,
                        )
                    }
                    val rating = info.getRating()
                    if (rating != null && rating > 0f) {
                        Text(
                            text = rating.ratingStarText(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = secondary,
                        )
                    } else {
                        Text(
                            text = stringResource(com.oq.barnote.R.string.teiseuting_noteu_mijagseong),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = accent,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(Dimens.Padding))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondary.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp),
            )
        } else {
            SkeletonView(
                modifier = Modifier.size(Dimens.LargeIconSize),
                cornerRadius = Dimens.Radius,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                SkeletonView(
                    modifier = Modifier.width(140.dp).height(16.dp),
                    cornerRadius = 50.dp,
                )
                SkeletonView(
                    modifier = Modifier.width(80.dp).height(14.dp),
                    cornerRadius = 50.dp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


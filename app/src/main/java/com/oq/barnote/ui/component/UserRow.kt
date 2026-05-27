package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.oqcore.views.SkeletonView

/**
 * 유저 행. iOS `UserRowView` 에 대응.
 */
@Composable
fun UserRow(
    userInfo: UserInfo,
    onButtonTap: (userId: String, isFollowing: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(Dimens.Radius), clip = false)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .padding(Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        // Profile image
        if (userInfo.user.profileImageId != null) {
            BTNImage(
                path = userInfo.user.profileImageId,
                modifier = Modifier.size(Dimens.LargeIconSize).clip(CircleShape),
                cornerRadius = 999.dp,
                fallbackIcon = Icons.Filled.AccountCircle,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(Dimens.LargeIconSize),
                tint = accent.copy(alpha = 0.3f),
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Text(
                text = userInfo.user.nickName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimary,
                maxLines = 1,
            )
            userInfo.user.intro?.takeIf { it.isNotEmpty() }?.let { intro ->
                Text(
                    text = intro,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary,
                    maxLines = 1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatItem(
                    icon = Icons.Filled.EditNote,
                    text = userInfo.noteCount.toString(),
                    tint = secondary,
                )
                userInfo.followerCount?.let { count ->
                    StatItem(
                        icon = Icons.Filled.Group,
                        text = count.toString(),
                        tint = secondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(Dimens.Padding))

        userInfo.isFollowing?.let { isFollowing ->
            Button(
                onClick = { onButtonTap(userInfo.id, isFollowing) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) secondary else accent,
                ),
                shape = RoundedCornerShape(Dimens.Radius),
                modifier = Modifier.width(120.dp),
            ) {
                Text(
                    text = if (isFollowing) "팔로잉 취소" else "팔로잉",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

/** UserRow 의 스켈레톤 버전. iOS `UserRowSkeletonView` 에 대응. */
@Composable
fun UserRowSkeleton(modifier: Modifier = Modifier) {
    val surfacePrimary = colorResource(R.color.surface_primary)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .padding(Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        SkeletonView(
            modifier = Modifier.size(Dimens.LargeIconSize),
            cornerRadius = 999.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            SkeletonView(modifier = Modifier.width(100.dp).height(16.dp), cornerRadius = 50.dp)
            SkeletonView(modifier = Modifier.width(150.dp).height(14.dp), cornerRadius = 50.dp)
        }
        SkeletonView(
            modifier = Modifier.width(120.dp).height(32.dp),
            cornerRadius = Dimens.Radius,
        )
    }
}

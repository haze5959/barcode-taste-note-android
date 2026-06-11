package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.models.Palette
import com.oq.barnote.core.oqcore.utils.OQSNSShareData
import com.oq.barnote.core.oqcore.utils.OQSNSShareManager
import com.oq.barnote.core.oqcore.utils.OQSNSShareType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OQSNSShareBottomSheet(
    data: OQSNSShareData,
    manager: OQSNSShareManager,
    palette: Palette = Palette(),
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfacePrimary,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.sns_share_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                // Dismiss button implicitly handled by clicking outside, 
                // but we can add an X button if perfectly mirroring iOS is desired
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OQSNSShareType.values().forEach { type ->
                    if (type != OQSNSShareType.Url || data.shareUrl != null) {
                        ShareItem(type = type, palette = palette) {
                            coroutineScope.launch {
                                sheetState.hide()
                                // share() 는 매니저 자체 스코프에서 fire-and-forget 으로 즉시 반환 —
                                // onDismiss() 로 시트가 dispose 되어 이 코루틴이 취소돼도 공유는 계속 진행.
                                manager.share(type, data)
                                onDismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareItem(
    type: OQSNSShareType,
    palette: Palette,
    onClick: () -> Unit
) {
    val title = stringResource(type.titleRes)
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // iOS OQSNSShare 와 동일: logoName 이 있으면 브랜드 로고를 원형으로,
        // 없으면 surfaceSecondary 원형 배경 위에 시스템 아이콘 (SF Symbol → Material Icons).
        val logoRes = when (type) {
            OQSNSShareType.Instagram -> R.drawable.instagram_logo
            OQSNSShareType.Kakao -> R.drawable.kakao_logo
            else -> null
        }
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (type) {
                        OQSNSShareType.Url -> Icons.Filled.Link
                        else -> Icons.Filled.MoreHoriz
                    },
                    contentDescription = title,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary
        )
    }
}

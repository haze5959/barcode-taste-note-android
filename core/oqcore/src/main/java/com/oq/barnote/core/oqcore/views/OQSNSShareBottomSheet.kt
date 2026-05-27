package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    text = "공유하기", // Localization could be applied here
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
                                onDismiss()
                                manager.share(type, data)
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
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            // Depending on the iconName/logoName, load the appropriate resource.
            // Since we don't have the literal icons, we'll just show the initial for now.
            Text(
                text = type.title.first().toString(),
                fontSize = 24.sp,
                color = palette.textPrimary
            )
        }
        Text(
            text = type.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary
        )
    }
}

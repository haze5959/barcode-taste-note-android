package com.oq.barnote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.R

/**
 * 정보 팝오버 (info ⓘ 아이콘 → 다이얼로그). iOS `InfoPopOver` 에 대응.
 *
 * iOS 의 popover 는 Compose 에 등가물이 없어 [AlertDialog] 로 단순화.
 *
 * @param items title-detail 쌍의 리스트
 */
@Composable
fun InfoPopOver(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    confirmLabel: String = "OK",
) {
    var showing by remember { mutableStateOf(false) }
    val secondary = colorResource(R.color.text_secondary)

    IconButton(
        onClick = { showing = true },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = secondary.copy(alpha = 0.5f),
        )
    }

    if (showing) {
        AlertDialog(
            onDismissRequest = { showing = false },
            confirmButton = {
                TextButton(onClick = { showing = false }) { Text(confirmLabel) }
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items.forEach { (itemTitle, detail) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = itemTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            if (detail.isNotEmpty()) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

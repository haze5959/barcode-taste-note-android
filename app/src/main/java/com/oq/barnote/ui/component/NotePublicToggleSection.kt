package com.oq.barnote.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.R

/**
 * 노트 전체 공개 토글. iOS `NotePublicToggleView` 에 대응.
 */
@Composable
fun NotePublicToggleSection(
    isPublic: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(com.oq.barnote.R.string.jeonce_gonggae),
                // iOS 섹션 헤더 .font(.headline) ≈ titleMedium (B2/B12).
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(com.oq.barnote.R.string.haedang_noteureul_dareun_iyongjawa_gongyuhabnida),
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
            )
        }
        Switch(
            checked = isPublic,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
            ),
        )
    }
}

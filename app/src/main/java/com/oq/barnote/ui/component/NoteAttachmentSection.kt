package com.oq.barnote.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import com.oq.barnote.core.oqcore.ui.component.MediaAttachmentThumbnail
import com.oq.barnote.core.oqcore.ui.component.OQThumbnailsView

/**
 * 노트 작성 시 이미지 첨부 섹션. iOS `NoteAttachmentSectionView` 에 대응.
 *
 * 기본 thumbnail 표시는 [OQThumbnailsView] 가 처리합니다.
 *
 * @param attachments 선택된 미디어 첨부 목록
 * @param isLoading 업로드 중 여부
 * @param onAdd 추가 버튼 탭 → photo picker 호출 (`rememberPhotoPicker` 등)
 * @param onRemove 개별 첨부 제거 콜백 (id 전달)
 */
@Composable
fun NoteAttachmentSection(
    attachments: List<MediaAttachment>,
    isLoading: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 5,
) {
    val secondary = colorResource(R.color.text_secondary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(com.oq.barnote.R.string.imiji_ceombu),
                // iOS 섹션 헤더 .font(.headline) ≈ titleMedium (B2/B12).
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.padding(start = 6.dp))
            InfoTagView(text = stringResource(com.oq.barnote.R.string.obsyeon), style = InfoTagStyle.Normal)
            Spacer(modifier = Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "${attachments.size}/$maxCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
            }
        }
        Text(
            // 기존 key 의 텍스트는 "5장" 하드코딩. maxCount 가 기본값(5)이 아닐 일이 거의 없어 그대로 사용.
            text = stringResource(com.oq.barnote.R.string.ceombuhal_sajineul_5jangggaji_seontaeghal_su_isseoyo),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
        )
        OQThumbnailsView(
            items = attachments.map { it.toThumbnail() },
            onAdd = onAdd,
            onRemove = onRemove,
            modifier = Modifier.fillMaxWidth(),
            maxCount = maxCount,
        )
    }
}

private fun MediaAttachment.toThumbnail(): MediaAttachmentThumbnail =
    MediaAttachmentThumbnail(id = id, data = data)

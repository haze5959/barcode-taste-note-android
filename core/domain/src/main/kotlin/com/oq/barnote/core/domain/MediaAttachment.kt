package com.oq.barnote.core.domain

/**
 * 업로드용 미디어 첨부 데이터.
 * iOS `OQMediaAttachmentPicker.Attachment` 와 동일한 역할.
 *
 * core:domain 은 안드로이드 의존성이 없으므로 `Uri` 가 아닌 raw byte/String 으로 보관합니다.
 * 실제 picker 구현체는 [Uri]/[InputStream] 을 이 데이터로 매핑해 Repository 에 전달합니다.
 */
data class MediaAttachment(
    val id: String,
    val data: ByteArray,
    val mimeType: String = "image/jpeg",
    val fileName: String = "image",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaAttachment) return false
        return id == other.id && data.contentEquals(other.data) &&
            mimeType == other.mimeType && fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

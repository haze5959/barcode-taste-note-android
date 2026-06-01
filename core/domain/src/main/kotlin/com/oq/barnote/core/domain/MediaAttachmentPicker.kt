package com.oq.barnote.core.domain

/**
 * 미디어 첨부 picker. iOS `MediaAttachmentPickerClient` 에 대응.
 *
 * 실제 구현은 Activity / Composable 컨텍스트 의존이라 feature 레이어에서 제공됩니다.
 * Domain 은 인터페이스 + Options/Result 모델만 정의합니다.
 */
interface MediaAttachmentPicker {

    /**
     * Picker 를 표시하고 사용자가 선택한 [MediaAttachment] 들을 반환합니다.
     * 사용자가 취소하면 `Result.failure(...)` 로 처리하거나 빈 리스트 반환은 구현체에 맡깁니다.
     */
    suspend fun pick(options: Options): List<MediaAttachment>

    /** Picker 옵션. iOS `OQMediaAttachmentPicker.Options` 에 대응. */
    data class Options(
        /** 선택 가능한 미디어 타입. */
        val mediaTypes: Set<Type> = setOf(Type.Photo),
        /** 최대 선택 개수. iOS 와 동일하게 기본 5. */
        val maxSelection: Int = 5,
        /** 카메라 직접 촬영을 허용할지. */
        val allowsCamera: Boolean = true,
        /**
         * 선택 후 이미지 편집기 (`OQImageEditor`) 를 띄울지 여부.
         * iOS 의 `OQMediaAttachmentPicker(useEditor: true)` 에 대응.
         * 다중 선택의 경우 각 이미지를 순차적으로 편집한다.
         */
        val useEditor: Boolean = false,
    )

    enum class Type { Photo, Video }
}

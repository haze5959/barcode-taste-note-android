package com.oq.barnote.ui.notedetail

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google ML Kit Translate (on-device) 기반 번역 헬퍼.
 *
 * - 모델은 최초 호출 시 자동 다운로드 (~30MB / 언어 쌍).
 * - source 언어가 명시되지 않으면 영어로 가정 (ML Kit Language ID 도입은 별도 TODO).
 * - 다운로드 조건: 모든 네트워크 허용 (`DownloadConditions.Builder().build()`).
 *   사용자가 셀룰러 데이터로도 즉시 사용 가능. WiFi 강제는 첫 번역 지연을 야기.
 */
@Singleton
class NoteTranslator @Inject constructor() {

    /**
     * [text] 를 [targetLanguage] 로 번역. 실패 시 throw.
     * @param sourceLanguage `null` 이면 영어로 가정.
     */
    suspend fun translate(
        text: String,
        targetLanguage: String = Locale.getDefault().language,
        sourceLanguage: String? = null,
    ): String {
        val src = TranslateLanguage.fromLanguageTag(sourceLanguage ?: "en")
            ?: TranslateLanguage.ENGLISH
        val dst = TranslateLanguage.fromLanguageTag(targetLanguage)
            ?: return text  // 지원하지 않는 target 언어면 원문 반환.
        if (src == dst) return text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(src)
            .setTargetLanguage(dst)
            .build()
        val translator = Translation.getClient(options)
        try {
            // 다운로드 조건 — 모든 네트워크 (셀룰러 포함) 허용. requireWifi() / requireCharging() 미적용.
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).awaitTask()
            return translator.translate(text).awaitTask()
        } finally {
            translator.close()
        }
    }
}

/** Google Tasks API → Kotlin suspend 변환 헬퍼. */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
}

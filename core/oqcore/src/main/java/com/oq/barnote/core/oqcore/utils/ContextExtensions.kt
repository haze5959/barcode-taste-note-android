package com.oq.barnote.core.oqcore.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * 도메인 무관 [Context] 확장 모음 — 클립보드 / 외부 URL / 파일 공유.
 * 여러 화면·ViewModel 에 흩어져 있던 인텐트/시스템 서비스 보일러플레이트를 한곳에 통합.
 */

/**
 * 텍스트를 시스템 클립보드에 복사. 성공 여부 반환.
 *
 * @param text 복사할 텍스트
 * @param label ClipData 라벨 (접근성/시스템 표시용, 기능엔 무관)
 */
fun Context.copyToClipboard(text: String, label: String = "text"): Boolean {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    return runCatching { cm.setPrimaryClip(ClipData.newPlainText(label, text)) }.isSuccess
}

/**
 * 외부 브라우저/앱으로 URL 열기 (`Intent.ACTION_VIEW`).
 *
 * in-app 표시(Chrome Custom Tabs)가 필요하면 `OQSafariView.open` 을 사용하세요. 이 함수는 "완전히
 * 외부 앱으로 떠나는" 케이스 (예: 사용자 개인 웹페이지, 외부 검색) 용입니다.
 *
 * @param newTask Activity 컨텍스트가 아닌 곳(앱 컨텍스트/ViewModel)에서 호출 시 true 필요.
 */
fun Context.openUrl(url: String, newTask: Boolean = true): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (newTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
    true
}.getOrElse {
    OQLog.w("[Context.openUrl] 실패: url=$url, $it")
    false
}

/**
 * 파일(content URI)을 다른 앱과 공유 (`Intent.ACTION_SEND` + chooser).
 *
 * @param uri FileProvider 등으로 만든 content:// URI
 * @param mimeType 기본 "text/plain"
 */
fun Context.shareFile(uri: Uri, mimeType: String = "text/plain"): Boolean = runCatching {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
    true
}.getOrElse {
    OQLog.w("[Context.shareFile] 실패: $it")
    false
}

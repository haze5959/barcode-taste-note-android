package com.oq.barnote.core.oqcore.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.share.model.ImageUploadResult
import com.oq.barnote.core.oqcore.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * SNS 공유 항목.
 *
 * Localized 라벨은 [titleRes] 통해 strings.xml 키로 노출.
 * Compose 에서 [androidx.compose.ui.res.stringResource] 로, 비-Composable 컨텍스트에서는 [Context.getString] 으로 해석.
 */
enum class OQSNSShareType(@StringRes val titleRes: Int, val iconName: String?, val logoName: String?) {
    Instagram(R.string.sns_share_instagram, "camera_viewfinder", "instagram_logo"),
    Kakao(R.string.sns_share_kakao, "bubble_right_fill", "kakao_logo"),
    Url(R.string.url_bogsa, "link", null),
    Other(R.string.sns_share_other, "ellipsis_circle", null)
}

data class OQSNSShareData(
    val title: String,
    val description: String,
    val nick: String,
    val profileImgUrl: String? = null,
    val imageURLs: List<String> = emptyList(),
    val shareUrl: String? = null,
    val appIconResId: Int? = null
)

@Singleton
class OQSNSShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: OQFileManager
) {
    suspend fun share(type: OQSNSShareType, data: OQSNSShareData) {
        withContext(Dispatchers.Main) {
            when (type) {
                OQSNSShareType.Instagram -> shareToInstagramStory(data)
                OQSNSShareType.Kakao -> shareToKakaoTalk(data)
                OQSNSShareType.Url -> shareToUrl(data)
                OQSNSShareType.Other -> shareWithActivityController(data)
            }
        }
    }

    private suspend fun shareToInstagramStory(data: OQSNSShareData) {
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            // TODO: Generate sticker image and use FileProvider to grant URI access
            // putExtra("interactive_asset_uri", stickerUri)
            putExtra("top_background_color", "#231557")
            putExtra("bottom_background_color", "#FF1361")
        }
        
        try {
            val chooser = Intent.createChooser(intent, "Share to Instagram").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            fallbackToBrowser("https://play.google.com/store/apps/details?id=com.instagram.android")
        }
    }

    /**
     * 카카오톡 공유 ([ShareClient.shareCustom]). iOS `ShareApi.shared.shareCustom` 와 동등.
     *
     * 동작 흐름:
     *  1. 외부 이미지 URL 들을 [ShareClient.scrapImage] 로 카카오 서버에 캐싱 → 카카오 URL 획득
     *  2. 이미지 개수에 따라 templateId 분기 (iOS 와 동일): 0~1=131000 / 2=131001 / 3+=130706
     *  3. templateArgs 구성 (TITLE, DESC, NICK, PROFILE, IMG1~3, SHARE_URL, PATH, query params)
     *  4. [ShareClient.isKakaoTalkSharingAvailable] 체크
     *     - 설치: `ShareClient.shareCustom` → result.intent 으로 카카오톡 앱 호출
     *     - 미설치: [WebSharerClient.makeCustomUrl] → 브라우저로 폴백
     */
    private suspend fun shareToKakaoTalk(data: OQSNSShareData) {
        val templateArgs = mutableMapOf<String, String>(
            "TITLE" to data.title,
            "DESC" to data.description,
            "NICK" to data.nick,
        )

        // 프로필 이미지 카카오 서버 캐싱
        data.profileImgUrl?.takeIf { it.isNotBlank() }
            ?.let { scrapKakaoImageUrl(it) }
            ?.let { templateArgs["PROFILE"] = it }

        // 첨부 이미지들 (최대 3) — IMG1, IMG2, IMG3
        val scrapped = data.imageURLs.take(3).mapNotNull { url ->
            url.takeIf { it.isNotBlank() }?.let { scrapKakaoImageUrl(it) }
        }
        scrapped.forEachIndexed { index, url -> templateArgs["IMG${index + 1}"] = url }

        // 이미지 개수에 따른 templateId 분기 (iOS 와 동일)
        val templateId: Long = when {
            scrapped.size >= 3 -> 130706L
            scrapped.size == 2 -> 131001L
            else -> 131000L
        }

        // shareUrl → SHARE_URL / PATH / query params
        data.shareUrl?.takeIf { it.isNotBlank() }?.let { shareUrl ->
            templateArgs["SHARE_URL"] = shareUrl
            val uri = runCatching { Uri.parse(shareUrl) }.getOrNull()
            if (uri != null) {
                val path = uri.path?.trimStart('/').orEmpty()
                if (path.isNotEmpty()) templateArgs["PATH"] = path
                uri.queryParameterNames.forEach { name ->
                    templateArgs[name] = uri.getQueryParameter(name).orEmpty()
                }
            }
        }

        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            // Kakao SDK 의 shareCustom 콜백은 worker thread 에서 호출. UI 작업은 Main 에서.
            shareCustomViaApp(templateId, templateArgs)
        } else {
            shareCustomViaWeb(templateId, templateArgs)
        }
    }

    /** 카카오톡 앱 설치 → ShareClient.shareCustom 호출 후 받은 intent 로 앱 띄움. */
    private suspend fun shareCustomViaApp(templateId: Long, templateArgs: Map<String, String>) {
        suspendCancellableCoroutine<Unit> { cont ->
            ShareClient.instance.shareCustom(
                context = context,
                templateId = templateId,
                templateArgs = templateArgs,
            ) { sharingResult, error ->
                if (sharingResult != null) {
                    runCatching {
                        val intent = sharingResult.intent.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.onFailure { OQLog.e("Kakao shareCustom startActivity failed", it) }
                } else if (error != null) {
                    OQLog.e("Kakao shareCustom failed", error)
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    /** 카카오톡 미설치 → 웹 공유 URL 생성 후 브라우저로 폴백. iOS `fallbackToSafari` 대응. */
    private fun shareCustomViaWeb(templateId: Long, templateArgs: Map<String, String>) {
        runCatching {
            val sharerUrl = WebSharerClient.instance.makeCustomUrl(templateId, templateArgs)
            val intent = Intent(Intent.ACTION_VIEW, sharerUrl).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { OQLog.e("Kakao WebSharer fallback failed", it) }
    }

    /**
     * 외부 이미지 URL 을 카카오 서버에 캐싱하고 카카오 URL 을 반환.
     * iOS `ShareApi.shared.imageUpload(image:)` + URL 다운로드 흐름을 1-step API 로 단축.
     */
    private suspend fun scrapKakaoImageUrl(imageUrl: String): String? =
        suspendCancellableCoroutine { cont ->
            ShareClient.instance.scrapImage(imageUrl) { result: ImageUploadResult?, error: Throwable? ->
                val url = result?.infos?.original?.url
                if (url == null && error != null) {
                    OQLog.w("Kakao scrapImage failed for $imageUrl: ${error.message}")
                }
                if (cont.isActive) cont.resume(url)
            }
        }

    private suspend fun shareToUrl(data: OQSNSShareData) {
        val shareUrl = data.shareUrl ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("share_url", shareUrl)
        clipboard.setPrimaryClip(clip)
        
        OQLog.i("URL copied: $shareUrl")
    }

    private suspend fun shareWithActivityController(data: OQSNSShareData) {
        val text = "${data.title}\n${data.description}\n${data.shareUrl ?: ""}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun fallbackToBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            OQLog.e("Failed to open browser: ${e.message}")
        }
    }
}

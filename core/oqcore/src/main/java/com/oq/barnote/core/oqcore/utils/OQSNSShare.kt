package com.oq.barnote.core.oqcore.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class OQSNSShareType(val title: String, val iconName: String?, val logoName: String?) {
    Instagram("Instagram Story", "camera_viewfinder", "instagram_logo"),
    Kakao("KakaoTalk", "bubble_right_fill", "kakao_logo"),
    Url("URL 복사", "link", null),
    Other("Other options", "ellipsis_circle", null)
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

    private suspend fun shareToKakaoTalk(data: OQSNSShareData) {
        // TODO: Kakao SDK (ShareApi) Integration for templateId matching iOS (131000, 131001, 130706)
        OQLog.i("Stub: Sharing to KakaoTalk with title: ${data.title}")
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

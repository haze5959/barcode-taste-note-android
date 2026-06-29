package com.oq.barnote.core.oqcore.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.model.ImageUploadResult
import com.oq.barnote.core.oqcore.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    /**
     * 공유 작업 전용 스코프. 공유 시트가 닫히면서(onDismiss) 호출측 rememberCoroutineScope 가 취소돼도
     * 공유 플로우(이미지 다운로드 → 인텐트 발사)가 끝까지 진행되도록 호출 스코프와 분리한다.
     * (기존: 시트 dispose 와 함께 호출 코루틴이 취소 → 첫 suspension 에서 조용히 중단 = "무반응" 버그)
     */
    private val shareScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * fire-and-forget 공유 진입점. iOS `share(type:data:)` (Task 내부 실행) 대응.
     * 어떤 실패도 앱 크래시로 전파하지 않고 로그만 남긴다.
     */
    fun share(type: OQSNSShareType, data: OQSNSShareData) {
        shareScope.launch {
            runCatching {
                when (type) {
                    OQSNSShareType.Instagram -> shareToInstagramStory(data)
                    OQSNSShareType.Kakao -> shareToKakaoTalk(data)
                    OQSNSShareType.Url -> shareToUrl(data)
                    OQSNSShareType.Other -> shareWithActivityController(data)
                }
            }.onFailure { OQLog.e("SNS share($type) failed", it) }
        }
    }

    /**
     * Instagram Stories 공유. iOS `shareToInstagramStory` 대응.
     *
     * 동작 흐름 (iOS 와 동일):
     *  0. 설치 선체크 (iOS `canOpenURL(instagram-stories://)`) — 미설치면 에셋 준비 없이 즉시 스토어 폴백
     *  1. 배경 = 첫 이미지 URL 다운로드 → FileProvider content URI (없으면 그라디언트 #231557→#FF1361 폴백)
     *  2. 스티커 = 프로필 + 닉/제목/설명 브랜드 카드 비트맵 ([buildStickerBitmap]) → FileProvider URI (interactive asset)
     *  3. `com.instagram.share.ADD_TO_STORY` 인텐트로 Instagram 호출 (대상 앱에 URI read 권한 부여)
     *  4. 실패 → Play 스토어 폴백 (iOS: App Store)
     *
     * 다운로드·비트맵 생성은 IO 에서, 실제 [Context.startActivity] 는 Main 에서 수행.
     * @see <a href="https://developers.facebook.com/docs/instagram-platform/sharing-to-stories">IG Stories 공유</a>
     */
    private suspend fun shareToInstagramStory(data: OQSNSShareData) {
        // iOS canOpenURL 선체크 대응 — 미설치 기기에서 grantUriPermission(Unknown package 예외)이나
        // 불필요한 다운로드 없이 곧장 스토어로. (manifest <queries> 에 패키지 선언되어 API 30+ 조회 가능)
        if (!isPackageInstalled(INSTAGRAM_PACKAGE)) {
            fallbackToBrowser(INSTAGRAM_STORE_URL)
            return
        }

        val (backgroundUri, stickerUri) = withContext(Dispatchers.IO) {
            // 배경 = 첫 이미지 (iOS: data.imageURLs.first)
            val bg = data.imageURLs.firstOrNull()?.takeIf { it.isNotBlank() }
                ?.let { downloadBytes(it) }
                ?.let { writeShareFile("ig_background.jpg", it) }
            // 스티커 = 프로필(있으면) + 브랜드 카드. 생성 실패해도 공유는 진행(graceful).
            // 프로필은 스티커에서 작은 원으로만 쓰이므로 풀 해상도 디코드 대신 다운샘플 → OOM·메모리 점유 감소.
            val profile = data.profileImgUrl?.takeIf { it.isNotBlank() }
                ?.let { downloadBytes(it) }
                ?.let { decodeSampledBitmap(it, reqSize = 144) }  // 스티커 아바타(72px)의 2배 여유
            val sticker = runCatching { buildStickerBitmap(data, profile) }.getOrNull()
                ?.let { bmp -> writeShareBitmap("ig_sticker.png", bmp).also { bmp.recycle() } }
            profile?.recycle()
            bg to sticker
        }

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setPackage(INSTAGRAM_PACKAGE)
            // iOS 는 source_application=bundleID. Android 는 패키지명 (FB App ID 등록 시 그 값 권장).
            putExtra("source_application", context.packageName)
            if (backgroundUri != null) {
                setDataAndType(backgroundUri, "image/jpeg")
            } else {
                // 배경 이미지가 없으면 iOS 와 동일한 그라디언트
                setType("image/*") // Type이 지정되지 않으면 Instagram의 intent filter를 통과하지 못해 ActivityNotFoundException 발생
                putExtra("top_background_color", "#231557")
                putExtra("bottom_background_color", "#FF1361")
            }
            stickerUri?.let { putExtra("interactive_asset_uri", it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Instagram 에 임시 파일 read 권한 부여 (FLAG 만으론 일부 단말에서 누락될 수 있어 명시).
        // grant 실패가 공유 자체를 막지 않도록 개별 runCatching.
        listOfNotNull(backgroundUri, stickerUri).forEach { uri ->
            runCatching {
                context.grantUriPermission(INSTAGRAM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onFailure { OQLog.w("Instagram share: grantUriPermission failed: ${it.message}") }
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 선체크 후에도 레이스(직후 삭제 등)로 실패할 수 있음 → Play 스토어 (iOS: App Store 폴백)
            fallbackToBrowser(INSTAGRAM_STORE_URL)
        } catch (e: Exception) {
            OQLog.e("Instagram story share failed: ${e.message}")
            fallbackToBrowser(INSTAGRAM_STORE_URL)
        }
    }

    /**
     * 카카오톡 공유 ([ShareClient.shareCustom]). iOS `shareToKakaoTalk` 와 동일 흐름.
     *
     * 동작 흐름 (iOS 와 동일):
     *  0. [ShareClient.isKakaoTalkSharingAvailable] 선체크 — 미설치면 스크랩 등 서버 호출 없이
     *     즉시 Play 스토어 폴백 (iOS: App Store 카카오톡 페이지)
     *  1. 외부 이미지 URL 들을 [ShareClient.scrapImage] 로 카카오 서버에 캐싱 → 카카오 URL 획득
     *  2. 이미지 개수에 따라 templateId 분기 (iOS 와 동일): 0~1=131000 / 2=131001 / 3+=130706
     *  3. templateArgs 구성 (TITLE, DESC, NICK, PROFILE, IMG1~3, SHARE_URL, PATH, query params)
     *  4. `ShareClient.shareCustom` → result.intent 으로 카카오톡 앱 호출
     */
    private suspend fun shareToKakaoTalk(data: OQSNSShareData) {
        // iOS isKakaoTalkSharingAvailable 선체크 대응. SDK 미초기화(키 미설정) 등 어떤 예외도
        // 크래시로 전파하지 않도록 runCatching — 사용 불가면 스토어 폴백.
        val available = runCatching {
            ShareClient.instance.isKakaoTalkSharingAvailable(context)
        }.onFailure { OQLog.e("Kakao availability check failed", it) }.getOrDefault(false)
        if (!available) {
            fallbackToBrowser(KAKAO_TALK_STORE_URL)
            return
        }

        val templateArgs = mutableMapOf<String, String>(
            "TITLE" to data.title,
            "DESC" to data.description,
            "NICK" to data.nick,
        )

        // 프로필 + 첨부 이미지(최대 3) 를 카카오 서버에 병렬 캐싱 — 직렬 RTT 누적으로 공유 시트가 늦게 뜨는 것 방지.
        // PROFILE 과 IMG1~3 은 키가 달라 핸들을 분리한다. awaitAll 이 순서를 보존하고 filterNotNull 로 실패분만
        // 제거하므로, 당겨짐·개수 기반 templateId 분기 의미는 직렬 구현(iOS) 과 동일하게 유지된다.
        val scrapped: List<String> = coroutineScope {
            val profileDeferred = data.profileImgUrl?.takeIf { it.isNotBlank() }
                ?.let { async { scrapKakaoImageUrl(it) } }
            val imageDeferreds = data.imageURLs.take(3)
                .filter { it.isNotBlank() }
                .map { async { scrapKakaoImageUrl(it) } }
            profileDeferred?.await()?.let { templateArgs["PROFILE"] = it }
            imageDeferreds.awaitAll().filterNotNull()
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

        // Kakao SDK 내부 Retrofit(kapi)은 커스텀 callbackExecutor 를 지정하지 않아, Retrofit 의 Android 기본
        // 콜백 디스패처(AndroidMainExecutor = Handler(mainLooper))로 shareCustom 콜백이 메인스레드에서 호출된다.
        // 따라서 콜백 본문에서 startActivity 를 그대로 호출해도 안전하다. (AAR 디컴파일로 확인)
        shareCustomViaApp(templateId, templateArgs)
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

    /**
     * 패키지 설치 여부. iOS `UIApplication.canOpenURL(스킴)` 대응.
     * API 30+ 패키지 가시성 제한이 있어 manifest `<queries>` 에 대상 패키지가 선언돼 있어야 한다
     * (com.instagram.android / com.kakao.talk 선언됨).
     */
    private fun isPackageInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)

    private companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val INSTAGRAM_STORE_URL =
            "https://play.google.com/store/apps/details?id=$INSTAGRAM_PACKAGE"
        // iOS 는 미설치 시 App Store 카카오톡 페이지로 보냄 — Play 스토어 등가.
        const val KAKAO_TALK_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.kakao.talk"
    }

    // ───────── Instagram Stories 공유 헬퍼 ─────────

    /** 외부 이미지 URL → 바이트 (IO). connect/read 타임아웃 적용. 실패 시 null. */
    private fun downloadBytes(urlStr: String): ByteArray? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        conn.inputStream.use { it.readBytes() }
    }.onFailure { OQLog.w("Instagram share: download failed for $urlStr: ${it.message}") }.getOrNull()

    /**
     * 바이트를 [reqSize] 근처로 다운샘플 디코드. 거대한 원본(서버 해상도)을 풀 ARGB 로 디코드해
     * OOM·과도한 일시 메모리 점유가 발생하는 것을 막는다 (스티커 프로필은 작은 원으로만 사용).
     * inJustDecodeBounds 로 치수만 먼저 읽어 inSampleSize 를 산정한다. 실패 시 null.
     */
    private fun decodeSampledBitmap(bytes: ByteArray, reqSize: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var inSample = 1
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxDim / inSample > reqSize) inSample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }.onFailure { OQLog.w("Instagram share: decodeSampledBitmap failed: ${it.message}") }.getOrNull()

    /** 바이트를 `cacheDir/share/<name>` 에 저장 후 FileProvider content URI 반환. */
    private fun writeShareFile(name: String, bytes: ByteArray): Uri? = runCatching {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, name).apply { writeBytes(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.onFailure { OQLog.w("Instagram share: writeShareFile failed: ${it.message}") }.getOrNull()

    /** 비트맵을 PNG 로 `cacheDir/share/<name>` 에 저장 후 FileProvider content URI 반환. */
    private fun writeShareBitmap(name: String, bitmap: Bitmap): Uri? = runCatching {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.onFailure { OQLog.w("Instagram share: writeShareBitmap failed: ${it.message}") }.getOrNull()

    /**
     * Instagram Stories 오버레이 스티커(브랜드 카드) 비트맵. iOS `OQInstagramStickerView` 대응.
     * 어두운 라운드 카드에 [프로필 + 닉네임/앱이름] / 제목 / 설명을 배치 (좌측 정렬).
     */
    private fun buildStickerBitmap(data: OQSNSShareData, profile: Bitmap?): Bitmap {
        val width = 720
        val outer = 24          // 카드 바깥 여백
        val pad = 36            // 카드 안쪽 여백
        val avatar = 72         // 프로필/헤더 높이
        val gapAvatarText = 20
        val gapHeaderTitle = 30
        val gapTitleDesc = 14
        val left = outer + pad
        val contentWidth = width - left * 2

        val nickPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 255, 255); textSize = 22f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255); textSize = 26f
        }

        fun textBlock(text: String, paint: TextPaint, maxLines: Int): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()

        val titleLayout = textBlock(data.title, titlePaint, 3)
        val descLayout = data.description.takeIf { it.isNotBlank() }?.let { textBlock(it, descPaint, 4) }

        val cardTop = outer
        val cardBottom = outer + pad + avatar + gapHeaderTitle + titleLayout.height +
            (descLayout?.let { gapTitleDesc + it.height } ?: 0) + pad
        val height = cardBottom + outer

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val cardRect = RectF(outer.toFloat(), cardTop.toFloat(), (width - outer).toFloat(), cardBottom.toFloat())
        canvas.drawRoundRect(cardRect, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(204, 20, 20, 24)   // 어두운 반투명 카드 (iOS frosted dark 근사)
        })
        canvas.drawRoundRect(cardRect, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.argb(70, 255, 255, 255)
        })

        var y = (outer + pad).toFloat()
        // 프로필 (원형) — 있으면 원형 크롭, 없으면 반투명 원 placeholder
        val cx = left + avatar / 2f
        val cy = y + avatar / 2f
        if (profile != null) {
            val scaled = Bitmap.createScaledBitmap(profile, avatar, avatar, true)
            canvas.save()
            canvas.clipPath(Path().apply { addCircle(cx, cy, avatar / 2f, Path.Direction.CW) })
            canvas.drawBitmap(scaled, left.toFloat(), y, null)
            canvas.restore()
            if (scaled !== profile) scaled.recycle()
        } else {
            canvas.drawCircle(cx, cy, avatar / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 255, 255, 255)
            })
        }

        // 앱 아이콘 (헤더 우측, 라운드 사각형) — iOS OQInstagramStickerView 의
        // appIcon.frame(48).clipShape(RoundedRectangle(cornerRadius:10)) 대응. 미지정(null)/디코드 실패면 iOS 처럼 생략.
        val appIcon = data.appIconResId
            ?.let { runCatching { BitmapFactory.decodeResource(context.resources, it) }.getOrNull() }
        val iconLeft: Float? = appIcon?.let { icon ->
            val iconRight = (width - outer - pad).toFloat()
            val iconL = iconRight - avatar
            val scaledIcon = Bitmap.createScaledBitmap(icon, avatar, avatar, true)
            canvas.save()
            canvas.clipPath(Path().apply {
                addRoundRect(RectF(iconL, y, iconRight, y + avatar), 16f, 16f, Path.Direction.CW)
            })
            canvas.drawBitmap(scaledIcon, iconL, y, null)
            canvas.restore()
            if (scaledIcon !== icon) scaledIcon.recycle()
            icon.recycle()
            iconL
        }

        // 닉네임 + 앱 이름 — 헤더 우측 아이콘과 겹치지 않게 폭을 제한하고 ellipsize (iOS Spacer 자동정렬 등가).
        // drawText 는 줄바꿈/잘림이 없어 긴 닉이면 아이콘 위로 겹치므로 명시적 절단이 필요하다.
        val textX = (left + avatar + gapAvatarText).toFloat()
        val textRight = iconLeft?.minus(gapAvatarText) ?: (width - outer - pad).toFloat()
        val maxTextWidth = (textRight - textX).coerceAtLeast(0f)
        val nick = TextUtils.ellipsize(data.nick, nickPaint, maxTextWidth, TextUtils.TruncateAt.END)
        canvas.drawText(nick, 0, nick.length, textX, y + nickPaint.textSize, nickPaint)
        runCatching { context.applicationInfo.loadLabel(context.packageManager).toString() }
            .getOrNull()?.takeIf { it.isNotBlank() }
            ?.let {
                val label = TextUtils.ellipsize(it, subPaint, maxTextWidth, TextUtils.TruncateAt.END)
                canvas.drawText(label, 0, label.length, textX, y + nickPaint.textSize + subPaint.textSize + 10f, subPaint)
            }

        // 앱 아이콘 그리기 (iOS와 동일하게 우측에 표시)
        val appIconDrawable = runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
        if (appIconDrawable != null) {
            val iconSize = avatar
            val iconRect = RectF(width - left - iconSize.toFloat(), y, width - left.toFloat(), y + iconSize)
            val path = Path().apply { addRoundRect(iconRect, 16f, 16f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            appIconDrawable.setBounds(iconRect.left.toInt(), iconRect.top.toInt(), iconRect.right.toInt(), iconRect.bottom.toInt())
            appIconDrawable.draw(canvas)
            canvas.restore()
        }

        y += avatar + gapHeaderTitle
        // 제목
        canvas.save(); canvas.translate(left.toFloat(), y); titleLayout.draw(canvas); canvas.restore()
        y += titleLayout.height
        // 설명
        descLayout?.let {
            y += gapTitleDesc
            canvas.save(); canvas.translate(left.toFloat(), y); it.draw(canvas); canvas.restore()
        }
        return bmp
    }
}

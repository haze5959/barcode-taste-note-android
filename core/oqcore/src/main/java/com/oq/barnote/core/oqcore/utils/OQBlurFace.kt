package com.oq.barnote.core.oqcore.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 이미지의 사람 얼굴을 검출해 **가우시안 블러** 처리. iOS `OQBlurFace` 대응.
 *
 * iOS 는 CoreImage(`CIDetector` 얼굴 검출 + `CIPixellate` + `CIRadialGradient` 마스크 + `CIBlendWithMask`)
 * 로 구현했지만 Android 엔 CoreImage 가 없어 다음으로 대체한다:
 * - **검출**: ML Kit On-device Face Detection (번들 모델 — 네트워크 불필요)
 * - **블러**: 얼굴별 영역을 분리형 박스 블러 3-pass(≈ 가우시안)로 흐리게 한 뒤, 라디얼 알파 마스크로
 *   가장자리를 부드럽게 페이드해 원본 위에 합성 (iOS 의 원형 라디얼 마스크 등가).
 *
 * 픽셀 연산은 무거우므로 다운스케일(1/[DOWNSCALE]) 상태에서 블러 후 업스케일(bilinear)해 비용을 줄인다.
 */
object OQBlurFace {

    private const val DOWNSCALE = 6        // 블러 전 축소 배율 (성능 + 추가적 부드러움)
    private const val BLUR_PASSES = 3      // 박스 블러 반복 (3회 ≈ 가우시안)

    /**
     * 얼굴이 있으면 블러 처리한 **새 비트맵**을, 없으면 `null` 을 반환한다.
     * (iOS `hasFaces()` + `blurFaces()` 를 한 번에 — 호출부는 null 이면 "얼굴 없음" 처리)
     */
    suspend fun blurFaces(bitmap: Bitmap): Bitmap? {
        val faces = runCatching { detectFaces(bitmap) }
            .onFailure { OQLog.e("OQBlurFace 검출 실패: ${it.message}") }
            .getOrDefault(emptyList())
        if (faces.isEmpty()) return null
        return withContext(Dispatchers.Default) { renderBlurred(bitmap, faces) }
    }

    private suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build(),
        )
        return suspendCancellableCoroutine { cont ->
            detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { faces -> if (cont.isActive) cont.resume(faces) }
                .addOnFailureListener { e ->
                    OQLog.w("ML Kit face detect failure: ${e.message}")
                    if (cont.isActive) cont.resume(emptyList())
                }
                .addOnCompleteListener { runCatching { detector.close() } }
            cont.invokeOnCancellation { runCatching { detector.close() } }
        }
    }

    /** 검출된 얼굴 영역들을 가우시안 블러 + 라디얼 마스크로 원본 복사본에 합성. */
    private fun renderBlurred(src: Bitmap, faces: List<Face>): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width
        val h = result.height

        for (face in faces) {
            val box = face.boundingBox
            // 머리카락/턱까지 덮도록 가로 18%·세로 28% 확장 후 비트맵 경계로 클램프.
            val padX = (box.width() * 0.18f).roundToInt()
            val padY = (box.height() * 0.28f).roundToInt()
            val left = max(0, box.left - padX)
            val top = max(0, box.top - padY)
            val right = min(w, box.right + padX)
            val bottom = min(h, box.bottom + padY)
            val rw = right - left
            val rh = bottom - top
            if (rw <= 1 || rh <= 1) continue

            val region = Bitmap.createBitmap(result, left, top, rw, rh)
            val blurred = gaussianBlur(region)

            // 라디얼 알파 마스크 — 중심부 불투명 → 가장자리 투명(부드러운 경계). iOS CIRadialGradient 대응.
            val cx = rw / 2f
            val cy = rh / 2f
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, max(cx, cy),
                    intArrayOf(Color.BLACK, Color.TRANSPARENT),
                    floatArrayOf(0.72f, 1f),
                    Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            Canvas(blurred).drawRect(0f, 0f, rw.toFloat(), rh.toFloat(), maskPaint)

            canvas.drawBitmap(blurred, left.toFloat(), top.toFloat(), null)
            region.recycle()
            blurred.recycle()
        }
        return result
    }

    /** 다운스케일 → 박스 블러 3-pass → 업스케일(bilinear). 결과는 부드러운 가우시안 근사. */
    private fun gaussianBlur(region: Bitmap): Bitmap {
        val rw = region.width
        val rh = region.height
        val sw = max(1, rw / DOWNSCALE)
        val sh = max(1, rh / DOWNSCALE)
        val small = Bitmap.createScaledBitmap(region, sw, sh, true)

        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        val radius = max(1, min(sw, sh) / 4)
        val tmp = IntArray(pixels.size)
        repeat(BLUR_PASSES) {
            horizontalAverage(pixels, tmp, sw, sh, radius)
            verticalAverage(tmp, pixels, sw, sh, radius)
        }
        small.setPixels(pixels, 0, sw, 0, 0, sw, sh)

        val up = Bitmap.createScaledBitmap(small, rw, rh, true)
        if (up !== small) small.recycle()
        return up
    }

    private fun horizontalAverage(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        for (y in 0 until h) {
            val base = y * w
            for (x in 0 until w) {
                var a = 0; var r = 0; var g = 0; var b = 0; var n = 0
                val from = max(0, x - radius)
                val to = min(w - 1, x + radius)
                for (xx in from..to) {
                    val p = src[base + xx]
                    a += (p ushr 24) and 0xFF
                    r += (p ushr 16) and 0xFF
                    g += (p ushr 8) and 0xFF
                    b += p and 0xFF
                    n++
                }
                dst[base + x] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }

    private fun verticalAverage(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0; var r = 0; var g = 0; var b = 0; var n = 0
                val from = max(0, y - radius)
                val to = min(h - 1, y + radius)
                for (yy in from..to) {
                    val p = src[yy * w + x]
                    a += (p ushr 24) and 0xFF
                    r += (p ushr 16) and 0xFF
                    g += (p ushr 8) and 0xFF
                    b += p and 0xFF
                    n++
                }
                dst[y * w + x] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }
}

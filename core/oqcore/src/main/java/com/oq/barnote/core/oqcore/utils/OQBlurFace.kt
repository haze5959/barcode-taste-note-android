package com.oq.barnote.core.oqcore.utils

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
 * 이미지의 사람 얼굴을 검출해 **모자이크(픽셀화)** 처리. iOS `OQBlurFace` 대응.
 *
 * iOS 는 CoreImage(`CIDetector` 얼굴 검출 + `CIPixellate` + `CIRadialGradient` 마스크 + `CIBlendWithMask`)
 * 로 구현했고, Android 엔 CoreImage 가 없어 다음으로 대체하되 파라미터·동작은 iOS 와 동일하게 맞춘다:
 * - **검출**: ML Kit On-device Face Detection (번들 모델 — 네트워크 불필요)
 * - **모자이크**: 얼굴 영역을 평균색으로 다운스케일 후 nearest-neighbor 로 업스케일해 또렷한 블록으로 만든다.
 *   블록 한 변 = `max(이미지 가로, 세로) / 50` (iOS `CIPixellate` 의 inputScale 과 동일).
 * - **합성**: 얼굴 중심에 반지름 `min(얼굴 가로, 세로) / 1.5` 의 원(iOS CIRadialGradient 와 동일)으로
 *   모자이크를 그려 원본 위에 올린다.
 *
 * 마스킹은 `drawCircle` + [BitmapShader] 로 처리한다. (RadialGradient + `PorterDuff.DST_IN` 알파 마스크는
 * 불투명 원본에서 스케일된 비트맵의 `hasAlpha()==false` 때문에 투명해야 할 가장자리가 '불투명 검정'으로
 * 렌더돼 **얼굴 위에 검은 네모박스**가 그려지는 버그가 있었음 → 원형 클립 셰이더로 원천 차단.)
 */
object OQBlurFace {

    /** iOS `CIPixellate` inputScale 과 동일: 모자이크 블록 한 변 = max(이미지 가로, 세로) / [PIXELLATE_DIVISOR]. */
    private const val PIXELLATE_DIVISOR = 50f

    /** iOS 와 동일: 얼굴을 가리는 원의 반지름 = min(얼굴 가로, 세로) / [FACE_RADIUS_DIVISOR]. */
    private const val FACE_RADIUS_DIVISOR = 1.5f

    /**
     * 얼굴이 있으면 모자이크 처리한 **새 비트맵**을, 없으면 `null` 을 반환한다.
     * (iOS `hasFaces()` + `blurFaces()` 를 한 번에 — 호출부는 null 이면 "얼굴 없음" 처리)
     */
    suspend fun blurFaces(bitmap: Bitmap): Bitmap? {
        val faces = runCatching { detectFaces(bitmap) }
            .onFailure { OQLog.e("OQBlurFace 검출 실패: ${it.message}") }
            .getOrDefault(emptyList())
        if (faces.isEmpty()) return null
        return withContext(Dispatchers.Default) { renderMosaic(bitmap, faces) }
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

    /** 검출된 얼굴들을 모자이크(픽셀화) + 원형 클립으로 원본 복사본에 합성. iOS `blurFaces()` 대응. */
    private fun renderMosaic(src: Bitmap, faces: List<Face>): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width
        val h = result.height
        // iOS: CIPixellate inputScale = max(extent.width, extent.height) / 50.
        val blockSize = max(1, (max(w, h) / PIXELLATE_DIVISOR).roundToInt())

        for (face in faces) {
            val box = face.boundingBox
            val centerX = box.exactCenterX()
            val centerY = box.exactCenterY()
            // iOS: radius = min(faceW, faceH) / 1.5.
            val radius = min(box.width(), box.height()) / FACE_RADIUS_DIVISOR
            if (radius < 1f) continue

            // 원이 포함되는 사각 영역만 모자이크 처리(성능). 이미지 경계로 클램프.
            val left = max(0, (centerX - radius).toInt())
            val top = max(0, (centerY - radius).toInt())
            val right = min(w, (centerX + radius).roundToInt())
            val bottom = min(h, (centerY + radius).roundToInt())
            val rw = right - left
            val rh = bottom - top
            if (rw <= 1 || rh <= 1) continue

            val region = Bitmap.createBitmap(result, left, top, rw, rh)
            val mosaic = pixelate(region, blockSize)

            // 모자이크 비트맵을 얼굴 위치에 정렬(translate)한 BitmapShader 로 원을 채워 그린다.
            // ANTI_ALIAS 로 원 가장자리만 매끄럽게(블록 내부는 1:1 매핑이라 또렷하게 유지).
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(mosaic, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                    setLocalMatrix(Matrix().apply { setTranslate(left.toFloat(), top.toFloat()) })
                }
            }
            canvas.drawCircle(centerX, centerY, radius, paint)

            region.recycle()
            mosaic.recycle()
        }
        return result
    }

    /**
     * 얼굴 영역을 [blockSizePx] 한 변 크기의 또렷한 모자이크 블록으로 만든다. iOS `CIPixellate` 대응.
     * 평균색으로 다운스케일(filter=true) → nearest-neighbor 업스케일(filter=false)해 블록 경계를 또렷하게 유지.
     */
    private fun pixelate(region: Bitmap, blockSizePx: Int): Bitmap {
        val rw = region.width
        val rh = region.height
        val block = max(1, blockSizePx)
        val sw = max(1, rw / block)
        val sh = max(1, rh / block)
        val small = Bitmap.createScaledBitmap(region, sw, sh, true)
        val mosaic = Bitmap.createScaledBitmap(small, rw, rh, false)
        if (small !== mosaic) small.recycle()
        return mosaic
    }
}

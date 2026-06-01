package com.oq.barnote.core.oqcore.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.oq.barnote.core.oqcore.utils.OQLog
import java.io.ByteArrayOutputStream

/**
 * 업로드용 이미지 최적화 유틸. iOS `UIImage.optimizeImageForUpload(toKBtye:)` 의 안드로이드 포팅.
 *
 * 알고리즘 (iOS 동일):
 * 1. 가장 긴 변이 [maxDimensionPx] 이하가 되도록 종횡비 유지 리사이징.
 * 2. JPEG 품질을 ~90 에서 시작해 10 씩 낮추며 압축, 결과가 [maxKb]*1024 바이트 이하가 되거나
 *    품질 하한 (~30) 에 도달할 때까지 반복.
 *
 * 대용량 이미지에서 OOM 을 피하기 위해 디코딩 시 `inSampleSize` 를 사용한다.
 * 디코딩/압축 중 어떤 실패가 발생해도 원본 [bytes] 를 그대로 반환한다 (`runCatching`).
 */
object OQImageOptimize {

    private const val QUALITY_START = 90
    private const val QUALITY_STEP = 10
    private const val QUALITY_FLOOR = 30

    /**
     * @param bytes 원본 이미지 바이트 (jpg/png/heic 등 [BitmapFactory] 디코딩 가능 포맷).
     * @param maxDimensionPx 가장 긴 변의 최대 픽셀. iOS 기본 720.
     * @param maxKb 목표 최대 용량 (KB). iOS 기본 200.
     * @return 최적화된 JPEG 바이트. 실패 시 원본 [bytes] 반환.
     */
    fun optimizeForUpload(
        bytes: ByteArray,
        maxDimensionPx: Int = 720,
        maxKb: Int = 200,
    ): ByteArray = runCatching {
        val maxFileSize = maxKb * 1024

        // 1. 원본 크기 측정 후 inSampleSize 로 1차 다운샘플 디코딩 (OOM 방지).
        val sized = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sized)
        if (sized.outWidth <= 0 || sized.outHeight <= 0) return@runCatching bytes

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sized.outWidth, sized.outHeight, maxDimensionPx)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return@runCatching bytes

        // 2. 가장 긴 변을 maxDimensionPx 이하로 정밀 스케일 (종횡비 유지).
        val processed = scaleToMaxDimension(decoded, maxDimensionPx)

        // 3. 품질을 점진적으로 낮추며 JPEG 압축.
        val output = ByteArrayOutputStream()
        var quality = QUALITY_START
        processed.compress(Bitmap.CompressFormat.JPEG, quality, output)
        OQLog.d("[Image resize] Initial size - ${output.size() / 1024}KB")

        while (output.size() > maxFileSize && quality > QUALITY_FLOOR) {
            quality -= QUALITY_STEP
            output.reset()
            processed.compress(Bitmap.CompressFormat.JPEG, quality, output)
            OQLog.d("[Image resize] resized size - ${output.size() / 1024}KB (q=$quality)")
        }

        // 스케일 결과 비트맵은 재활용. (decoded 와 동일 객체일 수 있으므로 분기.)
        if (processed !== decoded) decoded.recycle()
        processed.recycle()

        val result = output.toByteArray()
        OQLog.d("[Image resize] Complete resized size - ${result.size / 1024}KB")
        result
    }.getOrElse { e ->
        OQLog.e("Failed to optimize image, using original: $e")
        bytes
    }

    /**
     * 가장 긴 변이 [maxDimensionPx] 이하가 되는 가장 큰 2의 거듭제곱 샘플 크기.
     * 이후 [scaleToMaxDimension] 에서 정밀 스케일하므로 여기서는 절반 단위로만 줄인다.
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        var sampleSize = 1
        var longest = maxOf(width, height)
        // 정밀 스케일 여지를 남기기 위해 maxDimension 의 2배까지는 그대로 둔다.
        val target = maxDimensionPx * 2
        while (longest / 2 >= target) {
            longest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /** 가장 긴 변을 [maxDimensionPx] 이하로 종횡비 유지 스케일. 이미 작으면 원본 반환. */
    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimensionPx: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimensionPx) return bitmap
        val scale = maxDimensionPx.toFloat() / longest.toFloat()
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

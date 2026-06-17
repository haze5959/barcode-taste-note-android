package com.oq.barnote.core.oqcore.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oq.barnote.core.oqcore.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * iOS `Date.formattedByNow` 에 대응하는 상대 시간 포맷터.
 *
 * 입력: ISO8601 문자열 (서버 응답의 `registered` 필드 등)
 * 출력: "방금 전", "5분 전", "2시간 전", "3일 전", "2개월 전", "1년 전" 등
 *
 * locale 별 텍스트는 OQCore 의 strings.xml (`date_now`, `date_minutes_ago`, ...) 사용.
 */
object RelativeTime {

    @Composable
    fun formattedByNow(iso8601: String): String {
        val instant = runCatching { Instant.parse(iso8601) }.getOrNull()
            ?: return iso8601
        return formatInstant(instant)
    }

    @Composable
    private fun formatInstant(instant: Instant): String {
        val now = Instant.now()
        val duration = Duration.between(instant, now).abs()
        val seconds = duration.seconds

        return when {
            seconds < 60 -> stringResource(R.string.date_now)
            seconds < 3_600 -> stringResource(R.string.date_minutes_ago, (seconds / 60).toInt())
            seconds < 86_400 -> stringResource(R.string.date_hours_ago, (seconds / 3_600).toInt())
            seconds < 86_400 * 30 ->
                stringResource(R.string.date_days_ago, (seconds / 86_400).toInt())
            seconds < 86_400 * 365 ->
                stringResource(R.string.date_months_ago, (seconds / (86_400 * 30)).toInt())
            else -> stringResource(R.string.date_years_ago, (seconds / (86_400 * 365)).toInt())
        }
    }

    /** Composable 외부에서 사용. 영어 약식 ("just now", "5m ago" 같은 형태). */
    fun formatShort(iso8601: String): String {
        val instant = runCatching { Instant.parse(iso8601) }.getOrNull() ?: return iso8601
        val seconds = abs(Duration.between(instant, Instant.now()).seconds)
        return when {
            seconds < 60 -> "just now"
            seconds < 3_600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3_600}h ago"
            seconds < 86_400 * 30 -> "${seconds / 86_400}d ago"
            seconds < 86_400 * 365 -> "${seconds / (86_400 * 30)}mo ago"
            else -> "${seconds / (86_400 * 365)}y ago"
        }
    }
}

package com.oq.barnote.core.oqcore.utils

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 절대 날짜/시간 포맷 헬퍼. 상대 시간("5분 전")은 [RelativeTime] 을, 절대 표기는 여기를 사용.
 * 도메인 무관 — 어떤 앱에서도 재사용 가능.
 */
object OQDateFormat {

    /** ISO8601 문자열 → 지정 패턴 (시스템 시간대). 파싱 실패 시 [fallback] (기본 raw) 반환. */
    fun format(
        iso8601: String,
        pattern: String = "yyyy.MM.dd HH:mm",
        fallback: String? = null,
    ): String = runCatching {
        val zoned = Instant.parse(iso8601).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern(pattern).format(zoned)
    }.getOrDefault(fallback ?: iso8601)

    /** ISO8601 문자열 → 로케일 long 날짜 (예: "2026년 5월 29일" / "May 29, 2026"). */
    fun formatLocalizedDate(iso8601: String, locale: Locale = Locale.getDefault()): String =
        runCatching {
            val date = Instant.parse(iso8601).atZone(ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
        }.getOrDefault(iso8601)

    /** [LocalTime] → "HH:mm" (24시간 고정). 로케일 표기가 필요하면 [formatLocalizedTime] 사용. */
    fun formatTime(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm"))

    /**
     * [LocalTime] → 로케일 short 시간 (예: "오후 2:30" / "2:30 PM" / "14:30").
     * iOS `DateFormatter.timeStyle = .short` 대응.
     */
    fun formatLocalizedTime(time: LocalTime, locale: Locale = Locale.getDefault()): String =
        runCatching {
            time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
        }.getOrDefault(time.format(DateTimeFormatter.ofPattern("HH:mm")))

    /**
     * ISO8601 문자열 → 로케일 long 날짜 + short 시간 (예: "2026년 5월 29일 오후 3:07" / "May 29, 2026 at 3:07 PM").
     * iOS `Date.formattedWithTime` (= `.formatted(date: .abbreviated, time: .shortened)`) 대응.
     * 한국어 등에서 "2026년 5월 29일" 처럼 표기되도록 LONG 날짜 사용 (MEDIUM 은 "2026. 5. 29." 라 iOS 와 달랐음).
     * 노트 상세 작성일 / 데이터 내보내기 / 고객센터 신고 등 날짜+시간 표기에 사용.
     */
    fun formattedWithTime(iso8601: String, locale: Locale = Locale.getDefault()): String =
        runCatching {
            val zoned = Instant.parse(iso8601).atZone(ZoneId.systemDefault())
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                .withLocale(locale)
                .format(zoned)
        }.getOrDefault(iso8601)

    /**
     * ISO8601 문자열 → 로케일 short 날짜 + short 시간 (예: "26. 5. 29. 오후 3:07" / "5/29/26, 3:07 PM").
     * iOS `dateStyle=.short, timeStyle=.short` 대응. 예약 목록 등 컴팩트 표기에 사용.
     */
    fun formatShortDateTime(iso8601: String, locale: Locale = Locale.getDefault()): String =
        runCatching {
            val zoned = Instant.parse(iso8601).atZone(ZoneId.systemDefault())
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
                .withLocale(locale)
                .format(zoned)
        }.getOrDefault(iso8601)
}

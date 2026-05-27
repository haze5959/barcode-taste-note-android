package com.oq.barnote.core.oqcore.util

import java.util.Locale

/**
 * ISO 3166-1 alpha-2 코드(예: "kr", "jp") → 국기 이모지 + 지역명 변환 유틸.
 *
 * iOS `Country` 에 대응. 비표준 코드 "eu", "uk" 는 별도 처리합니다.
 *
 * `currentLocale` 결정 로직은 안드로이드에서는 호출자가 `Locale` 인스턴스를 직접 주입하는 형태로
 * 단순화했습니다. AppLanguage 설정 → Locale 변환은 [com.oq.barnote.core.oqcore.models.AppLanguage.toLocale] 를 사용하세요.
 */
object Country {

    /** "kr" → "🇰🇷 대한민국" 형태로 표시. 이름을 못 찾으면 국기만 반환. */
    fun display(code: String, locale: Locale = Locale.getDefault()): String {
        val flag = flagEmoji(code)
        val name = name(code, locale)
        return if (name.isEmpty()) flag else "$flag $name"
    }

    /** ISO alpha-2 코드를 국기 이모지로 변환. */
    fun flagEmoji(code: String): String {
        val normalized = code.lowercase(Locale.US)
        if (normalized == "eu") return "🇪🇺"
        if (normalized == "uk") return "🇬🇧"
        if (normalized.length != 2) return "🏳️"

        val base = 0x1F1E6 - 'A'.code // Regional Indicator Symbol 'A' offset
        val builder = StringBuilder()
        for (ch in normalized.uppercase(Locale.US)) {
            val v = ch.code
            if (v !in 'A'.code..'Z'.code) return "🏳️"
            builder.appendCodePoint(base + v)
        }
        return builder.toString()
    }

    /** ISO alpha-2 코드 → 주어진 locale 기준의 지역명. */
    fun name(code: String, locale: Locale = Locale.getDefault()): String {
        val normalized = code.uppercase(Locale.US)
        if (normalized == "EU") return "EU"
        val target = if (normalized == "UK") "GB" else normalized
        val display = Locale("", target).getDisplayCountry(locale)
        return display.ifEmpty { normalized }
    }
}

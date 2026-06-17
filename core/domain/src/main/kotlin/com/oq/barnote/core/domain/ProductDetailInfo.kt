package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 제품 상세 정보 항목. iOS `ProductDetailInfo` 에 대응.
 *
 * `displayValue` 는 raw 문자열을 화면 표시용으로 변환합니다.
 * 단, `Country` (ISO → 국기 + 지역명) 매핑은 안드로이드 Locale API 가 필요하므로
 * [com.oq.barnote.core.oqcore.utils.Country] 의 변환기를 [countryDisplay] 인자로 주입받습니다.
 */
@Serializable
enum class ProductDetailInfo(val rawValue: String) {
    @SerialName("style")
    Style("style"),

    /** wine 전용 */
    @SerialName("grape")
    Grape("grape"),

    @SerialName("manufacturer")
    Manufacturer("manufacturer"),

    @SerialName("country")
    Country("country"),

    @SerialName("alcohol")
    Alcohol("alcohol"),

    /** beer 전용 */
    @SerialName("ibu")
    Ibu("ibu");

    /**
     * 서버에서 받은 raw 문자열을 화면 표시용으로 변환합니다.
     *
     * - [Style] / [Grape]: Int 변환 후 enum 으로 매핑 (실패 시 raw 반환)
     * - [Manufacturer] / [Ibu]: 그대로 반환
     * - [Country]: [countryDisplay] 함수로 변환
     * - [Alcohol]: Int/Double 변환 후 % 부착
     */
    fun displayValue(
        raw: String,
        styleTitle: (ProductStyle) -> String,
        grapeTitle: (GrapeVariety) -> String,
        countryDisplay: (String) -> String,
    ): String = when (this) {
        Style -> raw.toIntOrNull()?.let { ProductStyle.fromRaw(it) }?.let(styleTitle) ?: raw
        Grape -> raw.toIntOrNull()?.let { GrapeVariety.fromRaw(it) }?.let(grapeTitle) ?: raw
        Manufacturer -> raw
        Country -> countryDisplay(raw)
        Alcohol -> {
            val intValue = raw.toIntOrNull()
            val doubleValue = raw.toDoubleOrNull()
            when {
                intValue != null -> "$intValue%"
                doubleValue != null -> {
                    if (doubleValue % 1.0 == 0.0) "${doubleValue.toInt()}%" else "$doubleValue%"
                }
                else -> raw
            }
        }
        Ibu -> raw
    }
}

package com.oq.barnote.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.vector.ImageVector
import com.oq.barnote.core.domain.ProductDetailInfo
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.PublicScope

/**
 * iOS Constants.swift 의 `systemImageName`/`systemName` (SF Symbol) → Material Icon 매핑.
 *
 * 1:1 대응되는 아이콘이 없는 경우 의미상 가장 가까운 Material Icon 으로 치환했습니다.
 * 모든 아이콘은 `material-icons-extended` 라이브러리에 포함되어 있습니다.
 */

// region ProductType -------------------------------------------------------

/**
 * iOS 매핑:
 * - wine/whisky/liqueur  → `wineglass.fill`  → [Icons.Filled.WineBar]
 * - beer/soju/other      → `mug.fill`        → [Icons.Filled.SportsBar]
 */
fun ProductType.icon(): ImageVector = when (this) {
    ProductType.Wine, ProductType.Whisky, ProductType.Liqueur -> Icons.Filled.WineBar
    ProductType.Beer, ProductType.Soju, ProductType.Other -> Icons.Filled.SportsBar
}

// endregion

// region PublicScope -------------------------------------------------------

/**
 * iOS 매핑:
 * - private      → `lock.fill`                    → [Icons.Filled.Lock]
 * - friendsOnly  → `person.2.badge.key.fill`      → [Icons.Filled.Group]
 *   (Material 에 사람+자물쇠 조합 아이콘이 없어 '사람 그룹' 의미인 Group 으로 치환)
 * - public       → `person.3.fill`                → [Icons.Filled.Groups]
 */
fun PublicScope.icon(): ImageVector = when (this) {
    PublicScope.Private -> Icons.Filled.Lock
    PublicScope.FriendsOnly -> Icons.Filled.Group
    PublicScope.Public -> Icons.Filled.Groups
}

// endregion

// region ProductDetailInfo -------------------------------------------------

/**
 * iOS 매핑:
 * - style         → `tag.fill`        → [Icons.Filled.Sell]
 * - grape         → `leaf`            → [Icons.Filled.Eco]
 * - manufacturer  → `building.2.fill` → [Icons.Filled.Business]
 * - country       → `globe`           → [Icons.Filled.Public]
 * - alcohol       → `drop.fill`       → [Icons.Filled.WaterDrop]
 * - ibu           → `gauge.medium`    → [Icons.Filled.Speed]
 */
fun ProductDetailInfo.icon(): ImageVector = when (this) {
    ProductDetailInfo.Style -> Icons.Filled.Sell
    ProductDetailInfo.Grape -> Icons.Filled.Eco
    ProductDetailInfo.Manufacturer -> Icons.Filled.Business
    ProductDetailInfo.Country -> Icons.Filled.Public
    ProductDetailInfo.Alcohol -> Icons.Filled.WaterDrop
    ProductDetailInfo.Ibu -> Icons.Filled.Speed
}

// endregion

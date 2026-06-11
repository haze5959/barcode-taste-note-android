package com.oq.barnote.ui.util

import com.oq.barnote.core.domain.Product
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면 간 [Product] 객체 in-memory 핸드오프. iOS `addNote(.init(product: product))` 처럼 객체를
 * 직접 전달해, AddNote 진입 시 제품 정보를 서버에서 재조회(getProductDetail)하지 않도록 한다.
 * Compose Navigation 라우트 인자에는 문자열(id)만 싣는 것이 안전하므로 객체는 여기로 건넨다.
 *
 * one-shot: [take] 는 호출 즉시 비운다 — 오래된 객체가 다른 진입에 새지 않는다.
 * 프로세스 재생성 등으로 비어 있으면 수신측이 기존처럼 서버 조회로 폴백한다.
 */
@Singleton
class ProductHandoff @Inject constructor() {
    private var pending: Product? = null

    /** AddNote 로 네비게이트하기 직전에 호출. */
    fun put(product: Product) {
        pending = product
    }

    /** [productId] 가 일치할 때만 반환. 어떤 경우든 1회 호출로 비워진다. */
    fun take(productId: String): Product? {
        val handed = pending?.takeIf { it.id == productId }
        pending = null
        return handed
    }
}

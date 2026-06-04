package com.oq.barnote.ui.tip

import androidx.annotation.StringRes
import com.oq.barnote.R

/**
 * 앱 안내 (tip) 카탈로그. iOS `BarnoteTips.swift` 에 대응.
 *
 * 새 tip 추가 시: enum 항목 + titleRes/messageRes 매핑만 정의.
 * 표시 위치는 Compose 에서 `BarNoteTip(tip = ..., anchor = { ... })` 패턴.
 *
 * 정책: 사용자가 한 번 dismiss 하면 영구 표시 안 함 ([TipPreferences] 가 영속화).
 */
enum class BarnoteTip(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int? = null, // null 이면 title-only (iOS 일부 Tip 대응).
) {
    /** MyPage 의 "노트 작성이 필요한 제품" Row 위에 안내. iOS `NeededNoteProductTip` 에 대응. */
    NeededNoteProduct(
        id = "needed_note_product",
        titleRes = R.string.masyeobon_jepum,
        messageRes = R.string.masyeobon_jepumeuro_deungroghago_sieumnoteureul_jagseonghaji,
    ),

    /** ProductDetail 하단 기본 CTA(마셔본 제품 등록) 위에 안내. iOS `TastedTip` 에 대응. */
    TastedProduct(
        id = "tasted_product",
        titleRes = R.string.masyeobon_jepum_deungrog,
        messageRes = R.string.masyeobon_jepumeuro_deungroghago_teiseuting_noteureul_jagseo,
    ),

    /** NoteDetail 공유 FAB 안내. iOS `NoteDetailShareTip` 에 대응. */
    NoteDetailShare(
        id = "note_detail_share",
        titleRes = R.string.gongyuhagi,
        messageRes = R.string.inseuta_web_kakaotogeuro_sieumnoteureul_gongyuhal_su_issseub,
    ),

    /** UserNoteList 공유 FAB 안내. iOS `UserNoteListShareTip` 에 대응. */
    UserNoteListShare(
        id = "user_note_list_share",
        titleRes = R.string.gongyuhagi,
        messageRes = R.string.inseuta_web_kakaotogeuro_nae_sieumnoteu_mogroggwa_jeulgyeoca,
    ),

    /** NoteList 보기 토글 FAB 안내 (title-only). iOS `NoteListViewToggleTip` 에 대응. */
    NoteListViewToggle(
        id = "note_list_view_toggle",
        titleRes = R.string.mogrogeul_ganryaghago_bbareuge_hwaginhaeboseyo,
    ),
}

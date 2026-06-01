package com.oq.barnote.ui.navigation

/**
 * Navigation 경로 상수 모음.
 *
 * 실제 화면이 구현되기 전 임시로 빈 화면 (`PlaceholderScreen`) 으로 연결되는 destination 도
 * 포함되어 있습니다. 추후 각 feature 모듈이 구현되면 라우트를 교체하면 됩니다.
 */
object Destinations {
    const val HOME = "home"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val RECENT_PRODUCT_LIST = "recent_product_list"

    // NoteList: ?isMine={isMine}
    const val NOTE_LIST = "note_list"
    const val NOTE_LIST_ARG_IS_MINE = "isMine"
    fun noteList(isMine: Boolean): String = "$NOTE_LIST?$NOTE_LIST_ARG_IS_MINE=$isMine"
    const val NOTE_LIST_ROUTE = "$NOTE_LIST?$NOTE_LIST_ARG_IS_MINE={$NOTE_LIST_ARG_IS_MINE}"

    // NoteDetail: /{id}?productName={productName}
    const val NOTE_DETAIL = "note_detail"
    const val NOTE_DETAIL_ARG_ID = "id"
    const val NOTE_DETAIL_ARG_PRODUCT_NAME = "productName"
    fun noteDetail(id: String, productName: String): String =
        "$NOTE_DETAIL/$id?$NOTE_DETAIL_ARG_PRODUCT_NAME=${encode(productName)}"
    const val NOTE_DETAIL_ROUTE =
        "$NOTE_DETAIL/{$NOTE_DETAIL_ARG_ID}?$NOTE_DETAIL_ARG_PRODUCT_NAME={$NOTE_DETAIL_ARG_PRODUCT_NAME}"

    // ProductDetail: /{id}?productName={productName}
    const val PRODUCT_DETAIL = "product_detail"
    const val PRODUCT_DETAIL_ARG_ID = "id"
    const val PRODUCT_DETAIL_ARG_PRODUCT_NAME = "productName"
    fun productDetail(id: String, productName: String): String =
        "$PRODUCT_DETAIL/$id?$PRODUCT_DETAIL_ARG_PRODUCT_NAME=${encode(productName)}"
    const val PRODUCT_DETAIL_ROUTE =
        "$PRODUCT_DETAIL/{$PRODUCT_DETAIL_ARG_ID}?$PRODUCT_DETAIL_ARG_PRODUCT_NAME={$PRODUCT_DETAIL_ARG_PRODUCT_NAME}"

    // --- Search --------------------------------------------------------------
    // Search 라우트는 키워드 자동 채움이 가능하도록 optional `keyword` query 를 지원.
    // 비어있으면 기본 빈 검색바로 진입, 채워있으면 자동으로 검색 수행 (SearchViewModel.OnAppear).
    const val SEARCH = "search"
    const val SEARCH_ARG_KEYWORD = "keyword"
    fun search(keyword: String? = null): String =
        if (keyword.isNullOrBlank()) SEARCH
        else "$SEARCH?$SEARCH_ARG_KEYWORD=${encode(keyword)}"
    const val SEARCH_ROUTE = "$SEARCH?$SEARCH_ARG_KEYWORD={$SEARCH_ARG_KEYWORD}"

    // AddProduct: ?barcode={barcode}&defaultName={defaultName}
    const val ADD_PRODUCT = "add_product"
    const val ADD_PRODUCT_ARG_BARCODE = "barcode"
    const val ADD_PRODUCT_ARG_DEFAULT_NAME = "defaultName"
    fun addProduct(barcode: String? = null, defaultName: String? = null): String {
        val params = buildList {
            if (barcode != null) add("$ADD_PRODUCT_ARG_BARCODE=$barcode")
            if (defaultName != null) add(
                "$ADD_PRODUCT_ARG_DEFAULT_NAME=${encode(defaultName)}",
            )
        }
        return if (params.isEmpty()) ADD_PRODUCT
        else "$ADD_PRODUCT?${params.joinToString("&")}"
    }
    const val ADD_PRODUCT_ROUTE =
        "$ADD_PRODUCT?$ADD_PRODUCT_ARG_BARCODE={$ADD_PRODUCT_ARG_BARCODE}&$ADD_PRODUCT_ARG_DEFAULT_NAME={$ADD_PRODUCT_ARG_DEFAULT_NAME}"

    // --- Settings ------------------------------------------------------------
    const val SETTINGS = "settings"
    const val RESERVATION_SETTINGS = "reservation_settings"
    const val CUSTOMER_CENTER = "customer_center"

    // WriteNote (AddNote): /{productId}
    const val WRITE_NOTE = "write_note"
    const val WRITE_NOTE_ARG_PRODUCT_ID = "productId"
    fun writeNote(productId: String): String = "$WRITE_NOTE/$productId"
    const val WRITE_NOTE_ROUTE = "$WRITE_NOTE/{$WRITE_NOTE_ARG_PRODUCT_ID}"

    // EditNote: /{noteId}
    const val EDIT_NOTE = "edit_note"
    const val EDIT_NOTE_ARG_NOTE_ID = "noteId"
    fun editNote(noteId: String): String = "$EDIT_NOTE/$noteId"
    const val EDIT_NOTE_ROUTE = "$EDIT_NOTE/{$EDIT_NOTE_ARG_NOTE_ID}"

    // UserSearch
    const val USER_SEARCH = "user_search"

    // AI 라벨 스캐너 (제품 자동 등록)
    const val AI_CAMERA = "ai_camera"

    // Followers / Following 빠른 헬퍼
    fun followersList(): String = userList("followers")

    // Report: ?productId={productId}
    const val REPORT = "report"
    const val REPORT_ARG_PRODUCT_ID = "productId"
    fun report(productId: String?): String =
        if (productId != null) "$REPORT?$REPORT_ARG_PRODUCT_ID=$productId" else REPORT
    const val REPORT_ROUTE = "$REPORT?$REPORT_ARG_PRODUCT_ID={$REPORT_ARG_PRODUCT_ID}"

    // --- MyPage --------------------------------------------------------------
    const val MY_PAGE = "my_page"
    const val USER_DETAIL = "user_detail"
    const val SUBSCRIBE = "subscribe"
    const val LOGIN = "login"
    const val NEEDED_REVIEW_NOTE_LIST = "needed_review_note_list"

    // ProductList: ?type={type}  (favorites / tasted)
    const val PRODUCT_LIST = "product_list"
    const val PRODUCT_LIST_ARG_TYPE = "type"
    fun productList(type: String): String = "$PRODUCT_LIST?$PRODUCT_LIST_ARG_TYPE=$type"
    const val PRODUCT_LIST_ROUTE = "$PRODUCT_LIST?$PRODUCT_LIST_ARG_TYPE={$PRODUCT_LIST_ARG_TYPE}"

    // UserList: ?type={type}  (following / followers)
    const val USER_LIST = "user_list"
    const val USER_LIST_ARG_TYPE = "type"
    fun userList(type: String): String = "$USER_LIST?$USER_LIST_ARG_TYPE=$type"
    const val USER_LIST_ROUTE = "$USER_LIST?$USER_LIST_ARG_TYPE={$USER_LIST_ARG_TYPE}"

    // UserNoteList: /{userId}
    const val USER_NOTE_LIST = "user_note_list"
    const val USER_NOTE_LIST_ARG_USER_ID = "userId"
    fun userNoteList(userId: String): String = "$USER_NOTE_LIST/$userId"
    const val USER_NOTE_LIST_ROUTE = "$USER_NOTE_LIST/{$USER_NOTE_LIST_ARG_USER_ID}"

    private fun encode(raw: String): String =
        java.net.URLEncoder.encode(raw, Charsets.UTF_8.name())
}

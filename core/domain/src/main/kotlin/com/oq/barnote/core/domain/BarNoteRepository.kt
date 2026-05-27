package com.oq.barnote.core.domain

/**
 * BarNote 앱의 전역 Repository.
 * iOS `BTNRepository` 프로토콜에 대응합니다.
 *
 * 에러 처리는 Kotlin 표준 [Result] 로 감쌉니다. 실패 시 [Result.exceptionOrNull] 은
 * 보통 `CommonError` (OQCore) 인스턴스이지만 호출자는 일반 [Throwable] 로 다뤄도 됩니다.
 *
 * 메서드 그룹:
 * - Etc (Home)
 * - Note
 * - Product
 * - User
 * - Image
 * - Report
 * - FCM
 */
interface BarNoteRepository {

    // region Etc -----------------------------------------------------------

    suspend fun getHomeInfo(): Result<HomeInfo>

    // endregion

    // region Note ----------------------------------------------------------

    suspend fun submitNote(noteDraft: NoteDraft): Result<Note>

    suspend fun editNote(id: String, noteDraft: NoteDraft): Result<Note>

    suspend fun deleteNote(id: String): Result<Boolean>

    suspend fun fetchUserNotes(
        userId: String,
        orderBy: NoteOrderByKey,
        index: Int,
    ): Result<List<NoteInfo>>

    suspend fun fetchNotes(
        index: Int,
        orderBy: NoteOrderByKey,
        productId: String? = null,
    ): Result<List<NoteInfo>>

    suspend fun fetchMyNotes(
        index: Int,
        per: Int,
        orderBy: NoteOrderByKey,
        includeUnrated: Boolean,
        productId: String? = null,
    ): Result<List<NoteInfo>>

    suspend fun fetchNotesWithNotRated(index: Int): Result<List<NoteInfo>>

    /** 특정 년/월에 작성된 노트 ID 들을 day -> [noteIds] 형태로 반환. */
    suspend fun fetchNoteIdsWithMonth(year: Int, month: Int): Result<Map<Int, List<String>>>

    suspend fun getNoteDetail(id: String): Result<NoteInfo>

    suspend fun getNoteDetails(ids: List<String>): Result<List<NoteInfo>>

    // endregion

    // region Product -------------------------------------------------------

    suspend fun getProductDetail(id: String): Result<ProductInfo>

    suspend fun fetchFavoriteProducts(
        userId: String?,
        index: Int,
        type: ProductType? = null,
    ): Result<List<ProductInfo>>

    suspend fun fetchProducts(
        search: String? = null,
        type: ProductType? = null,
        orderBy: ProductOrderByKey,
        index: Int,
    ): Result<List<ProductInfo>>

    suspend fun autocompleteProducts(
        search: String,
        type: ProductType? = null,
    ): Result<List<String>>

    suspend fun fetchTastedProducts(
        index: Int,
        type: ProductType? = null,
    ): Result<List<TastedProductInfo>>

    suspend fun findProduct(barcode: String): Result<ProductInfo>

    suspend fun createProduct(draft: ProductDraft): Result<Product>

    suspend fun createProductWithAI(
        imageId: String,
        barcodeId: String? = null,
    ): Result<Product>

    suspend fun favoriteProduct(id: String, isFavorite: Boolean): Result<Boolean>

    // endregion

    // region User ----------------------------------------------------------

    suspend fun getMyPage(): Result<MyPageInfo>

    suspend fun getMyInfo(): Result<UserInfo>

    suspend fun getUserInfo(id: String): Result<UserInfo>

    suspend fun searchUsers(nick: String, index: Int): Result<List<UserInfo>>

    suspend fun getMyFavoriteProductIds(): Result<List<String>>

    suspend fun updateNick(newNick: String? = null, newIntro: String? = null): Result<Boolean>

    suspend fun deleteMyInfo(): Result<Boolean>

    suspend fun followUser(userId: String): Result<Boolean>

    suspend fun unfollowUser(userId: String): Result<Boolean>

    suspend fun fetchFollowers(): Result<List<UserInfo>>

    suspend fun fetchFollowings(): Result<List<UserInfo>>

    // endregion

    // region Image ---------------------------------------------------------

    suspend fun uploadImage(attachment: MediaAttachment): Result<String>

    suspend fun uploadProfileImage(attachment: MediaAttachment): Result<String>

    suspend fun deleteImage(id: String): Result<Boolean>

    suspend fun fetchImageIds(
        page: Int,
        per: Int,
        productId: String? = null,
        noteId: String? = null,
    ): Result<List<String>>

    // endregion

    // region Report --------------------------------------------------------

    suspend fun report(productId: String?, body: String): Result<Report>

    suspend fun fetchReports(): Result<List<Report>>

    // endregion

    // region FCM -----------------------------------------------------------

    suspend fun registerFCMToken(
        token: String,
        userId: String,
        isActive: Boolean? = null,
    ): Result<Boolean>

    // endregion
}

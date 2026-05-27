package com.oq.barnote.core.data

import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.HomeInfo
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.MyPageInfo
import com.oq.barnote.core.domain.Note
import com.oq.barnote.core.domain.NoteDraft
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.ProductDraft
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.ProductOrderByKey
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.Report
import com.oq.barnote.core.domain.TastedProductInfo
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.network.BarNoteApi
import com.oq.barnote.core.oqcore.models.APIResponse
import com.oq.barnote.core.oqcore.models.APIResponseWithEmptyData
import com.oq.barnote.core.oqcore.models.CommonError
import com.oq.barnote.core.oqcore.network.NetworkError
import com.oq.barnote.core.oqcore.utils.OQLog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BarNote Repository 구현체. iOS `BTNRepositoryLive` 에 대응.
 *
 * - 인증 필요 여부에 따른 path 분기는 iOS 와 동일하게 [AuthStore.hasCredentials] 로 결정합니다.
 * - paging 기본 개수는 iOS `C.N.pagingCount` (= 10) 와 동일하게 [Constants.N.PAGING_COUNT] 사용.
 *   (app 모듈 의존을 피하기 위해 이 클래스는 직접 상수를 가지지 않고, 호출자가 per 를 명시하지 않으면
 *    [DEFAULT_PAGING_COUNT] 를 사용합니다.)
 */
@Singleton
class BarNoteRepositoryImpl @Inject constructor(
    private val api: BarNoteApi,
    private val authStore: AuthStore,
) : BarNoteRepository {

    // region Home ----------------------------------------------------------

    override suspend fun getHomeInfo(): Result<HomeInfo> = safeCall {
        api.getHomeInfo().unwrap()
    }

    // endregion

    // region Note ----------------------------------------------------------

    override suspend fun submitNote(noteDraft: NoteDraft): Result<Note> = safeCall {
        api.submitNote(noteDraft.toBody()).unwrap()
    }

    override suspend fun editNote(id: String, noteDraft: NoteDraft): Result<Note> = safeCall {
        api.editNote(id, noteDraft.toBody()).unwrap()
    }

    override suspend fun deleteNote(id: String): Result<Boolean> = safeCallBool {
        api.deleteNote(id)
    }

    override suspend fun fetchUserNotes(
        userId: String,
        orderBy: NoteOrderByKey,
        index: Int,
    ): Result<List<NoteInfo>> = safeCall {
        api.fetchUserNotes(
            userId = userId,
            params = mapOf(
                "page" to index,
                "per" to DEFAULT_PAGING_COUNT,
                "order_by" to orderBy.rawValue,
            ),
        ).unwrap()
    }

    override suspend fun fetchNotes(
        index: Int,
        orderBy: NoteOrderByKey,
        productId: String?,
    ): Result<List<NoteInfo>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", index)
            put("per", DEFAULT_PAGING_COUNT)
            put("order_by", orderBy.rawValue)
            productId?.let { put("product_id", it) }
        }
        api.fetchNotes(params).unwrap()
    }

    override suspend fun fetchMyNotes(
        index: Int,
        per: Int,
        orderBy: NoteOrderByKey,
        includeUnrated: Boolean,
        productId: String?,
    ): Result<List<NoteInfo>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", index)
            put("per", per)
            put("order_by", orderBy.rawValue)
            put("include_unrated", includeUnrated)
            productId?.let { put("product_id", it) }
        }
        api.fetchMyNotes(params).unwrap()
    }

    override suspend fun fetchNotesWithNotRated(index: Int): Result<List<NoteInfo>> = safeCall {
        api.fetchNotesWithNotRated(
            mapOf("page" to index, "per" to DEFAULT_PAGING_COUNT),
        ).unwrap()
    }

    override suspend fun fetchNoteIdsWithMonth(
        year: Int,
        month: Int,
    ): Result<Map<Int, List<String>>> = safeCall {
        // 서버는 day(Int) -> [noteId(String)] 를 JSON object 로 보내므로 키가 String 으로 옴.
        // Int 키로 정규화.
        api.fetchNoteIdsWithMonth(year, month).unwrap()
            .mapKeys { (k, _) -> k.toInt() }
    }

    override suspend fun getNoteDetail(id: String): Result<NoteInfo> = safeCall {
        api.getNoteDetail(id).unwrap()
    }

    override suspend fun getNoteDetails(ids: List<String>): Result<List<NoteInfo>> = safeCall {
        if (ids.isEmpty()) return@safeCall emptyList()
        api.fetchNotes(mapOf("ids" to ids.joinToString(","))).unwrap()
    }

    // endregion

    // region Product ------------------------------------------------------

    override suspend fun getProductDetail(id: String): Result<ProductInfo> = safeCall {
        val url = authedPath("products/$id")
        api.getProductDetail(url).unwrap()
    }

    override suspend fun fetchProducts(
        search: String?,
        type: ProductType?,
        orderBy: ProductOrderByKey,
        index: Int,
    ): Result<List<ProductInfo>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", index)
            put("per", DEFAULT_PAGING_COUNT)
            search?.let { put("name", it) }
            type?.let { put("type", it.rawValue) }
            put("order_by", orderBy.rawValue)
        }
        api.fetchProducts(params).unwrap()
    }

    override suspend fun autocompleteProducts(
        search: String,
        type: ProductType?,
    ): Result<List<String>> = safeCall {
        val params = buildMap<String, Any?> {
            put("search", search)
            type?.let { put("type", it.rawValue) }
        }
        api.autocompleteProducts(params).unwrap()
    }

    override suspend fun fetchFavoriteProducts(
        userId: String?,
        index: Int,
        type: ProductType?,
    ): Result<List<ProductInfo>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", index)
            put("per", DEFAULT_PAGING_COUNT)
            type?.let { put("type", it.rawValue) }
            userId?.let { put("user_id", it) }
        }
        // iOS 와 동일: userId 가 있으면 public, 없으면 본인용 /api/...
        val url = if (userId != null) "products/favorite" else "api/products/favorite"
        api.fetchFavoriteProducts(url, params).unwrap()
    }

    override suspend fun fetchTastedProducts(
        index: Int,
        type: ProductType?,
    ): Result<List<TastedProductInfo>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", index)
            put("per", DEFAULT_PAGING_COUNT)
            type?.let { put("type", it.rawValue) }
        }
        api.fetchTastedProducts(params).unwrap()
    }

    override suspend fun findProduct(barcode: String): Result<ProductInfo> = safeCall {
        val url = authedPath("products/barcode/$barcode")
        api.findProduct(url).unwrap()
    }

    override suspend fun createProduct(draft: ProductDraft): Result<Product> = safeCall {
        api.createProduct(draft.toBody()).unwrap()
    }

    override suspend fun createProductWithAI(
        imageId: String,
        barcodeId: String?,
    ): Result<Product> = safeCall {
        val params = buildMap<String, Any?> {
            put("image_id", imageId)
            barcodeId?.let { put("barcode_id", it) }
        }
        api.createProductWithAI(params).unwrap()
    }

    override suspend fun favoriteProduct(id: String, isFavorite: Boolean): Result<Boolean> =
        safeCallBool {
            api.favoriteProduct(mapOf("product_id" to id, "is_favorite" to isFavorite))
        }

    // endregion

    // region User ---------------------------------------------------------

    override suspend fun getMyPage(): Result<MyPageInfo> = safeCall {
        api.getMyPage().unwrap()
    }

    override suspend fun getMyInfo(): Result<UserInfo> = safeCall {
        api.getMyInfo().unwrap()
    }

    override suspend fun getUserInfo(id: String): Result<UserInfo> = safeCall {
        val url = authedPath("users/$id")
        api.getUserInfo(url).unwrap()
    }

    override suspend fun searchUsers(nick: String, index: Int): Result<List<UserInfo>> = safeCall {
        api.searchUsers(
            mapOf(
                "nick_name" to nick,
                "index" to index,
                "per" to DEFAULT_PAGING_COUNT,
            ),
        ).unwrap()
    }

    override suspend fun getMyFavoriteProductIds(): Result<List<String>> = safeCall {
        api.getMyFavoriteProductIds().unwrap()
    }

    override suspend fun updateNick(newNick: String?, newIntro: String?): Result<Boolean> =
        safeCallBool {
            val params = buildMap<String, Any?> {
                newNick?.let { put("nick_name", it) }
                newIntro?.let { put("intro", it) }
            }
            api.updateNick(params)
        }

    override suspend fun deleteMyInfo(): Result<Boolean> = safeCallBool {
        api.deleteMyInfo()
    }

    override suspend fun followUser(userId: String): Result<Boolean> = safeCallBool {
        api.followUser(mapOf("user_id" to userId))
    }

    override suspend fun unfollowUser(userId: String): Result<Boolean> = safeCallBool {
        api.unfollowUser(userId)
    }

    override suspend fun fetchFollowers(): Result<List<UserInfo>> = safeCall {
        api.fetchFollowers().unwrap()
    }

    override suspend fun fetchFollowings(): Result<List<UserInfo>> = safeCall {
        api.fetchFollowings().unwrap()
    }

    // endregion

    // region Image --------------------------------------------------------

    override suspend fun uploadImage(attachment: MediaAttachment): Result<String> = safeCall {
        val url = if (authStore.hasCredentials()) "api/images" else "images"
        api.uploadImage(url, attachment.toPart()).unwrap()
    }

    override suspend fun uploadProfileImage(attachment: MediaAttachment): Result<String> =
        safeCall { api.uploadProfileImage(attachment.toPart()).unwrap() }

    override suspend fun deleteImage(id: String): Result<Boolean> = safeCallBool {
        api.deleteImage(id)
    }

    override suspend fun fetchImageIds(
        page: Int,
        per: Int,
        productId: String?,
        noteId: String?,
    ): Result<List<String>> = safeCall {
        val params = buildMap<String, Any?> {
            put("page", page)
            put("per", per)
            productId?.let { put("product_id", it) }
            noteId?.let { put("note_id", it) }
        }
        api.fetchImageIds(params).unwrap()
    }

    // endregion

    // region Report -------------------------------------------------------

    override suspend fun report(productId: String?, body: String): Result<Report> = safeCall {
        val params = buildMap<String, Any?> {
            put("body", body)
            productId?.let { put("product_id", it) }
        }
        api.report(params).unwrap()
    }

    override suspend fun fetchReports(): Result<List<Report>> = safeCall {
        api.fetchReports().unwrap()
    }

    // endregion

    // region FCM ----------------------------------------------------------

    override suspend fun registerFCMToken(
        token: String,
        userId: String,
        isActive: Boolean?,
    ): Result<Boolean> = safeCallBool {
        val params = buildMap<String, Any?> {
            put("token", token)
            put("user_id", userId)
            isActive?.let { put("is_active", it) }
        }
        api.registerFCMToken(params)
    }

    // endregion

    // region Helpers ------------------------------------------------------

    /** auth 자격증명이 있으면 `api/` prefix 를 붙입니다. iOS 의 path 분기와 동일. */
    private suspend fun authedPath(path: String): String =
        if (authStore.hasCredentials()) "api/$path" else path

    private fun NoteDraft.toBody(): Map<String, Any?> = buildMap {
        put("product_id", productId)
        put("rating", rating)
        put("body", body)
        put("selected_flavors", selectedFlavors.map(Flavor::rawValue))
        put("image_ids", imageIds)
        put("public_scope", publicScope.rawValue)
        details?.let { put("details", it) }
    }

    private fun ProductDraft.toBody(): Map<String, Any?> = buildMap {
        put("name", name)
        put("desc", desc)
        put("type", type.rawValue)
        barcodeId?.let { put("barcode_id", it) }
        imageId?.let { put("image_id", it) }
    }

    private fun MediaAttachment.toPart(): MultipartBody.Part {
        val body = data.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", fileName, body)
    }

    companion object {
        /** iOS `C.N.pagingCount` 와 동일. core:data 가 app 의 Constants 에 의존하지 않도록 별도 상수. */
        const val DEFAULT_PAGING_COUNT: Int = 10
    }
}

// region APIResponse 확장 ---------------------------------------------------

/**
 * APIResponse&lt;T&gt; 를 풀어 T 를 꺼냅니다.
 * iOS [BTNRepositoryLive.responseMapper] 와 동일한 로직:
 * - result == true, data != null → 성공
 * - result == false, error != null → ApiError 로 실패
 * - 그 외 → 디코딩 에러
 */
@Throws(CommonError::class)
private fun <T> APIResponse<T>.unwrap(): T {
    if (result) {
        return data ?: run {
            OQLog.e("Invalid API response data")
            throw CommonError.Decoding
        }
    }
    val apiError = error ?: run {
        OQLog.e("Invalid API response error")
        throw CommonError.Decoding
    }
    throw CommonError.ApiError(apiError)
}

private fun APIResponseWithEmptyData.unwrap(): Boolean {
    if (result) return true
    val apiError = error ?: run {
        OQLog.e("Invalid API response error")
        throw CommonError.Decoding
    }
    throw CommonError.ApiError(apiError)
}

// endregion

// region safeCall ----------------------------------------------------------

/**
 * Repository 메서드에서 사용. iOS Result&lt;T, CommonError&gt; 패턴과 동등하게 Result&lt;T&gt; 로 wrap.
 * - 정상 응답: Result.success(value)
 * - CommonError: Result.failure(commonError)
 * - 그 외 Throwable: NetworkError.Transport 로 wrap 해서 CommonError.Network 로 보고.
 */
private inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CommonError) {
    OQLog.e(e)
    Result.failure(e)
} catch (e: Throwable) {
    OQLog.e(e)
    Result.failure(CommonError.Network(NetworkError.Transport(e)))
}

private inline fun safeCallBool(block: () -> Boolean): Result<Boolean> = safeCall(block)

// endregion

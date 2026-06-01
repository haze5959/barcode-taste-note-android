package com.oq.barnote.core.network

import com.oq.barnote.core.domain.HomeInfo
import com.oq.barnote.core.domain.MyPageInfo
import com.oq.barnote.core.domain.Note
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.Report
import com.oq.barnote.core.domain.TastedProductInfo
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.oqcore.models.APIResponse
import com.oq.barnote.core.oqcore.models.APIResponseWithEmptyData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * BarNote REST API.
 *
 * iOS [BTNRepositoryLive] 가 path 를 동적으로 결정하던 (`/api/...` vs `/...`) 패턴을
 * Retrofit 의 [@Url] 동적 URL 로 옮겼습니다. Repository 구현체에서 인증 상태에 따라
 * 적절한 path 를 전달합니다.
 *
 * 모든 응답은 [APIResponse] 래핑되어 있으며, body 가 없는 응답은 [APIResponseWithEmptyData] 사용.
 */
interface BarNoteApi {

    // region Home ----------------------------------------------------------

    @GET("btn/home")
    suspend fun getHomeInfo(): APIResponse<HomeInfo>

    // endregion

    // region Note ----------------------------------------------------------

    @POST("api/notes")
    suspend fun submitNote(@Body body: Map<String, @JvmSuppressWildcards Any?>): APIResponse<Note>

    @PUT("api/notes/{id}")
    suspend fun editNote(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<Note>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): APIResponseWithEmptyData

    @GET("notes/user/{userId}")
    suspend fun fetchUserNotes(
        @Path("userId") userId: String,
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<NoteInfo>>

    @GET("notes")
    suspend fun fetchNotes(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<NoteInfo>>

    @GET("api/notes")
    suspend fun fetchMyNotes(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<NoteInfo>>

    @GET("api/notes/not_rated")
    suspend fun fetchNotesWithNotRated(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<NoteInfo>>

    @GET("api/notes/calendar")
    suspend fun fetchNoteIdsWithMonth(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): APIResponse<Map<String, List<String>>>

    @GET("notes/{id}")
    suspend fun getNoteDetail(@Path("id") id: String): APIResponse<NoteInfo>

    // endregion

    // region Product ------------------------------------------------------

    /** auth 상태에 따라 `/api/products/{id}` 또는 `/products/{id}` 호출. */
    @GET
    suspend fun getProductDetail(@Url url: String): APIResponse<ProductInfo>

    @GET("products")
    suspend fun fetchProducts(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<ProductInfo>>

    @GET("products/autocomplete")
    suspend fun autocompleteProducts(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<String>>

    /** auth 여부에 따라 `/products/favorite` 또는 `/api/products/favorite`. */
    @GET
    suspend fun fetchFavoriteProducts(
        @Url url: String,
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<ProductInfo>>

    @GET("api/products/tasted")
    suspend fun fetchTastedProducts(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<TastedProductInfo>>

    /** auth 여부에 따라 `/api/products/barcode/{barcode}` 또는 `/products/barcode/{barcode}`. */
    @GET
    suspend fun findProduct(@Url url: String): APIResponse<ProductInfo>

    @POST("products")
    suspend fun createProduct(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<Product>

    @POST("api/products/ai")
    suspend fun createProductWithAI(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<Product>

    @POST("api/products/favorite")
    suspend fun favoriteProduct(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponseWithEmptyData

    // endregion

    // region User ----------------------------------------------------------

    @GET("api/users/mypage")
    suspend fun getMyPage(): APIResponse<MyPageInfo>

    @GET("api/users/me")
    suspend fun getMyInfo(): APIResponse<UserInfo>

    /** auth 여부에 따라 `/api/users/{id}` 또는 `/users/{id}`. */
    @GET
    suspend fun getUserInfo(@Url url: String): APIResponse<UserInfo>

    @GET("api/users/search")
    suspend fun searchUsers(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<UserInfo>>

    @GET("api/users/favorites")
    suspend fun getMyFavoriteProductIds(): APIResponse<List<String>>

    @PUT("api/users/me")
    suspend fun updateNick(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponseWithEmptyData

    @DELETE("api/users/me")
    suspend fun deleteMyInfo(): APIResponseWithEmptyData

    @POST("api/users/following")
    suspend fun followUser(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponseWithEmptyData

    @DELETE("api/users/following/{userId}")
    suspend fun unfollowUser(@Path("userId") userId: String): APIResponseWithEmptyData

    @GET("api/users/follower")
    suspend fun fetchFollowers(): APIResponse<List<UserInfo>>

    @GET("api/users/following")
    suspend fun fetchFollowings(): APIResponse<List<UserInfo>>

    @POST("api/users/fcm_token")
    suspend fun registerFCMToken(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponseWithEmptyData

    // endregion

    // region Image ---------------------------------------------------------

    /**
     * auth 여부에 따라 `/api/images` 또는 `/images`.
     *
     * iOS `NetworkClient.upload(path, id:, data:, name:)` 와 동일하게 client-generated UUID 를
     * `id` form part 로 함께 전송. 서버는 이 id 로 영수증 매칭 / 중복 업로드 차단에 활용 가능.
     */
    @Multipart
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Part("id") id: RequestBody,
        @Part image: MultipartBody.Part,
    ): APIResponse<String>

    @Multipart
    @POST("api/images/profile")
    suspend fun uploadProfileImage(
        @Part("id") id: RequestBody,
        @Part image: MultipartBody.Part,
    ): APIResponse<String>

    @DELETE("api/images/{id}")
    suspend fun deleteImage(@Path("id") id: String): APIResponseWithEmptyData

    @GET("images")
    suspend fun fetchImageIds(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<List<String>>

    // endregion

    // region Report --------------------------------------------------------

    @POST("api/btn/report")
    suspend fun report(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): APIResponse<Report>

    @GET("api/btn/report")
    suspend fun fetchReports(): APIResponse<List<Report>>

    // endregion
}

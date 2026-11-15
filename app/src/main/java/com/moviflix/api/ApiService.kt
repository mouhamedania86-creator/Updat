package com.moviflix.api

import com.moviflix.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("/user/app/email/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("/user/app/email/register/newCheck")
    suspend fun register(@Body body: RegisterRequest): Response<ApiResponse<LoginData>>

    @GET("/savita/auth/userInfo")
    suspend fun getUserInfo(): Response<ApiResponse<String>>

    @GET("/user/vip/myVipInfo")
    suspend fun getVipInfo(): Response<ApiResponse<VipInfo>>

    @GET("/user/vip/allWithExpire")
    suspend fun getAllVipInfo(): Response<ApiResponse<AllVipInfo>>

    @GET("/user/app/uid2/token/generate")
    suspend fun generateToken(): Response<ApiResponse<TokenData>>

    // ── Home ──────────────────────────────────────────────────────────────────
    @GET("/savita/homePage/getHome")
    suspend fun getHome(
        @Query("page") page: Int,
        @Query("navigationId") navigationId: Long = 1000000L,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<HomeData>>

    @GET("/cms/app/homePage/unreadQty")
    suspend fun getUnreadQty(): Response<ApiResponse<UnreadQty>>

    // ── Search ────────────────────────────────────────────────────────────────
    @GET("/aggregation/search/defaultPage")
    suspend fun getSearchDefault(
        @Query("size") size: Int = 10
    ): Response<ApiResponse<SearchDefaultData>>

    @GET("/aggregation/search/v2/searchContent")
    suspend fun searchContent(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<SearchResultData>>

    @GET("/aggregation/search/searchAll")
    suspend fun searchAll(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<SearchAllData>>

    @GET("/cms/app/search/v2/searchLenovo")
    suspend fun searchLenovo(
        @Query("keyword") keyword: String
    ): Response<ApiResponse<SearchSuggestData>>

    @GET("/aggregation/search/searchTag")
    suspend fun searchByTag(
        @Query("tagId") tagId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<SearchResultData>>

    // ── Movie/Drama detail ────────────────────────────────────────────────────
    @GET("/savita/movieDrama/get")
    suspend fun getContentDetail(
        @Query("id") id: String,
        @Query("category") category: Int,
        @Query("origin") origin: Int = 5
    ): Response<ApiResponse<String>>   // encrypted, decrypt with AES key

    // ── Episode play info ─────────────────────────────────────────────────────
    @GET("/savita/media/previewInfo")
    suspend fun getPreviewInfo(
        @Query("category") category: String,
        @Query("contentId") contentId: String,
        @Query("episodeId") episodeId: String,
        @Query("definition") definition: String = "GROOT_HD",
        @Query("projection") projection: Boolean = false,
        @Query("tryCode") tryCode: Int = 0
    ): Response<ApiResponse<String>>   // encrypted

    // ── Watch history ─────────────────────────────────────────────────────────
    @GET("/savita/behavior/app/findBatchWatchHistory")
    suspend fun getWatchHistory(
        @Query("contentIds") contentIds: String,
        @Query("category") category: Int
    ): Response<ApiResponse<List<WatchHistory>>>

    @POST("/user/behavior/app/addShortWatchHistory")
    suspend fun addWatchHistory(@Body body: WatchHistoryRequest): Response<ApiResponse<Any>>

    // ── Comments ──────────────────────────────────────────────────────────────
    @GET("/dynamic/app/talk/getCommentNum")
    suspend fun getCommentNum(
        @Query("contentId") contentId: String
    ): Response<ApiResponse<CommentNum>>

    @GET("/dynamic/app/talk/getTabConfig")
    suspend fun getTabConfig(
        @Query("category") category: Int,
        @Query("contentId") contentId: String
    ): Response<ApiResponse<TabConfig>>

    // ── Shorts ────────────────────────────────────────────────────────────────
    @GET("/cms/app/shorts/getShortsDetail")
    suspend fun getShortsDetail(
        @Query("shortsId") shortsId: String
    ): Response<ApiResponse<ShortsDetail>>

    @POST("/cms/app/shorts/batchGetShortsPlayInfo")
    suspend fun getShortsPlayInfo(@Body body: ShortsPlayInfoRequest): Response<ApiResponse<List<ShortsPlayInfo>>>

    // ── Country codes ─────────────────────────────────────────────────────────
    @GET("/user/mobile/countryCode/get")
    suspend fun getCountryCodes(): Response<ApiResponse<List<CountryCode>>>

    // ── Subscriptions ─────────────────────────────────────────────────────────
    @GET("/savita/message/subscribe")
    suspend fun getSubscriptions(): Response<ApiResponse<Any>>

    // ── Popup / promotions ────────────────────────────────────────────────────
    @GET("/user/app/popup/info")
    suspend fun getPopupInfo(): Response<ApiResponse<PopupInfo>>

    @GET("/user/app/popup/expiredVipDiscount/info")
    suspend fun getVipDiscountInfo(): Response<ApiResponse<VipDiscountInfo>>
}

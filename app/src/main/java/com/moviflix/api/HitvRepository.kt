package com.moviflix.api

import com.google.gson.Gson
import com.moviflix.models.*
import okhttp3.ResponseBody
import retrofit2.Response

class HitvRepository(
    private val api: ApiService,
    private val gson: Gson
) {

    // ── Auth ──────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String) =
        api.login(LoginRequest(email, password))

    suspend fun register(email: String, password: String, code: String) =
        api.register(RegisterRequest(email, password, code))

    suspend fun getUserInfo() = api.getUserInfo()
    suspend fun getVipInfo() = api.getVipInfo()

    // ── Home ──────────────────────────────────────────────────────────────────
    suspend fun getHome(page: Int) = api.getHome(page)
    suspend fun getUnreadQty() = api.getUnreadQty()

    // ── Search ────────────────────────────────────────────────────────────────
    suspend fun getSearchDefault() = api.getSearchDefault()
    suspend fun searchContent(keyword: String, page: Int = 1) =
        api.searchContent(keyword, page)
    suspend fun searchAll(keyword: String) = api.searchAll(keyword)
    suspend fun searchLenovo(keyword: String) = api.searchLenovo(keyword)
    suspend fun searchByTag(tagId: String, page: Int = 1) = api.searchByTag(tagId, page)

    // ── Content detail (encrypted response) ──────────────────────────────────
    suspend fun getContentDetail(id: String, category: Int): ContentDetail? {
        val response = api.getContentDetail(id, category)
        if (!response.isSuccessful) return null
        val body = response.body() ?: return null
        if (!body.isSuccess) return null
        val encData = body.data ?: return null
        val aesKey = response.raw().header("X-Raw-Aes-Key") ?: return null
        val json = NetworkModule.decryptData(encData, aesKey) ?: return null
        return gson.fromJson(json, ContentDetail::class.java)
    }

    // ── Play info (encrypted response) ───────────────────────────────────────
    suspend fun getPlayInfo(
        category: String,
        contentId: String,
        episodeId: String,
        quality: String = "GROOT_HD"
    ): PlayInfo? {
        val response = api.getPreviewInfo(category, contentId, episodeId, quality)
        if (!response.isSuccessful) return null
        val body = response.body() ?: return null
        if (!body.isSuccess) return null
        val encData = body.data ?: return null
        val aesKey = response.raw().header("X-Raw-Aes-Key") ?: return null
        val json = NetworkModule.decryptData(encData, aesKey) ?: return null
        return gson.fromJson(json, PlayInfo::class.java)
    }

    // ── Watch history ─────────────────────────────────────────────────────────
    suspend fun getWatchHistory(contentIds: List<String>, category: Int) =
        api.getWatchHistory(contentIds.joinToString(","), category)

    suspend fun addWatchHistory(
        contentId: String, episodeId: String,
        position: Long, duration: Long, category: Int
    ) = api.addWatchHistory(WatchHistoryRequest(contentId, episodeId, position, duration, category))

    // ── Comments ──────────────────────────────────────────────────────────────
    suspend fun getCommentNum(contentId: String) = api.getCommentNum(contentId)

    // ── Country codes ─────────────────────────────────────────────────────────
    suspend fun getCountryCodes() = api.getCountryCodes()

    // ── Popup ─────────────────────────────────────────────────────────────────
    suspend fun getPopupInfo() = api.getPopupInfo()
}

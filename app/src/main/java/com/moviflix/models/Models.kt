package com.moviflix.models

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Generic wrapper ───────────────────────────────────────────────────────────
data class ApiResponse<T>(
    @SerializedName("code") val code: String,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: T?
) {
    val isSuccess get() = code == "00000"
}

// ── Auth ──────────────────────────────────────────────────────────────────────
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phoneModel") val phoneModel: String = "MoviFlix Android",
    @SerializedName("phoneSystem") val phoneSystem: String = "android_15"
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("verifyCode") val verifyCode: String,
    @SerializedName("phoneModel") val phoneModel: String = "MoviFlix Android"
)

data class LoginData(
    @SerializedName("token") val token: String,
    @SerializedName("event") val event: String,
    @SerializedName("userInfo") val userInfo: UserInfo
)

data class UserInfo(
    @SerializedName("userId") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("nickName") val nickName: String,
    @SerializedName("headImgUrl") val headImgUrl: String,
    @SerializedName("loginType") val loginType: String,
    @SerializedName("userVipInfo") val userVipInfo: UserVipInfo? = null,
    @SerializedName("allVipInfo") val allVipInfo: AllVipInfo? = null
)

data class TokenData(
    @SerializedName("token") val token: String
)

// ── VIP ───────────────────────────────────────────────────────────────────────
data class VipInfo(
    @SerializedName("vipLevelId") val vipLevelId: Int? = null,
    @SerializedName("vipLevel") val vipLevel: Int? = null,
    @SerializedName("vipName") val vipName: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("payed") val payed: Boolean = false,
    @SerializedName("expireTime") val expireTime: Long? = null
)

data class UserVipInfo(
    @SerializedName("status") val status: Int = 0,
    @SerializedName("payed") val payed: Boolean = false,
    @SerializedName("vipName") val vipName: String = "",
    @SerializedName("expireTime") val expireTime: Long? = null,
    @SerializedName("headImgUrl") val headImgUrl: String = "",
    @SerializedName("nickName") val nickName: String = ""
)

data class AllVipInfo(
    @SerializedName("userId") val userId: Long = 0,
    @SerializedName("payed") val payed: Boolean = false,
    @SerializedName("otherVip") val otherVip: Boolean = false,
    @SerializedName("userVipInfos") val userVipInfos: List<VipInfo> = emptyList()
)

// ── Home ──────────────────────────────────────────────────────────────────────
data class HomeData(
    @SerializedName("page") val page: Int = 0,
    @SerializedName("recommendItems") val recommendItems: List<HomeItem> = emptyList(),
    @SerializedName("searchKeyWord") val searchKeyWord: String = "",
    @SerializedName("searchNewKeyWords") val searchNewKeyWords: List<SearchKeyword> = emptyList()
)

data class HomeItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("domainType") val domainType: Int = 0,
    @SerializedName("subType") val subType: String = "",
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo> = emptyList(),
    @SerializedName("navigationId") val navigationId: Long? = null,
    @SerializedName("navigationName") val navigationName: String? = null,
    @SerializedName("itemType") val itemType: Int? = null,
    @SerializedName("bannerList") val bannerList: List<BannerItem>? = null,
    @SerializedName("moduleTitle") val moduleTitle: String? = null,
    @SerializedName("contentList") val contentList: List<ContentItem>? = null
)

data class CornerInfo(
    @SerializedName("cornerType") val cornerType: Int = 0,
    @SerializedName("cornerTitle") val cornerTitle: String = ""
)

data class BannerItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("domainType") val domainType: Int = 0,
    @SerializedName("bannerImg") val bannerImg: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo> = emptyList()
)

data class ContentItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("domainType") val domainType: Int = 0,
    @SerializedName("subType") val subType: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo>? = null,
    @SerializedName("totalEpisodes") val totalEpisodes: Int? = null,
    @SerializedName("updateToEpisodes") val updateToEpisodes: Int? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("year") val year: Int? = null
)

data class SearchKeyword(
    @SerializedName("id") val id: String = "",
    @SerializedName("keyWord") val keyWord: String = "",
    @SerializedName("type") val type: Int = 0
)

data class UnreadQty(
    @SerializedName("fanQty") val fanQty: Int = 0,
    @SerializedName("stationQty") val stationQty: Int = 0,
    @SerializedName("unreadCommentQty") val unreadCommentQty: Int = 0,
    @SerializedName("unreadLikeQty") val unreadLikeQty: Int = 0
)

// ── Search ────────────────────────────────────────────────────────────────────
data class SearchDefaultData(
    @SerializedName("searchPlaceHolderVo") val placeholder: SearchPlaceholder? = null,
    @SerializedName("searchLeaderboard") val leaderboard: List<ContentItem> = emptyList()
)

data class SearchPlaceholder(
    @SerializedName("language") val language: String = "",
    @SerializedName("placeHolder") val placeHolder: String = ""
)

data class SearchResultData(
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 0,
    @SerializedName("totalPage") val totalPage: Int = 0,
    @SerializedName("list") val list: List<ContentItem> = emptyList()
)

data class SearchAllData(
    @SerializedName("starList") val starList: List<Any> = emptyList(),
    @SerializedName("albumList") val albumList: List<ContentItem>? = null,
    @SerializedName("liveList") val liveList: List<Any>? = null
)

data class SearchSuggestData(
    @SerializedName("list") val list: List<SuggestItem> = emptyList()
)

data class SuggestItem(
    @SerializedName("keyword") val keyword: String = "",
    @SerializedName("type") val type: Int = 0
)

// ── Content Detail (decrypted) ────────────────────────────────────────────────
data class ContentDetail(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("domainType") val domainType: Int = 0,
    @SerializedName("category") val category: Int = 0,
    @SerializedName("score") val score: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("totalEpisodes") val totalEpisodes: Int? = null,
    @SerializedName("updateToEpisodes") val updateToEpisodes: Int? = null,
    @SerializedName("updateStatus") val updateStatus: Int? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("actors") val actors: String? = null,
    @SerializedName("tags") val tags: List<TagItem>? = null,
    @SerializedName("episodeList") val episodeList: List<EpisodeItem>? = null,
    @SerializedName("seriesList") val seriesList: List<SeriesItem>? = null,
    @SerializedName("starList") val starList: List<StarItem>? = null,
    @SerializedName("horizontalCover") val horizontalCover: String? = null,
    @SerializedName("trailer") val trailer: String? = null,
    @SerializedName("languages") val languages: List<LanguageItem>? = null,
    @SerializedName("definitions") val definitions: List<DefinitionItem>? = null,
    @SerializedName("vipDefinitions") val vipDefinitions: List<DefinitionItem>? = null
)

data class TagItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = ""
)

data class EpisodeItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("episodeNo") val episodeNo: Int = 0,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("vipEpisode") val vipEpisode: Boolean = false,
    @SerializedName("tryEpisode") val tryEpisode: Boolean = false,
    @SerializedName("updateTime") val updateTime: Long? = null
)

data class SeriesItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("season") val season: Int = 0,
    @SerializedName("episodeList") val episodeList: List<EpisodeItem>? = null
)

data class StarItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("headImg") val headImg: String? = null,
    @SerializedName("role") val role: String? = null
)

data class LanguageItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("audioUrl") val audioUrl: String? = null
)

data class DefinitionItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("vip") val vip: Boolean = false
)

// ── Preview / Play info (decrypted) ───────────────────────────────────────────
data class PlayInfo(
    @SerializedName("url") val url: String = "",
    @SerializedName("urlList") val urlList: List<String>? = null,
    @SerializedName("drm") val drm: Boolean? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("subtitleList") val subtitleList: List<SubtitleItem>? = null,
    @SerializedName("definition") val definition: String? = null,
    @SerializedName("tryTime") val tryTime: Int? = null,
    @SerializedName("danmakuStatus") val danmakuStatus: Boolean? = null,
    @SerializedName("nextEpisodeId") val nextEpisodeId: String? = null,
    @SerializedName("codeType") val codeType: Int? = null
)

data class SubtitleItem(
    @SerializedName("languageCode") val languageCode: String = "",
    @SerializedName("languageName") val languageName: String = "",
    @SerializedName("url") val url: String = ""
)

// ── Watch history ─────────────────────────────────────────────────────────────
data class WatchHistory(
    @SerializedName("contentId") val contentId: String = "",
    @SerializedName("episodeId") val episodeId: String = "",
    @SerializedName("position") val position: Long = 0,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("updateTime") val updateTime: Long = 0
)

data class WatchHistoryRequest(
    @SerializedName("contentId") val contentId: String,
    @SerializedName("episodeId") val episodeId: String,
    @SerializedName("position") val position: Long,
    @SerializedName("duration") val duration: Long,
    @SerializedName("category") val category: Int
)

// ── Comments ──────────────────────────────────────────────────────────────────
data class CommentNum(
    @SerializedName("commented") val commented: Int = 0
)

data class TabConfig(
    @SerializedName("content") val content: List<Any> = emptyList()
)

// ── Shorts ────────────────────────────────────────────────────────────────────
data class ShortsDetail(
    @SerializedName("shortsId") val shortsId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("duration") val duration: Long = 0
)

data class ShortsPlayInfoRequest(
    @SerializedName("shortsIds") val shortsIds: List<String>
)

data class ShortsPlayInfo(
    @SerializedName("shortsId") val shortsId: String = "",
    @SerializedName("url") val url: String = ""
)

// ── Country code ──────────────────────────────────────────────────────────────
data class CountryCode(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("dialCode") val dialCode: String = "",
    @SerializedName("flag") val flag: String? = null
)

// ── Popups ────────────────────────────────────────────────────────────────────
data class PopupInfo(
    @SerializedName("type") val type: Int = 0,
    @SerializedName("url") val url: String? = null
)

data class VipDiscountInfo(
    @SerializedName("show") val show: Boolean = false,
    @SerializedName("discountRate") val discountRate: Int? = null
)

// ── Video qualities ───────────────────────────────────────────────────────────
enum class VideoQuality(val code: String, val label: String) {
    LD("GROOT_LD", "360p"),
    SD("GROOT_SD", "480p"),
    HD("GROOT_HD", "720p"),
    FHD("GROOT_FHD", "1080p")
}

// ── Room download entity ──────────────────────────────────────────────────────
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val episodeId: String,
    val contentId: String,
    val contentTitle: String,
    val episodeTitle: String,
    val coverUrl: String,
    val localPath: String,
    val subtitlePath: String?,
    val duration: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val quality: String = "GROOT_HD",
    val language: String = "ar",
    val status: Int = 0
)

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
    @SerializedName("userVipInfo") val userVipInfo: UserVipInfo?,
    @SerializedName("allVipInfo") val allVipInfo: AllVipInfo?
)

data class TokenData(
    @SerializedName("token") val token: String
)

// ── VIP ───────────────────────────────────────────────────────────────────────
data class VipInfo(
    @SerializedName("vipLevelId") val vipLevelId: Int?,
    @SerializedName("vipLevel") val vipLevel: Int?,
    @SerializedName("vipName") val vipName: String,
    @SerializedName("status") val status: Int,
    @SerializedName("payed") val payed: Boolean,
    @SerializedName("expireTime") val expireTime: Long?
)

data class UserVipInfo(
    @SerializedName("status") val status: Int,
    @SerializedName("payed") val payed: Boolean,
    @SerializedName("vipName") val vipName: String,
    @SerializedName("expireTime") val expireTime: Long?,
    @SerializedName("headImgUrl") val headImgUrl: String,
    @SerializedName("nickName") val nickName: String
)

data class AllVipInfo(
    @SerializedName("userId") val userId: Long,
    @SerializedName("payed") val payed: Boolean,
    @SerializedName("otherVip") val otherVip: Boolean,
    @SerializedName("userVipInfos") val userVipInfos: List<VipInfo>
)

// ── Home ──────────────────────────────────────────────────────────────────────
data class HomeData(
    @SerializedName("page") val page: Int,
    @SerializedName("recommendItems") val recommendItems: List<HomeItem>,
    @SerializedName("searchKeyWord") val searchKeyWord: String,
    @SerializedName("searchNewKeyWords") val searchNewKeyWords: List<SearchKeyword>
)

data class HomeItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("domainType") val domainType: Int,
    @SerializedName("subType") val subType: String,
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo>,
    @SerializedName("navigationId") val navigationId: Long?,
    @SerializedName("navigationName") val navigationName: String?,
    @SerializedName("itemType") val itemType: Int?,
    @SerializedName("bannerList") val bannerList: List<BannerItem>?,
    @SerializedName("moduleTitle") val moduleTitle: String?,
    @SerializedName("contentList") val contentList: List<ContentItem>?
)

data class CornerInfo(
    @SerializedName("cornerType") val cornerType: Int,
    @SerializedName("cornerTitle") val cornerTitle: String
)

data class BannerItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("domainType") val domainType: Int,
    @SerializedName("bannerImg") val bannerImg: String?,
    @SerializedName("score") val score: String?,
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo>
)

data class ContentItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("domainType") val domainType: Int,
    @SerializedName("subType") val subType: String?,
    @SerializedName("score") val score: String?,
    @SerializedName("cornerInfoList") val cornerInfoList: List<CornerInfo>?,
    @SerializedName("totalEpisodes") val totalEpisodes: Int?,
    @SerializedName("updateToEpisodes") val updateToEpisodes: Int?,
    @SerializedName("country") val country: String?,
    @SerializedName("year") val year: Int?
)

data class SearchKeyword(
    @SerializedName("id") val id: String,
    @SerializedName("keyWord") val keyWord: String,
    @SerializedName("type") val type: Int
)

data class UnreadQty(
    @SerializedName("fanQty") val fanQty: Int,
    @SerializedName("stationQty") val stationQty: Int,
    @SerializedName("unreadCommentQty") val unreadCommentQty: Int,
    @SerializedName("unreadLikeQty") val unreadLikeQty: Int
)

// ── Search ────────────────────────────────────────────────────────────────────
data class SearchDefaultData(
    @SerializedName("searchPlaceHolderVo") val placeholder: SearchPlaceholder,
    @SerializedName("searchLeaderboard") val leaderboard: List<ContentItem>
)

data class SearchPlaceholder(
    @SerializedName("language") val language: String,
    @SerializedName("placeHolder") val placeHolder: String
)

data class SearchResultData(
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalPage") val totalPage: Int,
    @SerializedName("list") val list: List<ContentItem>
)

data class SearchAllData(
    @SerializedName("starList") val starList: List<Any>,
    @SerializedName("albumList") val albumList: List<ContentItem>?,
    @SerializedName("liveList") val liveList: List<Any>?
)

data class SearchSuggestData(
    @SerializedName("list") val list: List<SuggestItem>
)

data class SuggestItem(
    @SerializedName("keyword") val keyword: String,
    @SerializedName("type") val type: Int
)

// ── Content Detail (decrypted) ────────────────────────────────────────────────
data class ContentDetail(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("domainType") val domainType: Int,
    @SerializedName("category") val category: Int,
    @SerializedName("score") val score: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("totalEpisodes") val totalEpisodes: Int?,
    @SerializedName("updateToEpisodes") val updateToEpisodes: Int?,
    @SerializedName("updateStatus") val updateStatus: Int?,
    @SerializedName("desc") val desc: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("actors") val actors: String?,
    @SerializedName("tags") val tags: List<TagItem>?,
    @SerializedName("episodeList") val episodeList: List<EpisodeItem>?,
    @SerializedName("seriesList") val seriesList: List<SeriesItem>?,
    @SerializedName("starList") val starList: List<StarItem>?,
    @SerializedName("horizontalCover") val horizontalCover: String?,
    @SerializedName("trailer") val trailer: String?,
    @SerializedName("languages") val languages: List<LanguageItem>?,
    @SerializedName("definitions") val definitions: List<DefinitionItem>?,
    @SerializedName("vipDefinitions") val vipDefinitions: List<DefinitionItem>?
)

data class TagItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class EpisodeItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("episodeNo") val episodeNo: Int,
    @SerializedName("cover") val cover: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("vipEpisode") val vipEpisode: Boolean,
    @SerializedName("tryEpisode") val tryEpisode: Boolean,
    @SerializedName("updateTime") val updateTime: Long?
)

data class SeriesItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("season") val season: Int,
    @SerializedName("episodeList") val episodeList: List<EpisodeItem>?
)

data class StarItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("headImg") val headImg: String?,
    @SerializedName("role") val role: String?
)

data class LanguageItem(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("audioUrl") val audioUrl: String?
)

data class DefinitionItem(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("vip") val vip: Boolean
)

// ── Preview / Play info (decrypted) ───────────────────────────────────────────
data class PlayInfo(
    @SerializedName("url") val url: String,
    @SerializedName("urlList") val urlList: List<String>?,
    @SerializedName("drm") val drm: Boolean?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("subtitleList") val subtitleList: List<SubtitleItem>?,
    @SerializedName("definition") val definition: String?,
    @SerializedName("tryTime") val tryTime: Int?,
    @SerializedName("danmakuStatus") val danmakuStatus: Boolean?,
    @SerializedName("nextEpisodeId") val nextEpisodeId: String?,
    @SerializedName("codeType") val codeType: Int?
)

data class SubtitleItem(
    @SerializedName("languageCode") val languageCode: String,
    @SerializedName("languageName") val languageName: String,
    @SerializedName("url") val url: String
)

// ── Watch history ─────────────────────────────────────────────────────────────
data class WatchHistory(
    @SerializedName("contentId") val contentId: String,
    @SerializedName("episodeId") val episodeId: String,
    @SerializedName("position") val position: Long,
    @SerializedName("duration") val duration: Long,
    @SerializedName("updateTime") val updateTime: Long
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
    @SerializedName("commented") val commented: Int
)

data class TabConfig(
    @SerializedName("content") val content: List<Any>
)

// ── Shorts ────────────────────────────────────────────────────────────────────
data class ShortsDetail(
    @SerializedName("shortsId") val shortsId: String,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("duration") val duration: Long
)

data class ShortsPlayInfoRequest(
    @SerializedName("shortsIds") val shortsIds: List<String>
)

data class ShortsPlayInfo(
    @SerializedName("shortsId") val shortsId: String,
    @SerializedName("url") val url: String
)

// ── Country code ──────────────────────────────────────────────────────────────
data class CountryCode(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("dialCode") val dialCode: String,
    @SerializedName("flag") val flag: String?
)

// ── Popups ────────────────────────────────────────────────────────────────────
data class PopupInfo(
    @SerializedName("type") val type: Int,
    @SerializedName("url") val url: String?
)

data class VipDiscountInfo(
    @SerializedName("show") val show: Boolean,
    @SerializedName("discountRate") val discountRate: Int?
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

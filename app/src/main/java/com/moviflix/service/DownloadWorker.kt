package com.moviflix.service

import android.content.Context
import androidx.work.*
import com.moviflix.models.DownloadEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_EPISODE_ID = "episodeId"
        const val KEY_CONTENT_ID = "contentId"
        const val KEY_CONTENT_TITLE = "contentTitle"
        const val KEY_EPISODE_TITLE = "episodeTitle"
        const val KEY_COVER_URL = "coverUrl"
        const val KEY_M3U8_URL = "m3u8Url"
        const val KEY_SUBTITLE_URL = "subtitleUrl"
        const val KEY_DURATION = "duration"
        const val KEY_QUALITY = "quality"
        const val KEY_LANGUAGE = "language"

        fun buildRequest(
            episodeId: String,
            contentId: String,
            contentTitle: String,
            episodeTitle: String,
            coverUrl: String,
            m3u8Url: String,
            subtitleUrl: String? = null,
            duration: Long = 0,
            quality: String = "GROOT_HD",
            language: String = "ar"
        ): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_EPISODE_ID to episodeId,
                KEY_CONTENT_ID to contentId,
                KEY_CONTENT_TITLE to contentTitle,
                KEY_EPISODE_TITLE to episodeTitle,
                KEY_COVER_URL to coverUrl,
                KEY_M3U8_URL to m3u8Url,
                KEY_SUBTITLE_URL to (subtitleUrl ?: ""),
                KEY_DURATION to duration,
                KEY_QUALITY to quality,
                KEY_LANGUAGE to language
            )
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("download_$episodeId")
                .build()
        }
    }

    private val db = DownloadDatabase.getInstance(applicationContext)
    private val client = OkHttpClient.Builder().build()

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return Result.failure()
        val contentId = inputData.getString(KEY_CONTENT_ID) ?: return Result.failure()
        val contentTitle = inputData.getString(KEY_CONTENT_TITLE) ?: ""
        val episodeTitle = inputData.getString(KEY_EPISODE_TITLE) ?: ""
        val coverUrl = inputData.getString(KEY_COVER_URL) ?: ""
        val m3u8Url = inputData.getString(KEY_M3U8_URL) ?: return Result.failure()
        val subtitleUrl = inputData.getString(KEY_SUBTITLE_URL)?.takeIf { it.isNotEmpty() }
        val duration = inputData.getLong(KEY_DURATION, 0)
        val quality = inputData.getString(KEY_QUALITY) ?: "GROOT_HD"
        val language = inputData.getString(KEY_LANGUAGE) ?: "ar"

        // Create download dir
        val dir = File(applicationContext.filesDir, "downloads/$episodeId")
        dir.mkdirs()

        // Update DB → downloading
        db.downloadDao().insertDownload(
            DownloadEntity(
                episodeId = episodeId,
                contentId = contentId,
                contentTitle = contentTitle,
                episodeTitle = episodeTitle,
                coverUrl = coverUrl,
                localPath = dir.absolutePath,
                subtitlePath = null,
                duration = duration,
                quality = quality,
                language = language,
                status = 1
            )
        )

        return try {
            // Parse m3u8 and download all .ts segments
            val segmentUrls = fetchM3u8Segments(m3u8Url)
            val outputFile = File(dir, "video.ts")
            FileOutputStream(outputFile).use { out ->
                segmentUrls.forEachIndexed { index, segUrl ->
                    setProgress(workDataOf(
                        "progress" to ((index + 1) * 100 / segmentUrls.size),
                        "episodeId" to episodeId
                    ))
                    downloadSegment(segUrl, out)
                }
            }

            // Download subtitle if present
            var subtitlePath: String? = null
            if (!subtitleUrl.isNullOrEmpty()) {
                val subFile = File(dir, "subtitle.srt")
                downloadFile(subtitleUrl, subFile)
                subtitlePath = subFile.absolutePath
            }

            // Update DB → done
            db.downloadDao().updateDownload(
                DownloadEntity(
                    episodeId = episodeId,
                    contentId = contentId,
                    contentTitle = contentTitle,
                    episodeTitle = episodeTitle,
                    coverUrl = coverUrl,
                    localPath = outputFile.absolutePath,
                    subtitlePath = subtitlePath,
                    duration = duration,
                    quality = quality,
                    language = language,
                    status = 2
                )
            )
            Result.success()
        } catch (e: Exception) {
            db.downloadDao().updateDownload(
                DownloadEntity(
                    episodeId = episodeId,
                    contentId = contentId,
                    contentTitle = contentTitle,
                    episodeTitle = episodeTitle,
                    coverUrl = coverUrl,
                    localPath = dir.absolutePath,
                    subtitlePath = null,
                    duration = duration,
                    quality = quality,
                    language = language,
                    status = 3
                )
            )
            Result.failure()
        }
    }

    private fun fetchM3u8Segments(m3u8Url: String): List<String> {
        val request = Request.Builder().url(m3u8Url)
            .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 15; MoviFlix)")
            .build()
        val body = client.newCall(request).execute().body?.string() ?: return emptyList()

        val baseUrl = m3u8Url.substringBeforeLast("/") + "/"
        val segments = mutableListOf<String>()

        // If this is a master playlist, resolve the best sub-playlist
        if (body.contains("#EXT-X-STREAM-INF")) {
            val subPlaylistLine = body.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .firstOrNull() ?: return emptyList()
            val subUrl = if (subPlaylistLine.startsWith("http")) subPlaylistLine
            else baseUrl + subPlaylistLine
            return fetchM3u8Segments(subUrl)
        }

        body.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val segUrl = if (trimmed.startsWith("http")) trimmed
                else baseUrl + trimmed
                segments.add(segUrl)
            }
        }
        return segments
    }

    private fun downloadSegment(url: String, out: FileOutputStream) {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 15; MoviFlix)")
            .build()
        client.newCall(request).execute().use { response ->
            response.body?.byteStream()?.use { stream ->
                stream.copyTo(out)
            }
        }
    }

    private fun downloadFile(url: String, dest: File) {
        val request = Request.Builder().url(url)
            .header("User-Agent", "okhttp/4.12.0")
            .build()
        client.newCall(request).execute().use { response ->
            dest.outputStream().use { out ->
                response.body?.byteStream()?.copyTo(out)
            }
        }
    }
}

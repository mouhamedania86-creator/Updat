package com.moviflix.ui.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.moviflix.R
import com.moviflix.api.HitvRepository
import com.moviflix.models.PlayInfo
import com.moviflix.models.SubtitleItem
import com.moviflix.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTENT_ID = "contentId"
        const val EXTRA_EPISODE_ID = "episodeId"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LOCAL_PATH = "localPath"       // offline playback
        const val EXTRA_LOCAL_SUBTITLE = "localSubtitle"

        fun launch(
            ctx: Context,
            contentId: String,
            episodeId: String,
            category: Int,
            title: String
        ) {
            ctx.startActivity(Intent(ctx, PlayerActivity::class.java).apply {
                putExtra(EXTRA_CONTENT_ID, contentId)
                putExtra(EXTRA_EPISODE_ID, episodeId)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_TITLE, title)
            })
        }

        fun launchOffline(ctx: Context, localPath: String, title: String, subtitle: String? = null) {
            ctx.startActivity(Intent(ctx, PlayerActivity::class.java).apply {
                putExtra(EXTRA_LOCAL_PATH, localPath)
                putExtra(EXTRA_TITLE, title)
                subtitle?.let { putExtra(EXTRA_LOCAL_SUBTITLE, it) }
            })
        }
    }

    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var controlsContainer: View
    private lateinit var ibPlay: ImageButton
    private lateinit var ibBack: ImageButton
    private lateinit var ibForward: ImageButton
    private lateinit var ibSettings: ImageButton
    private lateinit var sbProgress: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvScrubOverlay: TextView
    private lateinit var brightnessBar: SeekBar
    private lateinit var volumeBar: SeekBar

    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var session: SessionManager
    private lateinit var repo: HitvRepository

    private var currentPlayInfo: PlayInfo? = null
    private var currentAudioTrackIndex = 0
    private var currentSubtitleUrl: String? = null

    private val audioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private var hideControlsRunnable = Runnable { hideControls() }
    private var progressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)
        hideSystemUI()

        session = SessionManager(this)

        bindViews()
        setupGestures()

        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        tvTitle.text = title

        if (!localPath.isNullOrEmpty()) {
            val localSub = intent.getStringExtra(EXTRA_LOCAL_SUBTITLE)
            playLocal(localPath, localSub)
        } else {
            val contentId = intent.getStringExtra(EXTRA_CONTENT_ID) ?: return finish()
            val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID) ?: return finish()
            val category = intent.getIntExtra(EXTRA_CATEGORY, 1)
            fetchAndPlay(contentId, episodeId, category)
        }
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        tvTitle = findViewById(R.id.tvPlayerTitle)
        tvSubtitle = findViewById(R.id.tvSubtitleText)
        pbLoading = findViewById(R.id.pbPlayerLoading)
        controlsContainer = findViewById(R.id.controlsContainer)
        ibPlay = findViewById(R.id.ibPlayPause)
        ibBack = findViewById(R.id.ibSeekBack)
        ibForward = findViewById(R.id.ibSeekForward)
        ibSettings = findViewById(R.id.ibSettings)
        sbProgress = findViewById(R.id.sbProgress)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        tvScrubOverlay = findViewById(R.id.tvScrubOverlay)
        brightnessBar = findViewById(R.id.brightnessBar)
        volumeBar = findViewById(R.id.volumeBar)

        // Init volume bar
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar.max = maxVol
        volumeBar.progress = curVol

        // Init brightness bar
        val curBright = try {
            android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) { 128 }
        brightnessBar.max = 255
        brightnessBar.progress = curBright

        // Wire back button
        findViewById<ImageButton>(R.id.ibPlayerBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ibPlay.setOnClickListener { togglePlayPause() }
        ibBack.setOnClickListener { seekBy(-10_000) }
        ibForward.setOnClickListener { seekBy(10_000) }
        ibSettings.setOnClickListener { showSettingsMenu() }

        sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0
                    val pos = (duration * progress / 1000L)
                    tvCurrentTime.text = formatMs(pos)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) { handler.removeCallbacks(hideControlsRunnable) }
            override fun onStopTrackingTouch(sb: SeekBar) {
                val duration = player?.duration ?: 0
                player?.seekTo(duration * sb.progress / 1000L)
                scheduleHideControls()
            }
        })

        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        brightnessBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val lp = window.attributes
                    lp.screenBrightness = progress / 255f
                    window.attributes = lp
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun fetchAndPlay(contentId: String, episodeId: String, category: Int) {
        pbLoading.visibility = View.VISIBLE

        // Build repo inline here (or inject via App)
        val app = application as com.moviflix.MoviFlixApp
        repo = app.repository

        lifecycleScope.launch {
            val quality = session.defaultQuality
            val playInfo = withContext(Dispatchers.IO) {
                repo.getPlayInfo(category.toString(), contentId, episodeId, quality)
            }
            pbLoading.visibility = View.GONE
            if (playInfo == null) {
                Toast.makeText(this@PlayerActivity, "فشل تحميل الفيديو", Toast.LENGTH_LONG).show()
                return@launch
            }
            currentPlayInfo = playInfo
            startHlsPlayback(playInfo)
        }
    }

    private fun playLocal(path: String, subtitlePath: String?) {
        pbLoading.visibility = View.GONE
        val mediaItem = buildMediaItem("file://$path", subtitlePath?.let { "file://$it" })
        setupPlayer(mediaItem)
    }

    private fun startHlsPlayback(info: PlayInfo) {
        val subUrl = info.subtitleList
            ?.firstOrNull { it.languageCode == session.subtitleLanguage }
            ?.url
            ?: info.subtitleList?.firstOrNull()?.url
        currentSubtitleUrl = subUrl

        val mediaItem = buildMediaItem(info.url, subUrl)
        setupPlayer(mediaItem)
    }

    private fun buildMediaItem(url: String, subtitleUrl: String?): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (!subtitleUrl.isNullOrEmpty()) {
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(session.subtitleLanguage)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        return builder.build()
    }

    private fun setupPlayer(mediaItem: MediaItem) {
        player?.release()

        val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent("Dalvik/2.1.0 (Linux; U; Android 15; MoviFlix)")

        player = ExoPlayer.Builder(this)
            .build()
            .also { exo ->
                playerView.player = exo

                val source = HlsMediaSource.Factory(dataSource)
                    .createMediaSource(mediaItem)

                exo.setMediaSource(source)
                exo.prepare()
                exo.playWhenReady = true

                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> pbLoading.visibility = View.VISIBLE
                            Player.STATE_READY -> {
                                pbLoading.visibility = View.GONE
                                val dur = exo.duration
                                tvTotalTime.text = formatMs(dur)
                                startProgressUpdater()
                            }
                            Player.STATE_ENDED -> finish()
                            else -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        ibPlay.setImageResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        )
                    }

                    override fun onCues(cueGroup: CueGroup) {
                        val text = cueGroup.cues.joinToString("\n") { it.text?.toString() ?: "" }
                        tvSubtitle.text = text
                        tvSubtitle.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
                    }
                })
            }

        showControls()
        scheduleHideControls()
    }

    private fun startProgressUpdater() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = object : Runnable {
            override fun run() {
                val exo = player ?: return
                val pos = exo.currentPosition
                val dur = exo.duration.coerceAtLeast(1)
                sbProgress.progress = (pos * 1000 / dur).toInt()
                tvCurrentTime.text = formatMs(pos)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        scheduleHideControls()
    }

    private fun seekBy(ms: Long) {
        player?.let {
            val pos = (it.currentPosition + ms).coerceAtLeast(0)
            it.seekTo(pos)
        }
    }

    private fun showControls() {
        controlsContainer.visibility = View.VISIBLE
        handler.removeCallbacks(hideControlsRunnable)
    }

    private fun hideControls() {
        controlsContainer.visibility = View.GONE
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3500)
    }

    private fun showSettingsMenu() {
        val info = currentPlayInfo ?: return
        val items = mutableListOf<String>()

        // Quality options
        items.add("الجودة: 360p")
        items.add("الجودة: 480p")
        items.add("الجودة: 720p")
        items.add("الجودة: 1080p")

        // Subtitle options
        info.subtitleList?.forEach { sub ->
            items.add("ترجمة: ${sub.languageName}")
        }
        items.add("إخفاء الترجمة")

        android.app.AlertDialog.Builder(this)
            .setTitle("الإعدادات")
            .setItems(items.toTypedArray()) { _, which ->
                when {
                    which < 4 -> {
                        val qualityMap = mapOf(0 to "GROOT_LD", 1 to "GROOT_SD", 2 to "GROOT_HD", 3 to "GROOT_FHD")
                        val newQuality = qualityMap[which] ?: return@setItems
                        // Re-fetch play info with new quality
                        val cid = intent.getStringExtra(EXTRA_CONTENT_ID) ?: return@setItems
                        val eid = intent.getStringExtra(EXTRA_EPISODE_ID) ?: return@setItems
                        val cat = intent.getIntExtra(EXTRA_CATEGORY, 1)
                        val pos = player?.currentPosition ?: 0
                        lifecycleScope.launch {
                            val newInfo = withContext(Dispatchers.IO) {
                                repo.getPlayInfo(cat.toString(), cid, eid, newQuality)
                            }
                            newInfo?.let {
                                currentPlayInfo = it
                                startHlsPlayback(it)
                                player?.seekTo(pos)
                            }
                        }
                    }
                    else -> {
                        val subIndex = which - 4
                        if (subIndex < (info.subtitleList?.size ?: 0)) {
                            val sub = info.subtitleList!![subIndex]
                            // Switch subtitle by rebuilding media item with new sub URL
                            val pos = player?.currentPosition ?: 0
                            val item = buildMediaItem(info.url, sub.url)
                            player?.setMediaItem(item)
                            player?.prepare()
                            player?.seekTo(pos)
                            player?.play()
                        } else {
                            tvSubtitle.visibility = View.GONE
                        }
                    }
                }
            }.show()
    }

    // ── Gesture detector: tap=toggle controls, double-tap=seek, swipe=brightness/volume ──
    private lateinit var gestureDetector: GestureDetector

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (controlsContainer.visibility == View.VISIBLE) hideControls()
                else { showControls(); scheduleHideControls() }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val width = playerView.width
                val seekMs = if (e.x < width / 2) -10_000L else 10_000L
                seekBy(seekMs)
                tvScrubOverlay.text = if (seekMs < 0) "⏪ 10s" else "⏩ 10s"
                tvScrubOverlay.visibility = View.VISIBLE
                handler.postDelayed({ tvScrubOverlay.visibility = View.GONE }, 800)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                val width = playerView.width
                val e1x = e1?.x ?: return false
                if (e1x < width / 2) {
                    // Left side → brightness
                    val delta = (-distanceY / playerView.height * 255).toInt()
                    brightnessBar.progress = (brightnessBar.progress + delta).coerceIn(0, 255)
                    val lp = window.attributes
                    lp.screenBrightness = brightnessBar.progress / 255f
                    window.attributes = lp
                } else {
                    // Right side → volume
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val delta = (-distanceY / playerView.height * max).toInt()
                    val newVol = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + delta).coerceIn(0, max)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    volumeBar.progress = newVol
                }
                return true
            }
        })

        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(hideControlsRunnable)
        player?.release()
        player = null
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun formatMs(ms: Long): String {
        if (ms < 0) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }
}

package com.snapflow.downloader

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import java.io.File
import java.net.URI

/** Dark violet interface matched to the agreed SnapFlow preview, backed by real transfers. */
class MainActivity : Activity() {
    private lateinit var engine: DownloadEngine
    private lateinit var siteEngine: SiteMediaEngine
    private var siteBusy = false
    private var siteProgress = 0
    private var siteStatus = ""
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var urlField: EditText
    private var activeTab = "Home"
    private var checking = false
    private var pendingMedia: DownloadEngine.Prepared? = null
    private val handler = Handler(Looper.getMainLooper())
    private val bgColor = Color.rgb(10, 12, 24)
    private val cardColor = Color.rgb(25, 28, 48)
    private val purple = Color.rgb(144, 82, 246)
    private val muted = Color.rgb(160, 164, 190)
    private val white = Color.WHITE
    private val ticker = object : Runnable {
        override fun run() {
            if (activeTab == "Downloads") showTab("Downloads")
            handler.postDelayed(this, 2500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = DownloadEngine(this)
        siteEngine = SiteMediaEngine(this)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }
        setContentView(root)
        showTab("Home")
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { urlField.setText(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showTab("Home")
        if (intent.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { urlField.setText(it) }
        }
    }
    override fun onResume() { super.onResume(); handler.removeCallbacks(ticker); handler.post(ticker) }
    override fun onPause() { handler.removeCallbacks(ticker); super.onPause() }

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun gap(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(v: Int) = (resources.displayMetrics.density * v + 0.5f).toInt()
    private fun label(value: String, size: Int = 15, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    private fun button(name: String, primary: Boolean = false, click: () -> Unit): Button =
        Button(this).apply {
            text = name
            textSize = 13f
            isAllCaps = false
            setTextColor(white)
            backgroundTintList = ColorStateList.valueOf(if (primary) purple else cardColor)
            setOnClickListener { click() }
        }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(16), dp(17), dp(16))
        background = rounded(cardColor)
    }
    private fun addTitle(value: String, subtitle: String) {
        content.addView(label(value, 25, white, true))
        content.addView(gap(5))
        content.addView(label(subtitle, 13, muted))
        content.addView(gap(22))
    }
    private fun showTab(tab: String) {
        activeTab = tab
        root.removeAllViews()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(21), dp(20), dp(15))
        }
        top.addView(label("◈  SnapFlow", 27, purple, true))
        top.addView(label("Your media. Your downloads.", 12, muted))
        root.addView(top)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(20))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        when (tab) {
            "Home" -> home()
            "Downloads" -> downloads()
            "Quality" -> quality()
            "Player" -> player()
            else -> settings()
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(5), dp(6), dp(9))
            setBackgroundColor(cardColor)
        }
        listOf("Home" to "⌂", "Downloads" to "↓", "Player" to "▷", "Settings" to "⚙").forEach { (name, symbol) ->
            val item = TextView(this).apply {
                text = symbol + "\n" + name
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(if (tab == name) purple else muted)
                setTypeface(null, if (tab == name) Typeface.BOLD else Typeface.NORMAL)
                setOnClickListener { if (tab != name) showTab(name) }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        root.addView(nav)
    }

    private fun home() {
        addTitle("Download videos", "Fast, simple and available offline")
        val hero = card().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(100, 49, 190), Color.rgb(48, 29, 99))).apply {
                cornerRadius = dp(22).toFloat()
            }
        }
        hero.addView(label("↓   YOUR VIDEO LIBRARY", 13, Color.rgb(230, 214, 255), true))
        hero.addView(gap(10))
        hero.addView(label("Save videos.\nWatch anywhere.", 25, white, true))
        hero.addView(gap(9))
        hero.addView(label("Download authorized direct MP4 and WebM video links to your phone.", 13))
        content.addView(hero)
        content.addView(gap(24))
        content.addView(label("Paste video link", 18, white, true))
        content.addView(gap(10))
        urlField = EditText(this).apply {
            hint = "https://example.org/video.mp4"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(15), dp(15), dp(15), dp(15))
            background = rounded(cardColor, 13)
        }
        content.addView(urlField)
        content.addView(gap(9))
        val action = button("↓  Download video", true) { startDownload() }
        content.addView(action)
        content.addView(gap(13))
        val demo = button("Try a CC0 sample video") {
            urlField.setText("https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4")
            startDownload()
        }
        content.addView(demo)
        content.addView(gap(24))
        content.addView(label("Platforms", 18, white, true))
        content.addView(gap(8))
        val info = card()
        info.addView(label("YouTube     Instagram     Facebook", 15, white, true))
        info.addView(gap(9))
        info.addView(label("These websites have platform-specific restrictions. This first APK supports direct, permitted video files only, not social watch-page extraction.", 13, muted))
        content.addView(info)
        content.addView(gap(20))
        content.addView(label("Save location", 16, white, true))
        content.addView(label("Direct files: Downloads/SnapFlow • Website media: app library", 13, muted))
    }

    private fun startDownload() {
        if (checking) return
        val url = urlField.text.toString().trim()
        if (url.isBlank()) { toast("Paste a direct video link first."); return }
        val path = try { URI(url).path.orEmpty().lowercase() } catch (_: Exception) { "" }
        if (!path.endsWith(".mp4") && !path.endsWith(".webm")) {
            startSiteDownload(url)
            return
        }
        checking = true
        toast("Checking direct video URL...")
        Thread {
            try {
                val media = engine.prepare(url)
                runOnUiThread {
                    checking = false
                    pendingMedia = media
                    showTab("Quality")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    checking = false
                    toast(e.message ?: "Video could not be downloaded.")
                }
            }
        }.start()
    }

    private fun startSiteDownload(url: String) {
        if (siteBusy) { toast("A website download is already running"); return }
        siteBusy = true
        siteProgress = 0
        siteStatus = "Finding website video"
        showTab("Downloads")
        Thread {
            try {
                val video = siteEngine.download(url) { progress, message ->
                    runOnUiThread {
                        siteProgress = progress
                        siteStatus = message
                        if (activeTab == "Downloads") showTab("Downloads")
                    }
                }
                runOnUiThread {
                    siteBusy = false
                    siteStatus = "Saved: " + video.name
                    showTab("Player")
                    toast("Website video saved")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    siteBusy = false
                    siteStatus = "Website download failed: " + (e.message ?: "unknown error")
                    showTab("Downloads")
                }
            }
        }.start()
    }

    private fun quality() {
        val media = pendingMedia
        if (media == null) { showTab("Home"); return }
        addTitle("Download video", "Choose available quality")
        val preview = card()
        preview.addView(label("▣  Video source verified", 19, white, true))
        preview.addView(gap(10))
        preview.addView(label("File type: " + media.extension.uppercase(), 14, muted))
        preview.addView(label("Size: " +
            (if (media.size > 0) (media.size / 1024L / 1024L).toString() + " MB" else "Not provided by server"), 14, muted))
        content.addView(preview)
        content.addView(gap(16))
        content.addView(label("Available quality", 18, white, true))
        content.addView(gap(8))
        val original = card()
        original.addView(label("◉  Original source quality", 16, purple, true))
        original.addView(gap(7))
        original.addView(label("This URL provides one downloadable file. Extra resolutions are not invented.", 13, muted))
        content.addView(original)
        content.addView(gap(22))
        content.addView(button("↓  Start download", true) {
            try {
                val id = engine.enqueue(media)
                pendingMedia = null
                toast("Video queued: " + id)
                showTab("Downloads")
            } catch (e: Exception) { toast(e.message ?: "Could not start download") }
        })
        content.addView(button("←  Back to home") { showTab("Home") })
    }

    private fun downloads() {
        addTitle("My downloads", "Real transfer progress from Android")
        if (siteBusy || siteStatus.isNotBlank()) {
            val site = card()
            site.addView(label("Website media", 17, white, true))
            site.addView(gap(9))
            site.addView(label(siteStatus, 13, muted))
            if (siteBusy) {
                val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
                progress.max = 100
                progress.progress = siteProgress
                progress.progressTintList = ColorStateList.valueOf(purple)
                site.addView(progress)
            }
            content.addView(site)
            content.addView(gap(12))
        }
        val items = try { engine.history() } catch (e: Exception) {
            content.addView(label("Could not load downloads: " + e.message, 13, muted))
            return
        }
        if (items.isEmpty()) {
            val empty = card()
            empty.addView(label("↓  No downloads yet", 18, white, true))
            empty.addView(gap(8))
            empty.addView(label("Start with the CC0 sample video on Home to test real downloading.", 13, muted))
            content.addView(empty)
        }
        items.forEach { item ->
            val block = card()
            block.addView(label("▣  " + item.title, 15, white, true))
            block.addView(gap(7))
            val status = when (item.status) {
                DownloadManager.STATUS_SUCCESSFUL -> "✓ Download complete"
                DownloadManager.STATUS_RUNNING -> "↓ Downloading"
                DownloadManager.STATUS_PENDING -> "Queued"
                DownloadManager.STATUS_PAUSED -> "Paused: network or Wi-Fi restriction"
                DownloadManager.STATUS_FAILED -> "Failed: " + engine.failureText(item.reason)
                else -> "Unknown status"
            }
            block.addView(label(status, 13, if (item.status == DownloadManager.STATUS_FAILED) Color.rgb(255, 120, 130) else muted))
            block.addView(gap(10))
            val pct = if (item.totalBytes > 0) (item.receivedBytes * 100L / item.totalBytes).toInt().coerceIn(0, 100) else 0
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = if (item.status == DownloadManager.STATUS_SUCCESSFUL) 100 else pct
                progressTintList = ColorStateList.valueOf(purple)
                progressBackgroundTintList = ColorStateList.valueOf(bgColor)
            }
            block.addView(bar)
            block.addView(gap(5))
            block.addView(label((item.receivedBytes / 1024L) .toString() + " KB • " +
                (if (item.totalBytes > 0) pct.toString() + "%" else "size unknown"), 12, muted))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                actions.addView(button("▷ Play video") { openVideo(item.id) })
            } else if (item.status == DownloadManager.STATUS_RUNNING ||
                       item.status == DownloadManager.STATUS_PENDING ||
                       item.status == DownloadManager.STATUS_PAUSED) {
                actions.addView(button("× Cancel download") {
                    engine.cancel(item.id)
                    showTab("Downloads")
                })
            }
            block.addView(actions)
            content.addView(block)
            content.addView(gap(12))
        }
    }

    private fun openVideo(id: Long) {
        val uri = engine.open(id)
        if (uri == null) { toast("Video file is missing."); return }
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) { toast("No compatible video player found on phone.") }
    }
    private fun player() {
        addTitle("Video player", "Play downloaded videos offline")
        val siteVideos = siteEngine.savedVideos()
        if (siteVideos.isNotEmpty()) {
            content.addView(label("Saved website videos", 17, white, true))
            content.addView(gap(10))
            siteVideos.forEach { video ->
                content.addView(button("▷  " + video.name) { playLocalVideo(video.file) })
            }
            content.addView(gap(20))
        }
        val items = engine.history().filter { it.status == DownloadManager.STATUS_SUCCESSFUL }
        if (items.isEmpty() && siteVideos.isEmpty()) {
            val block = card()
            block.addView(label("▷  No saved videos", 19, white, true))
            block.addView(gap(8))
            block.addView(label("Download a video first. Then play it offline here.", 13, muted))
            content.addView(block)
        } else items.forEach {
            content.addView(button("▷  " + it.title) { openVideo(it.id) })
        }
    }
    private fun playLocalVideo(file: File) {
        root.removeAllViews()
        root.addView(button("←  Back to library") { showTab("Player") })
        val video = VideoView(this)
        val controls = MediaController(this)
        controls.setAnchorView(video)
        video.setMediaController(controls)
        video.setVideoPath(file.absolutePath)
        root.addView(video, LinearLayout.LayoutParams(-1, 0, 1f))
        video.setOnErrorListener { _, _, _ ->
            toast("Playback failed: unsupported media codec")
            true
        }
        video.setOnPreparedListener { video.start() }
    }

    private fun settings() {
        addTitle("Settings", "Control downloading and storage")
        val block = card()
        block.addView(label("Network", 18, white, true))
        val wifi = Switch(this).apply {
            text = "Wi-Fi downloads only"
            textSize = 15f
            setTextColor(white)
            isChecked = engine.wifiOnly()
            setOnCheckedChangeListener { _, checked -> engine.setWifiOnly(checked) }
        }
        block.addView(wifi)
        block.addView(gap(10))
        block.addView(label("Saved videos: Downloads/SnapFlow", 13, muted))
        block.addView(gap(12))
        block.addView(label("SnapFlow v0.4 • Direct and permitted website media", 13, muted))
        content.addView(block)
        content.addView(gap(15))
        val about = card()
        about.addView(label("Supported media", 17, white, true))
        about.addView(gap(8))
        about.addView(label("Save only media you own or may download. Platform permissions still apply. No DRM or login bypass.", 13, muted))
        content.addView(about)
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}

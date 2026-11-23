package com.snapflow.downloader

import android.content.Context
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local yt-dlp adapter. Only for media the user owns or is permitted to save.
 * No account cookies, protected content access, paywall bypass or DRM handling.
 */
class SiteMediaEngine(private val context: Context) {
    data class Video(val name: String, val file: File)
    private val initialized = AtomicBoolean(false)
    private val outputDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SnapFlow").apply {
            require(mkdirs() || isDirectory) { "Could not create media directory" }
        }
    }

    @Synchronized
    private fun ensureReady() {
        if (!initialized.get()) {
            YoutubeDL.getInstance().init(context.applicationContext)
            initialized.set(true)
        }
    }

    fun savedVideos(): List<Video> = outputDir.listFiles()
        .orEmpty().filter { it.isFile && it.length() > 1024 &&
            (it.extension.equals("mp4", true) || it.extension.equals("webm", true) ||
             it.extension.equals("mkv", true)) }
        .sortedByDescending { it.lastModified() }
        .map { Video(it.name, it) }

    fun download(rawUrl: String, onProgress: (Int, String) -> Unit): Video {
        val url = rawUrl.trim()
        val parsed = try { URI(url) } catch (_: Exception) { throw IllegalArgumentException("Invalid video page URL") }
        require(parsed.scheme.equals("https", true) && parsed.host != null &&
            parsed.userInfo == null) { "Only public HTTPS video pages are accepted" }
        require(parsed.port == -1 || parsed.port == 443) { "Only standard HTTPS ports are accepted" }
        ensureReady()
        val before = savedVideos().map { it.file.absolutePath }.toSet()
        val req = YoutubeDLRequest(url)
        req.addOption("--no-playlist")
        req.addOption("--age-limit", "0")
        req.addOption("--no-overwrites")
        req.addOption("--no-part")
        req.addOption("--restrict-filenames")
        req.addOption("--no-warnings")
        req.addOption("-f", "best[ext=mp4]/best[ext=webm]/best")
        req.addOption("-o", File(outputDir, "%(title).90B_%(id)s.%(ext)s").absolutePath)
        onProgress(0, "Looking for downloadable media")
        YoutubeDL.getInstance().execute(req, callback = { progress: Float, _: Long, line: String ->
            onProgress(progress.toInt().coerceIn(0, 99), line.takeLast(160))
        })
        val file = savedVideos().firstOrNull { it.file.absolutePath !in before }
            ?: throw IllegalStateException("Extractor finished but no valid video file was saved")
        onProgress(100, "Saved " + file.name)
        return file
    }
}

package com.snapflow.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.util.Locale

/** Authorized direct HTTPS MP4/WebM downloads. No protected website extraction. */
class DownloadEngine(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("snapflow_downloads", Context.MODE_PRIVATE)
    data class Prepared(val url: String, val mime: String, val extension: String, val size: Long)
    data class Transfer(val id: Long, val status: Int, val receivedBytes: Long, val totalBytes: Long, val reason: Int, val title: String)

    fun prepare(raw: String): Prepared {
        val url = raw.trim()
        val uri = try { URI(url) } catch (_: Exception) { throw IllegalArgumentException("Paste a valid video URL.") }
        require(uri.scheme.equals("https", true)) { "Only HTTPS links are supported." }
        require(uri.rawUserInfo == null && uri.rawFragment == null) { "Invalid video link." }
        val host = uri.host?.lowercase(Locale.ROOT)
            ?: throw IllegalArgumentException("Missing public video host.")
        require(isPublicHost(host)) { "Private/local video hosts are not supported." }
        require(uri.port == -1 || uri.port == 443) { "Only standard HTTPS is supported." }
        val inferred = when {
            uri.path.orEmpty().lowercase(Locale.ROOT).endsWith(".mp4") -> "mp4"
            uri.path.orEmpty().lowercase(Locale.ROOT).endsWith(".webm") -> "webm"
            else -> null
        }
        // Direct MP4/WebM URLs can be handed to Android DownloadManager directly.
        // Many real CDN servers reject HEAD even when GET works. The DownloadManager
        // performs the actual GET and exposes the real HTTP error if it fails.
        if (inferred != null) {
            return Prepared(url, if (inferred == "mp4") "video/mp4" else "video/webm",
                inferred, -1L)
        }
        // For extensionless signed links we check advertised media type.
        // Perform network checks only from a background thread.
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = false
            connectTimeout = 12000
            readTimeout = 12000
        }
        try {
            val response = connection.responseCode
            if (response in 300..399) throw IllegalArgumentException("Redirect detected. Paste the final direct video URL.")
            if (response == 401 || response == 403) throw IllegalArgumentException("Video access denied (HTTP " + response + ").")
            if (response == 404) throw IllegalArgumentException("Video not found (HTTP 404).")
            if (response !in 200..299 && response != 405 && response != 501)
                throw IllegalArgumentException("Video server returned HTTP " + response + ".")
            val type = connection.contentType.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
            val ext = when {
                type == "video/mp4" -> "mp4"
                type == "video/webm" -> "webm"
                (type.isBlank() || type == "application/octet-stream" ||
                    response == 405 || response == 501) && inferred != null -> inferred
                else -> throw IllegalArgumentException("Not a direct MP4/WebM file (type: " + type.ifBlank { "unknown" } + ").")
            }
            return Prepared(url, if (ext == "mp4") "video/mp4" else "video/webm",
                ext, connection.getHeaderFieldLong("Content-Length", -1L))
        } catch (e: java.io.IOException) {
            throw IllegalArgumentException("Video server unavailable: " + (e.localizedMessage ?: "network error"))
        } finally { connection.disconnect() }
    }

    fun enqueue(media: Prepared): Long {
        val fileName = "SnapDay_" + System.currentTimeMillis() + "." + media.extension
        val wifiOnly = prefs.getBoolean("wifi_only", false)
        val req = DownloadManager.Request(Uri.parse(media.url))
            .setTitle(fileName).setDescription("SnapDay video download").setMimeType(media.mime)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(!wifiOnly).setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SnapDay/" + fileName)
        val id = manager.enqueue(req)
        val saved = prefs.getString("ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        prefs.edit().putString("ids", (listOf(id) + saved).distinct().take(100).joinToString(",")).apply()
        return id
    }

    fun history(): List<Transfer> {
        val ids = prefs.getString("ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        return ids.mapNotNull { id ->
            manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                fun number(name: String) = cursor.getLong(cursor.getColumnIndexOrThrow(name))
                val idx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                Transfer(id, number(DownloadManager.COLUMN_STATUS).toInt(),
                    number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                    number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                    number(DownloadManager.COLUMN_REASON).toInt(),
                    if (idx >= 0) cursor.getString(idx) ?: "Video" else "Video")
            }
        }
    }

    fun failureText(reason: Int): String = when (reason) {
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network/server error"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server rejected this video link"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Phone storage is full"
        DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume video transfer"
        DownloadManager.ERROR_FILE_ERROR -> "Cannot save video file"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage unavailable"
        else -> if (reason in 400..599) "HTTP " + reason else "Download failed (code " + reason + ")"
    }
    fun setWifiOnly(enabled: Boolean) = prefs.edit().putBoolean("wifi_only", enabled).apply()
    fun wifiOnly(): Boolean = prefs.getBoolean("wifi_only", false)
    fun cancel(id: Long) = manager.remove(id)
    fun open(id: Long): Uri? = manager.getUriForDownloadedFile(id)

    private fun isPublicHost(host: String): Boolean {
        if (host == "localhost" || listOf(".local", ".internal", ".localhost", ".onion", ".test", ".invalid", ".example").any { host.endsWith(it) }) return false
        if (!host.contains('.') || host.startsWith('.') || host.endsWith('.')) return false
        return try {
            InetAddress.getAllByName(host).all { address ->
                !address.isAnyLocalAddress && !address.isLoopbackAddress &&
                !address.isSiteLocalAddress && !address.isLinkLocalAddress &&
                !address.isMulticastAddress
            }
        } catch (_: Exception) { false }
    }
}

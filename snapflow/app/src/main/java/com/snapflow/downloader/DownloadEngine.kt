package com.snapflow.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.net.URI
import java.util.Locale

/** Direct, HTTPS-hosted MP4/WebM only. Does not extract protected site streams. */
class DownloadEngine(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("snapflow_downloads", Context.MODE_PRIVATE)

    data class Transfer(
        val id: Long,
        val status: Int,
        val receivedBytes: Long,
        val totalBytes: Long,
        val reason: Int
    )

    fun enqueue(rawUrl: String): Long {
        val url = rawUrl.trim()
        val parsed = URI(url)
        require(parsed.scheme.equals("https", ignoreCase = true)) { "Only HTTPS links are supported." }
        require(parsed.userInfo == null && parsed.fragment == null) { "Invalid video URL." }
        val host = parsed.host?.lowercase(Locale.ROOT)
        require(!host.isNullOrBlank() && isAllowedPublicHost(host)) { "A public video host is required." }
        require(parsed.port == -1 || parsed.port == 443) { "Only the standard HTTPS port is supported." }

        val path = parsed.path.orEmpty().lowercase(Locale.ROOT)
        val ext = when {
            path.endsWith(".mp4") -> "mp4"
            path.endsWith(".webm") -> "webm"
            else -> throw IllegalArgumentException("Paste a direct .mp4 or .webm URL, not a webpage link.")
        }
        val fileName = "SnapFlow_${System.currentTimeMillis()}.$ext"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading video")
            .setMimeType(if (ext == "mp4") "video/mp4" else "video/webm")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SnapFlow/$fileName")
        val id = manager.enqueue(request)
        val old = prefs.getString("ids", "").orEmpty()
        prefs.edit().putString("ids", (listOf(id.toString()) + old.split(',').filter { it.isNotBlank() }).distinct().take(50).joinToString(",")).apply()
        return id
    }

    fun history(): List<Transfer> {
        val ids = prefs.getString("ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return emptyList()
        return ids.mapNotNull { id ->
            manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
                Transfer(
                    id,
                    number(DownloadManager.COLUMN_STATUS).toInt(),
                    number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                    number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                    number(DownloadManager.COLUMN_REASON).toInt()
                )
            }
        }
    }

    fun cancel(id: Long) = manager.remove(id)
    fun open(id: Long): Uri? = manager.getUriForDownloadedFile(id)

    companion object {
        internal fun isAllowedPublicHost(host: String): Boolean {
            if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
                host.endsWith(".internal") || host.endsWith(".test") ||
                host.endsWith(".example") || host.endsWith(".invalid") ||
                host.endsWith(".onion") || host.startsWith("127.") ||
                host == "0.0.0.0" || host == "::1" || host.contains(':')) return false
            // Reject IP literals entirely, including private, loopback and alternative numeric forms.
            if (host.all { it.isDigit() || it == '.' } || host.matches(Regex("[0-9a-fA-F:.]+"))) return false
            return host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
        }
    }
}

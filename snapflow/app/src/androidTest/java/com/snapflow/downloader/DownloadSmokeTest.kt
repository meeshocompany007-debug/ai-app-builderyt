package com.snapflow.downloader

import android.app.DownloadManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android DownloadManager network smoke test.
 * This test does not pass unless actual video bytes are saved and readable.
 */
@RunWith(AndroidJUnit4::class)
class DownloadSmokeTest {
    @Test fun bundledSiteEngineStartsOnAndroid() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        com.yausername.youtubedl_android.YoutubeDL.getInstance().init(ctx)
        assertTrue("Site engine initialization passed", true)
    }

    @Test fun downloadsCc0Mp4AndReadsVideoBytes() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = DownloadEngine(ctx)
        val url = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
        val media = engine.prepare(url)
        val id = engine.enqueue(media)
        val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            val endAt = System.currentTimeMillis() + 150_000L
            var latest: DownloadEngine.Transfer? = null
            while (System.currentTimeMillis() < endAt) {
                latest = engine.history().firstOrNull { it.id == id }
                if (latest?.status == DownloadManager.STATUS_SUCCESSFUL) break
                if (latest?.status == DownloadManager.STATUS_FAILED) {
                    fail("Real download failed: " + engine.failureText(latest.reason))
                }
                Thread.sleep(1_000L)
            }
            assertTrue("Timed out downloading real video: " + latest, latest?.status == DownloadManager.STATUS_SUCCESSFUL)
            val uri = engine.open(id) ?: throw AssertionError("No readable video URI returned")
            var totalRead = 0L
            val header = ByteArray(12)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val headerCount = input.read(header)
                assertTrue("Video header unreadable", headerCount >= 8)
                assertTrue("MP4 file signature missing", String(header, 4, 4, Charsets.US_ASCII) == "ftyp")
                totalRead += headerCount
                val block = ByteArray(16_384)
                while (true) {
                    val count = input.read(block)
                    if (count < 0) break
                    totalRead += count
                }
            } ?: fail("Could not open saved video bytes")
            assertTrue("Saved video too small: " + totalRead, totalRead > 10_000L)
        } finally {
            manager.remove(id)
        }
    }
}

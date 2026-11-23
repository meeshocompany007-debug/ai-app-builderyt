package com.snapflow.downloader

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var engine: DownloadEngine
    private lateinit var urlInput: EditText
    private lateinit var transfers: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val purple = Color.rgb(151, 65, 246)
    private val background = Color.rgb(10, 13, 24)
    private val panel = Color.rgb(26, 30, 47)
    private val poll = object : Runnable {
        override fun run() {
            renderTransfers()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = DownloadEngine(this)
        window.statusBarColor = background
        window.navigationBarColor = background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(20))
            setBackgroundColor(background)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("SnapFlow", 32, purple, true))
        root.addView(text("Real downloads • MP4 / WebM", 14, Color.LTGRAY))
        root.addView(space(24))
        root.addView(text("Paste a direct video link", 20, Color.WHITE, true))
        root.addView(space(12))

        urlInput = EditText(this).apply {
            hint = "https://website.com/video.mp4"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        root.addView(urlInput)
        root.addView(space(14))

        val download = Button(this).apply {
            text = "START DOWNLOAD"
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
            setOnClickListener {
                try {
                    val id = engine.enqueue(urlInput.text.toString())
                    Toast.makeText(this@MainActivity, "Download queued: $id", Toast.LENGTH_LONG).show()
                    renderTransfers()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.message ?: "Cannot download this link", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(download)

        root.addView(space(18))
        root.addView(text("Supported: direct HTTPS video files you have permission to download. Social platform webpages and protected media are not supported in this build.", 13, Color.LTGRAY))
        root.addView(space(26))
        root.addView(text("My Downloads", 22, Color.WHITE, true))
        root.addView(space(12))
        transfers = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(transfers)

        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { urlInput.setText(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(poll)
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    private fun renderTransfers() {
        transfers.removeAllViews()
        val items = try { engine.history() } catch (e: Exception) { emptyList() }
        if (items.isEmpty()) {
            transfers.addView(text("No downloads yet. Paste a direct video URL above.", 14, Color.LTGRAY))
            return
        }
        items.forEach { transfer ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(panel)
            }
            val status = when (transfer.status) {
                DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                DownloadManager.STATUS_RUNNING -> "Downloading"
                DownloadManager.STATUS_PENDING -> "Queued"
                DownloadManager.STATUS_PAUSED -> "Paused (reason ${transfer.reason})"
                DownloadManager.STATUS_FAILED -> "Failed (reason ${transfer.reason})"
                else -> "Unknown"
            }
            box.addView(text("Download #${transfer.id} • $status", 15, Color.WHITE, true))
            val progress = if (transfer.totalBytes > 0) "${(transfer.receivedBytes * 100 / transfer.totalBytes).coerceIn(0, 100)}%" else "size unknown"
            box.addView(text("${transfer.receivedBytes / 1024} KB received • $progress", 13, Color.LTGRAY))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (transfer.status == DownloadManager.STATUS_SUCCESSFUL) {
                actions.addView(Button(this).apply {
                    text = "PLAY"
                    setOnClickListener {
                        val uri = engine.open(transfer.id)
                        if (uri == null) {
                            Toast.makeText(this@MainActivity, "File unavailable", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "No compatible video player installed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })
            } else if (transfer.status != DownloadManager.STATUS_FAILED) {
                actions.addView(Button(this).apply {
                    text = "CANCEL"
                    setOnClickListener {
                        engine.cancel(transfer.id)
                        renderTransfers()
                    }
                })
            }
            box.addView(actions)
            transfers.addView(box)
            transfers.addView(space(10))
        }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

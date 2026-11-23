package com.snapflow.downloader

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.os.Build
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.net.URI

/** SnapDay storyboard navigation & genuine downloader. See DESIGN_REFERENCE_SNAPDAY.md. */
class SnapDayActivity: Activity() {
    lateinit var ui: DayKit
    lateinit var pref: SharedPreferences
    lateinit var direct: DownloadEngine
    lateinit var site: SiteMediaEngine
    lateinit var frame: FrameLayout
    lateinit var root: LinearLayout
    lateinit var body: LinearLayout
    var page = "splash"
    var category = "For You"
    var listFilter = "All"
    var historyFilter = "Watched"
    var selectedLink = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
    var selectedTitle = "Flower - CC0 nature sample"
    var directMedia: DownloadEngine.Prepared? = null
    var siteBusy = false
    var siteProgress = 0
    var siteStatus = ""
    var pageUrl = "https://www.google.com/"
    var webView: WebView? = null
    var audioUri: Uri? = null
    var audioPlayer: android.media.MediaPlayer? = null
    var urlInput: EditText? = null
    var floatOffsetX = 0
    var floatOffsetY = 0
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = DayKit(this)
        pref = getSharedPreferences("snapday_prefs",MODE_PRIVATE)
        direct = DownloadEngine(this)
        site = SiteMediaEngine(this)
        window.statusBarColor = Color.parseColor("#140D36")
        window.navigationBarColor = Color.parseColor("#170D37")
        val debugScreen=if((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)
            intent?.getStringExtra("snapday_screen") else null
        if(debugScreen in setOf("home","intro1","intro2","intro3","premium")) {
            // Debug-only route for automated visual QA; disabled in release builds.
            go(debugScreen!!)
        } else {
            go("splash")
            main.postDelayed({
                if(page == "splash") go(if(pref.getBoolean("introduced",false)) "home" else "intro1")
            }, 850)
        }
        readShare(intent)
    }
    private fun readShare(i:Intent?) {
        if(i?.action==Intent.ACTION_SEND) {
            val text=i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if(text.isNotBlank()) main.postDelayed({
                selectedLink=text
                go("home")
                urlInput?.setText(text)
            },1100)
        }
    }
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readShare(intent)
    }
    override fun onDestroy() {
        webView?.destroy()
        audioPlayer?.release()
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    @Deprecated("Activity legacy back callback")
    override fun onBackPressed() {
        when(page) {
            "home" -> super.onBackPressed()
            "browser" -> if(webView?.canGoBack()==true) webView?.goBack() else go("home")
            "intro2" -> go("intro1")
            "intro3" -> go("intro2")
            "email","signup" -> go("login")
            "premium" -> go("me")
            "audio" -> go("player")
            else -> go("home")
        }
    }
    fun go(name:String) {
        if(page=="browser" && name!="browser") {
            webView?.stopLoading()
            webView?.destroy()
            webView=null
        }
        page=name
        frame=FrameLayout(this).apply { setBackgroundColor(ui.white) }
        root=ui.col().apply {
            setBackgroundColor(if(name in listOf("splash","audio")) ui.navy else ui.white)
        }
        frame.addView(root, FrameLayout.LayoutParams(-1,-1))
        // Android 15+ enforces edge-to-edge for target SDK 35. Respect the system
        // status and navigation gesture bars so the header and CTA remain tappable.
        if(Build.VERSION.SDK_INT >= 35) {
            window.setDecorFitsSystemWindows(false)
            frame.setOnApplyWindowInsetsListener { _, insets ->
                val bars=insets.getInsets(WindowInsets.Type.systemBars())
                root.setPadding(0,bars.top,0,bars.bottom)
                insets
            }
        }
        setContentView(frame)
        if(name=="splash") { DayIntro(this).splash(); return }
        if(name.startsWith("intro") || name in listOf("login","email","signup","complete")) {
            DayIntro(this).display(name)
            return
        }
        if(name=="home") homeHeader() else header(name)
        if(name=="browser") {
            DayPages(this).browser()
        } else {
            val scroll=ScrollView(this).apply {
                isFillViewport=false
                setBackgroundColor(if(name=="audio")ui.navy else ui.white)
            }
            body=ui.col().apply { setPadding(ui.dp(17),ui.dp(12),ui.dp(17),ui.dp(24)) }
            scroll.addView(body)
            root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
            DayPages(this).display(name)
        }
        if(name in listOf("home","downloads","player","files","me")) navigation(name)
        if(name=="home" && pref.getBoolean("floating",true)) floating()
    }
    private fun homeHeader() {
        val r=ui.row().apply { setPadding(ui.dp(15),ui.dp(12),ui.dp(15),ui.dp(11)) }
        r.addView(ui.text("☰",22,ui.ink,true).apply {
            setPadding(0,0,ui.dp(14),0)
            setOnClickListener { go("me") }
        })
        r.addView(SnapDayLogo(this),LinearLayout.LayoutParams(ui.dp(32),ui.dp(32)))
        r.addView(ui.text(" SnapDay",22,ui.ink,true),LinearLayout.LayoutParams(0,-2,1f))
        r.addView(ui.text("♛",24,ui.violet,true).apply { setOnClickListener { go("premium") } })
        r.addView(ui.text("   ♧",21,ui.ink).apply { setOnClickListener { go("downloads") } })
        root.addView(r)
    }
    private fun header(name:String) {
        val dark=name=="audio"
        val titles=mapOf("video" to "Video Player","quality" to "Download Video","audio" to "Now Playing (MP3)",
            "downloads" to "Downloads","player" to "Video Player","files" to "Files","settings" to "Settings",
            "browser" to "Browser","history" to "History","me" to "About SnapDay","premium" to "Premium",
            "floatGuide" to "Floating Download Button")
        val r=ui.row().apply {
            setPadding(ui.dp(16),ui.dp(14),ui.dp(16),ui.dp(12))
            setBackgroundColor(if(dark)ui.navy else ui.white)
        }
        r.addView(ui.text("‹",30,if(dark)ui.white else ui.ink,true).apply {
            setPadding(0,0,ui.dp(15),0)
            setOnClickListener { onBackPressed() }
        })
        r.addView(ui.text(titles[name] ?: "SnapDay",19,if(dark)ui.white else ui.ink,true),
            LinearLayout.LayoutParams(0,-2,1f))
        root.addView(r)
        if(!dark) root.addView(ui.line())
    }
    private fun navigation(selected:String) {
        val r=ui.row().apply {
            setBackgroundColor(ui.white)
            setPadding(ui.dp(6),ui.dp(5),ui.dp(6),ui.dp(5))
            elevation=ui.dp(4).toFloat()
        }
        listOf(Triple("home","⌂","Home"),Triple("downloads","↓","Downloads"),
            Triple("player","▷","Player"),Triple("files","▣","Files"),Triple("me","♙","Me"))
            .forEach { item ->
                val cell=ui.col().apply { gravity=Gravity.CENTER }
                cell.addView(ui.text(item.second,22,if(item.first==selected)ui.violet else ui.muted,true,Gravity.CENTER))
                cell.addView(ui.text(item.third,11,if(item.first==selected)ui.violet else ui.ink,item.first==selected,Gravity.CENTER))
                r.addView(cell,LinearLayout.LayoutParams(0,ui.dp(55),1f))
                cell.setOnClickListener { go(item.first) }
            }
        root.addView(r)
    }
    private fun floating() {
        val btn=ui.text("↓",33,ui.white,true,Gravity.CENTER).apply {
            background=ui.gradient(ui.purple2,ui.violet,60)
            elevation=ui.dp(12).toFloat()
        }
        val p=FrameLayout.LayoutParams(ui.dp(64),ui.dp(64),Gravity.END or Gravity.BOTTOM).apply {
            marginEnd=ui.dp(20)+floatOffsetX;bottomMargin=ui.dp(83)+floatOffsetY
        }
        frame.addView(btn,p)
        var lastX=0f;var lastY=0f;var dragged=false
        btn.setOnTouchListener { _,e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {lastX=e.rawX;lastY=e.rawY;dragged=false;true}
                MotionEvent.ACTION_MOVE -> {
                    val dx=e.rawX-lastX;val dy=e.rawY-lastY
                    if(kotlin.math.abs(dx)>4 || kotlin.math.abs(dy)>4) dragged=true
                    p.marginEnd=(p.marginEnd-dx.toInt()).coerceIn(0,ui.dp(275))
                    p.bottomMargin=(p.bottomMargin-dy.toInt()).coerceIn(ui.dp(74),
                        (frame.height-ui.dp(80)).coerceAtLeast(ui.dp(74)))
                    btn.layoutParams=p
                    floatOffsetX=p.marginEnd-ui.dp(20);floatOffsetY=p.bottomMargin-ui.dp(83)
                    lastX=e.rawX;lastY=e.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {if(!dragged) floatingMenu();true}
                else -> true
            }
        }
    }
    private fun floatingMenu() {
        val options=arrayOf("Paste Link","Download Video","Download MP3","Open Browser","Floating guide")
        AlertDialog.Builder(this).setTitle("SnapDay")
            .setItems(options) { _,which ->
                when(which) {
                    0 -> paste()
                    1 -> download(urlInput?.text?.toString().orEmpty())
                    2 -> message("MP3 extraction requires authorized media and a supported conversion engine. You can open your own MP3 in Player.")
                    3 -> go("browser")
                    else -> go("floatGuide")
                }
            }.show()
    }
    fun paste() {
        val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text=clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if(text.isBlank()) {toast("Clipboard has no link");return}
        urlInput?.setText(text)
        download(text)
    }
    fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    fun message(s:String) {
        AlertDialog.Builder(this).setTitle("SnapDay").setMessage(s)
            .setPositiveButton("OK",null).show()
    }
    fun browserAt(url:String) { pageUrl=url;go("browser") }
    fun download(raw:String) {
        val url=raw.trim()
        if(url.isBlank()) {toast("Paste a video URL first");return}
        selectedLink=url
        val ext=try {URI(url).path.orEmpty().lowercase()}catch(_:Exception){""}
        if(ext.endsWith(".mp4") || ext.endsWith(".webm")) {
            toast("Checking direct video...")
            Thread {
                try {
                    val media=direct.prepare(url)
                    runOnUiThread { directMedia=media;go("quality") }
                } catch(e:Exception){ runOnUiThread {message(e.message ?: "Invalid media URL")} }
            }.start()
        } else {
            AlertDialog.Builder(this).setTitle("Website media")
                .setMessage("Only save media you own or are permitted to download. Platform restrictions apply. Try this link?")
                .setPositiveButton("Continue") { _,_ -> websiteDownload(url) }
                .setNegativeButton("Cancel",null).show()
        }
    }
    private fun websiteDownload(url:String) {
        if(siteBusy){toast("Another download is running");return}
        siteBusy=true;siteProgress=0;siteStatus="Checking available media"
        go("downloads")
        Thread {
            try {
                val saved=site.download(url) { progress,status ->
                    runOnUiThread {
                        siteProgress=progress
                        siteStatus=status
                    }
                }
                runOnUiThread {
                    siteBusy=false;siteProgress=100;siteStatus="Saved: "+saved.name
                    go("downloads")
                }
            } catch(e:Exception) {
                runOnUiThread {
                    siteBusy=false;siteStatus="Failed: "+(e.message ?: "Source unavailable")
                    go("downloads")
                }
            }
        }.start()
    }
    fun playVideo(uri:Uri) {
        val i=Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri,"video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(i)
            pref.edit().putString("lastWatched","Local video at "+System.currentTimeMillis()).apply()
        } catch(e:Exception){message("No compatible video player: "+(e.message ?:""))}
    }
    fun share(url:String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type="text/plain";putExtra(Intent.EXTRA_TEXT,url)
        },"Share video"))
    }
    fun picker(mime:String,code:Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type=mime;addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },code)
    }
    @Deprecated("Uses stable Android document picker.")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK)return
        val uri=data?.data ?:return
        try {contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        catch(_:Exception){}
        when(requestCode) {
            201 -> playVideo(uri)
            202 -> {
                audioPlayer?.release();audioPlayer=null;audioUri=uri
                go("audio")
            }
            else -> toast("File selected")
        }
    }
}

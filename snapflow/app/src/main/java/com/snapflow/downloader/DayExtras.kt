package com.snapflow.downloader

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Screens 12, 13, 15-19 and separate optional Premium upgrade. */
class DayExtras(private val a:SnapDayActivity) {
    private val u get()=a.ui
    private val b get()=a.body
    private fun gap(n:Int=12)=b.addView(u.space(n))
    private fun label(s:String,size:Int=15,bold:Boolean=false,color:Int=u.ink) {
        b.addView(u.text(s,size,color,bold))
    }
    private fun button(s:String,primary:Boolean=true,action:()->Unit) {
        b.addView(u.button(s,primary,action))
    }
    fun display(which:String) {
        when(which) {
            "player" -> player()
            "audio" -> audio()
            "files" -> files()
            "settings" -> settings()
            "history" -> history()
            "me" -> profile()
            "premium" -> premium()
            "floatGuide" -> floatGuide()
            else -> player()
        }
    }
    private fun player() {
        label("Your Offline Library",22,true)
        gap(10)
        val completed=a.direct.history().filter {it.status==DownloadManager.STATUS_SUCCESSFUL}
        val site=a.site.savedVideos()
        if(completed.isEmpty() && site.isEmpty()) {
            b.addView(u.poster("▷","No videos saved","Download a video to play offline",
                Color.parseColor("#1C2448"),Color.parseColor("#773AD0")),
                LinearLayout.LayoutParams(-1,u.dp(214)))
            gap(16)
        }
        completed.forEach { t ->
            b.addView(u.cardLabel(t.title,"Completed video","▶").apply {
                setOnClickListener { a.direct.open(t.id)?.let {a.playVideo(it)} }
            })
            gap(10)
        }
        site.forEach { item ->
            b.addView(u.cardLabel(item.name,"Website media • Saved video","▶").apply {
                setOnClickListener {a.playVideo(Uri.fromFile(item.file))}
            })
            gap(10)
        }
        gap(15)
        button("♫ Open MP3 Player",false) {a.go("audio")}
        gap(10)
        button("▷ Choose video from device",false) {a.picker("video/*",201)}
    }
    private fun audio() {
        label("NOW PLAYING (MP3)",14,true,u.white)
        gap(24)
        val album=FrameLayout(a).apply {
            background=u.gradient(Color.parseColor("#25103D"),Color.parseColor("#171124"),18)
        }
        val art=android.widget.ImageView(a).apply {
            setImageResource(R.drawable.snapday_mp3_art)
            scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
            background=u.shape(u.navy,19)
            clipToOutline=true
            contentDescription="MP3 player album art"
        }
        album.addView(art,FrameLayout.LayoutParams(u.dp(220),u.dp(220),Gravity.CENTER))
        b.addView(album,LinearLayout.LayoutParams(-1,u.dp(278)))
        gap(20)
        val name=a.audioUri?.lastPathSegment?.takeLast(40) ?: "Choose a song"
        b.addView(u.text(name,21,u.white,true,Gravity.CENTER))
        b.addView(u.text("SnapDay Music Player",13,Color.LTGRAY,false,Gravity.CENTER))
        gap(24)
        val seek=SeekBar(a).apply { max=100 }
        b.addView(seek)
        val current=a.audioPlayer
        if(current!=null && current.duration>0) {
            seek.progress=(current.currentPosition*100/current.duration).coerceIn(0,100)
            seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s:SeekBar?,p:Int,fromUser:Boolean) {
                    if(fromUser) a.audioPlayer?.let {
                        if(it.duration>0)it.seekTo(it.duration*p/100)
                    }
                }
                override fun onStartTrackingTouch(s:SeekBar?){}
                override fun onStopTrackingTouch(s:SeekBar?){}
            })
        }
        gap(10)
        val row=u.row()
        listOf("⤨","◀|","▶ / ❚❚","|▶","↻").forEachIndexed { i,symbol ->
            val view=u.text(symbol,18,u.white,true,Gravity.CENTER)
            row.addView(view,LinearLayout.LayoutParams(0,u.dp(65),1f))
            view.setOnClickListener {
                when(i) {
                    2 -> toggleAudio()
                    0,4 -> a.toast("Shuffle and repeat activate when a full playlist is created")
                    else -> a.toast("Use Choose Music to select another track")
                }
            }
        }
        b.addView(row)
        gap(24)
        button("♫ Choose MP3 on phone",false) {a.picker("audio/*",202)}
        gap(12)
        button("Playlist    Equalizer    Sleep Timer",false) {
            AlertDialog.Builder(a).setItems(arrayOf("Playlist","Equalizer","Sleep Timer")) { _,index ->
                when(index) {
                    0 -> a.picker("audio/*",202)
                    1 -> equalizer()
                    else -> sleepTimer()
                }
            }.show()
        }
    }
    private fun toggleAudio() {
        val uri=a.audioUri
        if(uri==null) {a.picker("audio/*",202);return}
        try {
            if(a.audioPlayer==null) {
                a.audioPlayer=android.media.MediaPlayer().apply {
                    setDataSource(a,uri)
                    prepare()
                }
            }
            val player=a.audioPlayer ?:return
            if(player.isPlaying)player.pause() else player.start()
            a.go("audio")
        } catch(e:Exception) {a.message("Playback failed: "+(e.message ?:"Unsupported audio"))}
    }
    private fun sleepTimer() {
        AlertDialog.Builder(a).setTitle("Sleep Timer")
            .setItems(arrayOf("15 minutes","30 minutes","60 minutes")) { _,which ->
                val minutes=listOf(15,30,60)[which]
                Handler(Looper.getMainLooper()).postDelayed({
                    a.audioPlayer?.let {if(it.isPlaying)it.pause()}
                },minutes*60_000L)
                a.toast("Audio sleep timer set to "+minutes+" minutes")
            }.show()
    }
    private fun equalizer() {
        val player=a.audioPlayer
        if(player==null) {a.message("Open and play an MP3 first to enable the Android audio equalizer.");return}
        try {
            val eq=android.media.audiofx.Equalizer(0,player.audioSessionId)
            eq.enabled=true
            val bands=eq.numberOfBands.toInt()
            val names=(0 until bands).map {eq.getCenterFreq(it.toShort())/1000}
            a.message("Android audio equalizer enabled with "+bands+" bands: "+names.joinToString(", ")+" Hz. Advanced presets will be added later.")
        } catch(e:Exception){a.message("Device equalizer not available: "+(e.message?:""))}
    }
    private fun files() {
        label("Files",23,true)
        gap(7)
        label("Saved media and device documents",13,false,u.muted)
        gap(18)
        val categories=listOf(
            Triple("Videos","▶",Color.parseColor("#F32969")),
            Triple("Audio (MP3)","♫",Color.parseColor("#9844DE")),
            Triple("Images","▧",Color.parseColor("#25BC6B")),
            Triple("Documents","▤",Color.parseColor("#EC9F14")))
        categories.chunked(2).forEach { group ->
            val row=u.row()
            group.forEachIndexed { i,item ->
                val tile=u.panel(u.lilac,16,17)
                tile.addView(u.icon(item.second,u.white,item.third,58))
                tile.addView(u.space(11))
                tile.addView(u.text(item.first,16,u.ink,true))
                tile.addView(u.text("Browse files",12,u.muted))
                row.addView(tile,LinearLayout.LayoutParams(0,u.dp(145),1f))
                tile.setOnClickListener {
                    when(item.first) {
                        "Videos" -> a.go("player")
                        "Audio (MP3)" -> a.go("audio")
                        "Images" -> a.picker("image/*",203)
                        else -> a.picker("application/*",203)
                    }
                }
                if(i==0)row.addView(View(a).apply {
                    layoutParams=LinearLayout.LayoutParams(u.dp(11),1)
                })
            }
            b.addView(row)
            gap(12)
        }
        gap(10)
        label("My Downloaded Videos",18,true)
        gap(10)
        a.site.savedVideos().forEach { video ->
            b.addView(u.cardLabel(video.name,"Size: "+video.file.length()/1024L+" KB","▶").apply {
                setOnClickListener {a.playVideo(Uri.fromFile(video.file))}
            })
            gap(10)
        }
        a.direct.history().filter {it.status==DownloadManager.STATUS_SUCCESSFUL}.forEach { t ->
            b.addView(u.cardLabel(t.title,"Saved in Downloads/SnapDay","↓").apply {
                setOnClickListener {a.direct.open(t.id)?.let {a.playVideo(it)}}
            })
            gap(10)
        }
        label("Android Downloads/SnapDay contains direct downloads. App-owned files appear above.",12,false,u.muted)
    }
    private fun settingsSwitch(title:String,subtitle:String,checked:Boolean,change:(Boolean)->Unit) {
        val c=u.panel()
        val row=u.row()
        val titles=u.col()
        titles.addView(u.text(title,15,u.ink,true))
        titles.addView(u.text(subtitle,12,u.muted))
        row.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(Switch(a).apply {
            isChecked=checked
            setOnCheckedChangeListener { _,v ->change(v) }
        })
        c.addView(row)
        b.addView(c)
        gap(10)
    }
    private fun settings() {
        label("Download Settings",18,true)
        gap(9)
        b.addView(u.cardLabel("Download Quality","Use verified available source formats","HD").apply {
            setOnClickListener {
                a.message("SnapDay only offers resolutions supplied by a verified source. Unavailable 4K or 1080p options will not be displayed.")
            }
        })
        gap(10)
        b.addView(u.cardLabel("Download Path","Downloads/SnapDay • app-owned website media","▤").apply {
            setOnClickListener {a.message("Android direct downloads: Downloads/SnapDay. Website-media files: SnapDay private media storage.")}
        })
        gap(10)
        b.addView(u.cardLabel("Player Settings","Video and MP3 playback","▷").apply {
            setOnClickListener {a.go("player")}
        })
        gap(19)
        label("Advanced Tools",18,true)
        gap(11)
        settingsSwitch("Floating Download Button","Move anywhere inside Home",a.pref.getBoolean("floating",true)) {
            a.pref.edit().putBoolean("floating",it).apply()
        }
        settingsSwitch("Auto Detect Link","Clipboard read only when you select Paste",a.pref.getBoolean("detect",true)) {
            a.pref.edit().putBoolean("detect",it).apply()
        }
        settingsSwitch("Wi-Fi Downloads Only","Restrict future direct downloads",a.direct.wifiOnly()) {
            a.direct.setWifiOnly(it)
        }
        b.addView(u.cardLabel("Notifications","Managed by Android notification settings","♧").apply {
            setOnClickListener {
                try {
                    startNotificationSettings()
                }catch(e:Exception) {a.message("Open Android Settings to configure notifications.")}
            }
        })
        gap(10)
        b.addView(u.cardLabel("Storage","Open media and file manager","▣").apply {
            setOnClickListener {a.go("files")}
        })
    }
    private fun startNotificationSettings() {
        a.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.packageName)
        })
    }
    fun browser() {
        val bar=u.row().apply {setPadding(u.dp(11),u.dp(7),u.dp(11),u.dp(7))}
        val address=EditText(a).apply {
            hint="Search or enter URL"
            textSize=13f
            setSingleLine(true)
            setText(a.pageUrl)
            background=u.shape(u.lilac,13,u.line)
            setPadding(u.dp(9),u.dp(9),u.dp(9),u.dp(9))
        }
        bar.addView(address,LinearLayout.LayoutParams(0,-2,1f))
        bar.addView(u.button("Go",false) {
            val s=address.text.toString().trim()
            a.pageUrl=if(s.startsWith("https://"))s
                else if(s.startsWith("http://"))s.replaceFirst("http://","https://")
                else "https://www.google.com/search?q="+Uri.encode(s)
            a.webView?.loadUrl(a.pageUrl)
        })
        a.root.addView(bar)
        val sources=u.row()
        listOf("YouTube" to "https://www.youtube.com","Instagram" to "https://www.instagram.com",
            "Facebook" to "https://m.facebook.com","TikTok" to "https://www.tiktok.com")
            .forEach { site ->
                sources.addView(u.pill(site.first,false) {
                    a.pageUrl=site.second
                    a.webView?.loadUrl(a.pageUrl)
                },LinearLayout.LayoutParams(0,-2,1f))
            }
        a.root.addView(sources)
        val browser=WebView(a).apply {
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            webViewClient=object:WebViewClient() {
                override fun onPageFinished(view:WebView?,url:String?) {
                    super.onPageFinished(view,url)
                    a.pageUrl=url ?: a.pageUrl
                    address.setText(a.pageUrl)
                }
            }
            loadUrl(a.pageUrl)
        }
        a.webView=browser
        a.root.addView(browser,LinearLayout.LayoutParams(-1,0,1f))
        val tools=u.row()
        listOf("←","→","⌂","↓ Link","⋮").forEachIndexed { i,symbol ->
            val item=u.text(symbol,15,u.violet,true,Gravity.CENTER)
            tools.addView(item,LinearLayout.LayoutParams(0,u.dp(45),1f))
            item.setOnClickListener {
                when(i) {
                    0 -> browser.goBack()
                    1 -> browser.goForward()
                    2 -> browser.loadUrl("https://www.google.com/")
                    3 -> a.download(a.pageUrl)
                    else -> a.message("Only download authorized content and respect website restrictions.")
                }
            }
        }
        a.root.addView(tools)
    }
    private fun history() {
        val tabs=u.row()
        listOf("Watched","Downloaded").forEach {
            tabs.addView(u.pill(it,it==a.historyFilter) {
                a.historyFilter=it;a.go("history")
            },LinearLayout.LayoutParams(0,-2,1f))
        }
        b.addView(tabs)
        gap(16)
        if(a.historyFilter=="Watched") {
            val recent=a.pref.getString("lastWatched","").orEmpty()
            if(recent.isEmpty())label("No watched videos yet",15,false,u.muted)
            else b.addView(u.cardLabel(recent,"Watched on this device","▷"))
        } else {
            val items=a.direct.history()
            if(items.isEmpty() && a.site.savedVideos().isEmpty())label("No download history",15,false,u.muted)
            items.forEach {
                b.addView(u.cardLabel(it.title,"Download ID "+it.id,"↓"))
                gap(9)
            }
            a.site.savedVideos().forEach {
                b.addView(u.cardLabel(it.name,"Website media saved","▷"));gap(9)
            }
        }
    }
    private fun profile() {
        val box=u.panel(u.white,25,22)
        box.gravity=Gravity.CENTER_HORIZONTAL
        box.addView(SnapDayLogo(a),LinearLayout.LayoutParams(u.dp(70),u.dp(70)))
        box.addView(u.space(12))
        box.addView(u.text("SnapDay",26,u.ink,true,Gravity.CENTER))
        box.addView(u.text("Video Downloader & Player • v0.5",13,u.muted,false,Gravity.CENTER))
        b.addView(box)
        gap(14)
        button("♛   Premium  ₹99/month") {a.go("premium")}
        gap(13)
        listOf("Login / Sign In (Optional)","History","Settings","Rate This App",
            "Share App","Feedback","Privacy Policy","Terms of Service").forEach { name ->
            b.addView(u.cardLabel(name,"›","◈").apply {
                setOnClickListener {
                    when(name) {
                        "Login / Sign In (Optional)" -> a.go("login")
                        "History" -> a.go("history")
                        "Settings" -> a.go("settings")
                        "Rate This App" -> a.message("The app is not published in an app store yet.")
                        "Share App" -> a.share("SnapDay - Video Downloader & Player")
                        "Feedback" -> {
                            try {
                                a.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                    data=Uri.parse("mailto:?subject=SnapDay%20Feedback")
                                })
                            }catch(e:Exception){a.message("No email app installed.")}
                        }
                        else -> a.message(name+" will be published for the production app. No legal policy content is invented.")
                    }
                }
            })
            gap(9)
        }
    }
    private fun premium() {
        val box=u.panel(u.lilac,24,24)
        box.gravity=Gravity.CENTER_HORIZONTAL
        box.addView(u.text("♛",66,u.violet,true,Gravity.CENTER))
        box.addView(u.text("SnapDay Premium",28,u.ink,true,Gravity.CENTER))
        box.addView(u.text("₹99 / month",23,u.violet,true,Gravity.CENTER))
        box.addView(u.space(20))
        listOf("✓  Ad-free experience (planned)","✓  Faster supported transfers (planned)",
            "✓  More media tools (planned)","✓  Support developers").forEach {
            box.addView(u.text(it,15,u.ink))
            box.addView(u.space(11))
        }
        b.addView(box)
        gap(19)
        button("Upgrade to Premium") {
            a.message("₹99/month is a preview. Payment cannot be taken until Google Play Billing and purchase verification are configured.")
        }
        gap(10)
        button("Continue Free",false){a.go("home")}
        gap(15)
        label("Core playback, media browsing and permitted direct downloads do not require Premium.",13,false,u.muted)
    }
    private fun floatGuide() {
        label("Drag & Move Anywhere",24,true)
        gap(13)
        b.addView(u.poster("↓","Floating Download Button","Move within the Home screen",
            Color.parseColor("#963BE8"),Color.parseColor("#371A79")),
            LinearLayout.LayoutParams(-1,u.dp(250)))
        gap(17)
        label("Tap the purple floating button to open Paste Link, Download Video, Download MP3 and Open Browser.",14,false,u.muted)
        gap(18)
        button("Back to Home") {a.go("home")}
    }
}

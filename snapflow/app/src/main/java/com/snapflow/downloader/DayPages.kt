package com.snapflow.downloader

import android.app.DownloadManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.VideoView
import android.widget.MediaController
import android.widget.HorizontalScrollView

/** Screens 9, 10, 11 and 14 backed by the download services, plus routing. */
class DayPages(private val a:SnapDayActivity) {
    private val u get()=a.ui
    private val b get()=a.body
    private fun space(n:Int=12)=b.addView(u.space(n))
    private fun text(s:String,size:Int=15,bold:Boolean=false,color:Int=u.ink) {
        b.addView(u.text(s,size,color,bold))
    }
    private fun button(s:String,primary:Boolean=true,action:()->Unit) {
        b.addView(u.button(s,primary,action))
    }
    fun display(which:String) {
        when(which) {
            "home" -> home()
            "video" -> video()
            "quality" -> quality()
            "downloads" -> downloads()
            else -> DayExtras(a).display(which)
        }
    }
    private fun chips(values:List<String>,active:String,onPick:(String)->Unit) {
        val scroller=HorizontalScrollView(a).apply {isHorizontalScrollBarEnabled=false}
        val row=u.row()
        values.forEach { value ->
            row.addView(u.pill(value,value==active){onPick(value)})
            row.addView(View(a).apply { layoutParams=LinearLayout.LayoutParams(u.dp(7),1) })
        }
        scroller.addView(row)
        b.addView(scroller)
    }
    private fun shortcut(symbol:String,title:String,background:Int,url:String):LinearLayout {
        val col=u.col().apply {gravity=Gravity.CENTER}
        col.addView(u.icon(symbol,u.white,background,43))
        col.addView(u.text(title,10,u.ink,true,Gravity.CENTER))
        col.setOnClickListener { a.browserAt(url) }
        return col
    }
    private fun mediaRow(title:String,subtitle:String,symbol:String,c1:Int,c2:Int,artwork:Int=0,click:()->Unit) {
        val row=u.row()
        val poster:View=if(artwork!=0) android.widget.ImageView(a).apply {
            setImageResource(artwork)
            scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
            background=u.shape(u.lilac,13)
            clipToOutline=true
        } else u.poster(symbol,"","",c1,c2)
        row.addView(poster,LinearLayout.LayoutParams(u.dp(123),u.dp(83)))
        val labels=u.col().apply {setPadding(u.dp(12),0,0,0)}
        labels.addView(u.text(title,14,u.ink,true))
        labels.addView(u.space(6))
        labels.addView(u.text(subtitle,12,u.muted))
        row.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
        b.addView(row)
        row.setOnClickListener { click() }
    }
    private fun home() {
        val search=u.row()
        val input=EditText(a).apply {
            hint="⌕  Search or paste link (YouTube, Insta...)"
            textSize=13f
            setSingleLine(true)
            setPadding(u.dp(12),u.dp(10),u.dp(12),u.dp(10))
            background=u.shape(u.white,14,u.line)
            setOnEditorActionListener { _,_,_ ->
                a.download(text.toString())
                true
            }
        }
        a.urlInput=input
        search.addView(input,LinearLayout.LayoutParams(0,-2,1f))
        val paste=u.icon("⇧",u.violet,u.lilac,42).apply {
            setOnClickListener {a.paste()}
        }
        search.addView(paste)
        b.addView(search)
        space(14)
        val sources=u.row()
        val entries=listOf(
            shortcut("▶","YouTube",Color.parseColor("#F41C2E"),"https://www.youtube.com"),
            shortcut("◎","Instagram",Color.parseColor("#D83B9B"),"https://www.instagram.com"),
            shortcut("f","Facebook",Color.parseColor("#1873E8"),"https://m.facebook.com"),
            shortcut("♪","TikTok",Color.BLACK,"https://www.tiktok.com"),
            shortcut("••","More",u.violet,"https://www.google.com"))
        entries.forEach {sources.addView(it,LinearLayout.LayoutParams(0,-2,1f))}
        b.addView(sources)
        space(16)
        chips(listOf("For You","Trending","Satsang","Music","Shorts"),a.category) {
            a.category=it;a.go("home")
        }
        space(14)
        val satsang=android.widget.ImageView(a).apply {
            setImageResource(R.drawable.snapday_satsang)
            scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
            background=u.shape(u.lilac,16)
            clipToOutline=true
            elevation=u.dp(3).toFloat()
            contentDescription="User-approved Satsang thumbnail illustration"
        }
        b.addView(satsang,LinearLayout.LayoutParams(-1,u.dp(194)))
        satsang.setOnClickListener {a.browserAt("https://www.youtube.com/results?search_query=hindi+satsang")}
        space(9)
        text("जीवन में सफलता का सच्चा मंत्र | Satsang",15,true)
        text("Hindi Satsang • Explore source videos",12,false,u.muted)
        space(21)
        text(if(a.category=="Music")"Popular Music ♫" else "Popular Videos 🔥",20,true)
        space(12)
        mediaRow("Hindi songs and music videos","Browse the original music website","♫",
            Color.parseColor("#67325A"),Color.parseColor("#C8558C"),
            R.drawable.snapday_romance) {
            a.browserAt("https://www.youtube.com/results?search_query=hindi+music+videos")
        }
        space(12)
        mediaRow("Flower — CC0 Sample Video","A real, freely licensed test video",
            "🌸",Color.parseColor("#A038A2"),Color.parseColor("#EF88A5")) {
            a.selectedLink="https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
            a.selectedTitle="Flower — CC0 sample video"
            a.go("video")
        }
        space(13)
        mediaRow("Listen to Your Music","Local MP3 and music player",
            "🎧",Color.parseColor("#191737"),Color.parseColor("#863AA6")) {a.go("audio")}
        space(13)
        mediaRow("Explore Trending Videos","Browse videos from supported websites",
            "▶",Color.parseColor("#265886"),Color.parseColor("#6927B8")) {a.go("browser")}
        space(95)
    }
    private fun video() {
        text(a.selectedTitle,19,true)
        space(13)
        val mediaUrl=a.selectedLink
        val isFile=try {
            val path=Uri.parse(mediaUrl).path.orEmpty().lowercase()
            path.endsWith(".mp4") || path.endsWith(".webm")
        }catch(_:Exception){false}
        if(isFile) {
            val player=VideoView(a)
            val controls=MediaController(a)
            controls.setAnchorView(player)
            player.setMediaController(controls)
            player.setVideoURI(Uri.parse(mediaUrl))
            player.setOnErrorListener { _,_,_ ->
                a.toast("Unable to stream this URL. Try an authorized direct download.")
                true
            }
            b.addView(player,LinearLayout.LayoutParams(-1,u.dp(220)))
            player.setOnPreparedListener {player.start()}
        } else {
            val artwork=android.widget.ImageView(a).apply {
                setImageResource(R.drawable.snapday_romance)
                scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
                background=u.shape(u.lilac,16)
                clipToOutline=true
                setOnClickListener { a.browserAt(mediaUrl) }
            }
            b.addView(artwork,LinearLayout.LayoutParams(-1,u.dp(220)))
        }
        space(14)
        text("Saved media • Source controls",13,false,u.muted)
        space(16)
        val controls=u.row()
        listOf("♡ Like","↗ Share","↓ Download","▣ Save").forEachIndexed { index,title ->
            val btn=u.text(title,12,u.violet,true,Gravity.CENTER)
            controls.addView(btn,LinearLayout.LayoutParams(0,u.dp(44),1f))
            btn.setOnClickListener {
                when(index) {
                    0 -> a.toast("Favourite on this device")
                    1 -> a.share(a.selectedLink)
                    2 -> a.download(a.selectedLink)
                    else -> a.message("Save the source file using Download to keep it offline.")
                }
            }
        }
        b.addView(controls)
        space(24)
        text("Recommended Videos",19,true)
        space(13)
        mediaRow("Hindi Satsang","Explore video sources","🪷",
            Color.parseColor("#A84C16"),Color.parseColor("#F9B322")) {
            a.browserAt("https://www.youtube.com/results?search_query=hindi+satsang")
        }
        space(12)
        mediaRow("Your Downloads","Play local video files","▷",
            Color.parseColor("#342279"),Color.parseColor("#9241D9")) {a.go("downloads")}
    }
    private fun quality() {
        text("Download Video",23,true)
        space(11)
        b.addView(u.cardLabel("Direct video detected",a.selectedTitle,"▶"))
        space(18)
        chips(listOf("Video","Audio (MP3)","Subtitles"),"Video") {
            if(it!="Video")a.message(it+" is only offered when the source actually provides it. No fabricated files or resolutions.")
        }
        space(21)
        text("Available Quality",19,true)
        space(9)
        val current=a.directMedia
        if(current==null) {
            text("No direct media selected. Paste a valid link on Home.",14,false,u.muted)
        } else {
            val size=if(current.size>0)(current.size/1048576L).toString()+" MB" else "Size unknown"
            b.addView(u.cardLabel("◉  Original source quality",current.extension.uppercase()+" • "+size,
                "HD",Color.parseColor("#7E36DD")))
            space(16)
            text("4K, 2K, 1080p, 720p and lower qualities appear only when the media actually supplies them.",13,false,u.muted)
            space(25)
            button("↓  Download") {
                try {
                    a.direct.enqueue(current)
                    a.go("downloads")
                    a.toast("Video queued. Check progress below.")
                } catch(e:Exception){a.message(e.message ?: "Download could not start")}
            }
        }
    }
    private fun downloads() {
        chips(listOf("All","Video","Audio","In Progress"),a.listFilter) {
            a.listFilter=it;a.go("downloads")
        }
        space(17)
        if(a.siteBusy || a.siteStatus.isNotBlank()) {
            val container=u.panel(u.lilac)
            container.addView(u.text("Website download",16,u.ink,true))
            container.addView(u.space(7))
            container.addView(u.text(a.siteStatus,12,u.muted))
            if(a.siteBusy) {
                val bar=ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal)
                bar.max=100;bar.progress=a.siteProgress
                container.addView(bar)
            }
            b.addView(container)
            space(13)
        }
        val transfers=try {a.direct.history()}catch(_:Exception){emptyList()}
        val shown=transfers.filter {
            when(a.listFilter) {
                "Audio" -> false
                "In Progress" -> it.status==DownloadManager.STATUS_PENDING ||
                        it.status==DownloadManager.STATUS_RUNNING ||
                        it.status==DownloadManager.STATUS_PAUSED
                else -> true
            }
        }
        shown.forEach { item ->
            val state=when(item.status) {
                DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                DownloadManager.STATUS_RUNNING -> "Downloading"
                DownloadManager.STATUS_PENDING -> "Queued"
                DownloadManager.STATUS_PAUSED -> "Paused"
                DownloadManager.STATUS_FAILED -> "Failed: "+a.direct.failureText(item.reason)
                else -> "Unknown"
            }
            val card=u.cardLabel(item.title,state,"↓")
            val p=ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal)
            p.max=100
            p.progress=if(item.status==DownloadManager.STATUS_SUCCESSFUL)100
                else if(item.totalBytes>0)(item.receivedBytes*100L/item.totalBytes).toInt() else 0
            p.progressTintList=ColorStateList.valueOf(u.violet)
            card.addView(u.space(8));card.addView(p)
            card.addView(u.text((item.receivedBytes/1024L).toString()+" KB saved",12,u.muted))
            if(item.status==DownloadManager.STATUS_SUCCESSFUL) {
                card.addView(u.button("▷ Play",false) {
                    val uri=a.direct.open(item.id)
                    if(uri!=null)a.playVideo(uri) else a.toast("Video file unavailable")
                })
            }
            if(item.status in listOf(DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PENDING,DownloadManager.STATUS_PAUSED)) {
                card.addView(u.button("Cancel",false) {
                    a.direct.cancel(item.id)
                    a.go("downloads")
                })
            }
            b.addView(card)
            space(10)
        }
        if(a.listFilter!="In Progress" && a.listFilter!="Audio") {
            a.site.savedVideos().forEach { video ->
                b.addView(u.cardLabel(video.name,"Saved in app video library","▷").apply {
                    setOnClickListener { a.playVideo(Uri.fromFile(video.file)) }
                })
                space(9)
            }
        }
        if(transfers.isEmpty() && a.site.savedVideos().isEmpty() && !a.siteBusy) {
            text("No downloads yet",18,true)
            space(8)
            text("Tap Flower CC0 sample video on Home to test real downloads.",13,false,u.muted)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if(a.page=="downloads")a.go("downloads")
        }, 2500)
    }
    fun browser() { DayExtras(a).browser() }
}

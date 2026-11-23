package com.snapflow.downloader

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Shared design system for all 19 approved SnapDay screens. */
class DayKit(val a: Activity) {
    val violet = Color.parseColor("#7923E8")
    val purple2 = Color.parseColor("#A249F4")
    val pink = Color.parseColor("#F01481")
    val ink = Color.parseColor("#101126")
    val navy = Color.parseColor("#151127")
    val muted = Color.parseColor("#6A6880")
    val lilac = Color.parseColor("#F5F0FF")
    val white = Color.WHITE
    val line = Color.parseColor("#E8E1F8")
    fun dp(n: Int) = (a.resources.displayMetrics.density*n+0.5f).toInt()

    fun shape(color: Int = white, radius: Int = 17, border: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (border != 0) setStroke(dp(1), border)
        }
    fun gradient(start: Int = pink, end: Int = violet, radius: Int = 17): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
        }
    fun text(value: String, size: Int = 15, color: Int = ink, bold: Boolean = false,
             gravity: Int = Gravity.START): TextView = TextView(a).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        setGravity(gravity)
        includeFontPadding = true
        if(bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setLineSpacing(dp(1).toFloat(),1f)
    }
    fun col(padding: Int = 0): LinearLayout = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL
        if(padding>0) setPadding(dp(padding),dp(padding),dp(padding),dp(padding))
    }
    fun row(): LinearLayout = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    fun space(dp: Int): View = View(a).apply {
        layoutParams = LinearLayout.LayoutParams(1, this@DayKit.dp(dp))
    }
    fun panel(color: Int = white, padding: Int = 14, radius: Int = 18, stroke: Int = 0): LinearLayout =
        col(padding).apply {
            background = shape(color,radius,stroke)
            elevation = this@DayKit.dp(2).toFloat()
        }
    fun button(title: String, primary: Boolean = true, action: () -> Unit): TextView =
        text(title,15,if(primary) white else violet,true,Gravity.CENTER).apply {
            minHeight = dp(48)
            setPadding(dp(15),dp(12),dp(15),dp(12))
            background = if(primary) gradient(violet,Color.parseColor("#6B0EDC"),13)
                else shape(lilac,13,line)
            elevation = dp(2).toFloat()
            isClickable = true
            setOnClickListener { action() }
        }
    fun pill(title: String, active: Boolean, action: () -> Unit): TextView =
        text(title,12,if(active) white else ink,active,Gravity.CENTER).apply {
            setPadding(dp(13),dp(9),dp(13),dp(9))
            background = if(active) gradient(violet,purple2,50) else shape(white,50,line)
            isClickable = true
            setOnClickListener { action() }
        }
    fun icon(symbol: String, tint: Int = violet, color: Int = lilac, size: Int = 46): TextView =
        text(symbol,22,tint,true,Gravity.CENTER).apply {
            background = shape(color,13)
            layoutParams = LinearLayout.LayoutParams(dp(size),dp(size))
        }
    fun line(): View = View(a).apply {
        setBackgroundColor(this@DayKit.line)
        layoutParams = LinearLayout.LayoutParams(-1,dp(1))
    }
    fun cardLabel(title: String, subtitle: String, symbol: String, accent: Int = violet): LinearLayout {
        val wrapper = panel()
        val r = row()
        val badge = icon(symbol,white,accent,50)
        r.addView(badge)
        val labels = col().apply { setPadding(dp(12),0,0,0) }
        labels.addView(text(title,16,ink,true))
        labels.addView(text(subtitle,12,muted))
        r.addView(labels, LinearLayout.LayoutParams(0,-2,1f))
        wrapper.addView(r)
        return wrapper
    }
    fun poster(symbol:String, title:String, subtitle:String, color1:Int, color2:Int):FrameLayout {
        val frame = FrameLayout(a).apply {
            background = gradient(color1,color2,17)
            elevation = dp(2).toFloat()
            minimumHeight = dp(160)
        }
        val big = text(symbol,67,white,false,Gravity.CENTER).apply {
            alpha = 0.75f
        }
        frame.addView(big,FrameLayout.LayoutParams(-1,-1))
        val foot = col(7).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT,Color.parseColor("#A0000017")))
        }
        foot.addView(text(title,19,white,true))
        foot.addView(text(subtitle,12,white))
        frame.addView(foot,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))
        return frame
    }
}

/** Canvas-based original logo; no remote or copyrighted brand assets required. */
class SnapDayLogo(a: Activity): View(a) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val desired = (resources.displayMetrics.density*100).toInt()
        setMeasuredDimension(resolveSize(desired,widthMeasureSpec),resolveSize(desired,heightMeasureSpec))
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val s = width.coerceAtMost(height).toFloat()
        val left = (width-s)/2f; val top = (height-s)/2f
        paint.shader = LinearGradient(left,top,left+s,top+s,
            Color.parseColor("#FF4884"),Color.parseColor("#7B1AE6"),Shader.TileMode.CLAMP)
        c.drawRoundRect(left,top,left+s,top+s,s*.21f,s*.21f,paint)
        paint.shader = null; paint.color=Color.WHITE
        val triangle=Path().apply {
            moveTo(left+s*.34f,top+s*.22f)
            lineTo(left+s*.73f,top+s*.47f)
            quadTo(left+s*.79f,top+s*.51f,left+s*.73f,top+s*.56f)
            lineTo(left+s*.34f,top+s*.8f)
            quadTo(left+s*.27f,top+s*.83f,left+s*.27f,top+s*.75f)
            lineTo(left+s*.27f,top+s*.3f)
            close()
        }
        c.drawPath(triangle,paint)
        paint.color=Color.parseColor("#7B1AE6")
        c.drawCircle(left+s*.78f,top+s*.77f,s*.17f,paint)
        paint.color=Color.WHITE;paint.strokeWidth=s*.055f;paint.strokeCap=Paint.Cap.ROUND
        c.drawLine(left+s*.78f,top+s*.68f,left+s*.78f,top+s*.82f,paint)
        c.drawLine(left+s*.72f,top+s*.77f,left+s*.78f,top+s*.83f,paint)
        c.drawLine(left+s*.84f,top+s*.77f,left+s*.78f,top+s*.83f,paint)
    }
}

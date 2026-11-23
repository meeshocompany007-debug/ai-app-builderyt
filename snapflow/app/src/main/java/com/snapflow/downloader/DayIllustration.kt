package com.snapflow.downloader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

/**
 * Resolution-independent onboarding artwork (fully vector, no raster screenshots or
 * text baked into illustrations). Scales sharply on any Android screen density.
 */
class DayIllustration(context: Context, private val step: Int): View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private fun color(hex: String) = Color.parseColor(hex)
    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, fill: String) {
        p.shader=null;p.style=Paint.Style.FILL;p.color=color(fill)
        c.drawOval(RectF(x-rx,y-ry,x+rx,y+ry),p)
    }
    private fun circle(c: Canvas,x:Float,y:Float,r:Float,fill:String) {
        p.shader=null;p.style=Paint.Style.FILL;p.color=color(fill);c.drawCircle(x,y,r,p)
    }
    private fun rect(c: Canvas,l:Float,t:Float,r:Float,b:Float,radius:Float,fill:String) {
        p.shader=null;p.style=Paint.Style.FILL;p.color=color(fill)
        c.drawRoundRect(l,t,r,b,radius,radius,p)
    }
    private fun gradient(c: Canvas,l:Float,t:Float,r:Float,b:Float,radius:Float,
                         start:String,end:String) {
        p.style=Paint.Style.FILL
        p.shader=LinearGradient(l,t,r,b,color(start),color(end),Shader.TileMode.CLAMP)
        c.drawRoundRect(l,t,r,b,radius,radius,p)
        p.shader=null
    }
    private fun line(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,thick:Float,fill:String) {
        p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=thick
        p.strokeCap=Paint.Cap.ROUND;p.color=color(fill)
        c.drawLine(x1,y1,x2,y2,p);p.style=Paint.Style.FILL
    }
    private fun path(c:Canvas, fill:String, vararg points:Float) {
        val shape=Path()
        shape.moveTo(points[0],points[1])
        for(i in 2 until points.size step 2) shape.lineTo(points[i],points[i+1])
        shape.close()
        p.shader=null;p.style=Paint.Style.FILL;p.color=color(fill)
        c.drawPath(shape,p)
    }
    private fun word(c:Canvas,what:String,x:Float,y:Float,size:Float,fill:String,bold:Boolean=true) {
        p.shader=null;p.style=Paint.Style.FILL;p.color=color(fill)
        p.textSize=size
        p.typeface=Typeface.create("sans-serif", if(bold)Typeface.BOLD else Typeface.NORMAL)
        p.textAlign=Paint.Align.CENTER;c.drawText(what,x,y,p)
    }

    override fun onMeasure(w:Int,h:Int) {
        val target=(resources.displayMetrics.density*224).toInt()
        setMeasuredDimension(View.resolveSize(target,w), View.resolveSize(target,h))
    }

    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width/360f,height/240f)
        gradient(canvas,5f,5f,355f,235f,31f,"#FAF6FF","#E9DBFF")
        circle(canvas,183f,123f,101f,"#DCC8FF")
        circle(canvas,183f,123f,85f,"#EADFFF")
        circle(canvas,40f,35f,7f,"#FFFFFF")
        circle(canvas,324f,58f,11f,"#C5A2FE")
        circle(canvas,37f,177f,13f,"#D3B5FF")
        when(step) {
            1 -> headphones(canvas)
            2 -> quality(canvas)
            else -> rocket(canvas)
        }
        canvas.restore()
    }

    private fun headphones(c:Canvas) {
        // Hoodie and headphones portrait, built from vector shapes.
        oval(c,177f,213f,86f,73f,"#623ED2")
        path(c,"#4630A3",112f,226f,124f,154f,156f,141f,180f,158f,201f,141f,238f,159f,246f,227f)
        oval(c,184f,130f,48f,62f,"#FFC8A4")
        oval(c,170f,77f,56f,29f,"#33213F")
        oval(c,145f,98f,38f,31f,"#33213F")
        oval(c,218f,82f,22f,22f,"#33213F")
        // Headphones arc and ear-pads.
        p.color=color("#6D39DC");p.style=Paint.Style.STROKE;p.strokeWidth=13f
        c.drawArc(RectF(120f,65f,244f,173f),185f,178f,false,p)
        p.style=Paint.Style.FILL
        gradient(c,115f,118f,141f,166f,11f,"#733EDE","#4618AA")
        gradient(c,224f,116f,247f,162f,11f,"#753DE9","#4519A9")
        oval(c,168f,133f,4f,5f,"#35223D")
        oval(c,203f,131f,4f,5f,"#35223D")
        line(c,175f,154f,194f,154f,3f,"#A64D61")
        // Phone in right hand.
        c.save();c.rotate(-11f,255f,135f)
        rect(c,241f,83f,281f,161f,8f,"#24173E")
        rect(c,246f,92f,276f,152f,4f,"#49358C")
        circle(c,261f,145f,2.4f,"#E2D7FF")
        c.restore()
        oval(c,238f,164f,15f,23f,"#FFC8A4")
        // Recognizable floating platform glyphs, not blurry screenshots.
        gradient(c,51f,61f,95f,105f,12f,"#FF4545","#D51228")
        path(c,"#FFFFFF",68f,74f,68f,94f,86f,84f)
        gradient(c,267f,34f,309f,76f,13f,"#FD5184","#9B26E8")
        p.color=Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=3.5f
        c.drawRoundRect(276f,43f,300f,67f,7f,7f,p)
        c.drawCircle(288f,55f,6f,p);p.style=Paint.Style.FILL
        circle(c,297f,47f,2.4f,"#FFFFFF")
        gradient(c,55f,151f,96f,194f,12f,"#389AFF","#1260DF")
        word(c,"f",75f,185f,37f,"#FFFFFF")
        gradient(c,273f,165f,317f,208f,12f,"#22202F","#05050E")
        word(c,"♪",295f,198f,36f,"#FFFFFF")
    }

    private fun quality(c:Canvas) {
        circle(c,182f,142f,99f,"#D9C4FF")
        gradient(c,53f,83f,308f,201f,20f,"#A852FA","#5918CC")
        gradient(c,64f,94f,298f,188f,16f,"#7128E7","#4311A0")
        path(c,"#FFFFFF",160f,114f,160f,171f,205f,143f)
        line(c,91f,178f,248f,178f,5f,"#D8BDFD")
        line(c,91f,178f,176f,178f,5f,"#FFFFFF")
        gradient(c,74f,37f,142f,84f,12f,"#FFCF2B","#FF9517")
        gradient(c,160f,26f,244f,84f,12f,"#FF9D16","#F46B08")
        word(c,"HD",108f,68f,27f,"#FFFFFF")
        word(c,"4K",202f,66f,33f,"#FFFFFF")
        gradient(c,259f,152f,328f,220f,35f,"#AA50F8","#7421E1")
        line(c,293f,167f,293f,193f,5f,"#FFFFFF")
        path(c,"#FFFFFF",281f,187f,305f,187f,293f,200f)
        line(c,277f,205f,308f,205f,5f,"#FFFFFF")
    }

    private fun rocket(c:Canvas) {
        // Clouds, lift-off flames and sharply-rendered purple rocket.
        oval(c,100f,197f,63f,29f,"#FFFFFF")
        oval(c,165f,201f,78f,36f,"#FDF9FF")
        oval(c,256f,194f,76f,34f,"#FFFFFF")
        oval(c,234f,120f,80f,84f,"#C3A3FA")
        c.save()
        c.rotate(34f,193f,130f)
        path(c,"#F8A929",182f,195f,168f,227f,193f,210f,206f,230f,202f,194f)
        path(c,"#FFE75C",186f,195f,185f,218f,198f,197f)
        path(c,"#5B18B8",153f,161f,123f,184f,127f,143f,154f,121f)
        path(c,"#6D20CA",225f,162f,254f,187f,251f,143f,221f,124f)
        gradient(c,157f,43f,223f,195f,35f,"#B068FF","#5D10BC")
        path(c,"#7D27D7",157f,75f,190f,20f,223f,75f)
        oval(c,189f,102f,21f,21f,"#4E18AA")
        oval(c,189f,102f,14f,14f,"#E5D9FF")
        line(c,183f,144f,183f,170f,4f,"#D7A9FF")
        c.restore()
        circle(c,106f,61f,10f,"#FFFFFF")
        circle(c,286f,58f,7f,"#FFFFFF")
    }
}

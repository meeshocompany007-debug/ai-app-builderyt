package com.snapflow.downloader

import android.graphics.Color
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.text.InputType

/** Screens 1-8: splash, three onboarding steps, optional login, email, signup, success. */
class DayIntro(private val a: SnapDayActivity) {
    private val u get() = a.ui
    fun splash() {
        val root=a.root
        root.background=u.gradient(Color.parseColor("#121247"),Color.parseColor("#6A0FCB"),0)
        root.gravity=Gravity.CENTER
        root.addView(SnapDayLogo(a),LinearLayout.LayoutParams(u.dp(108),u.dp(108)))
        root.addView(u.space(20))
        root.addView(u.text("SnapDay",38,u.white,true,Gravity.CENTER))
        root.addView(u.text("Video Downloader & Player",16,u.white,false,Gravity.CENTER))
        root.addView(u.space(46))
        root.addView(android.widget.ProgressBar(a),LinearLayout.LayoutParams(u.dp(35),u.dp(35)))
        root.addView(u.space(16))
        root.addView(u.text("Loading...",13,u.white,false,Gravity.CENTER))
    }
    fun display(screen:String) {
        if(screen.startsWith("intro")) intro(screen)
        else auth(screen)
    }
    private fun intro(page:String) {
        a.root.setBackgroundColor(u.white)
        val step=when(page) { "intro1" -> 1; "intro2" -> 2; else -> 3 }
        val toolbar=u.row().apply {setPadding(u.dp(20),u.dp(17),u.dp(20),u.dp(9))}
        toolbar.addView(u.text("SnapDay",18,u.violet,true),LinearLayout.LayoutParams(0,-2,1f))
        toolbar.addView(u.text("Skip ("+step+"/3)",14,u.violet,true).apply {
            setOnClickListener {a.go("login")}
        })
        a.root.addView(toolbar)
        val center=u.col(24).apply { gravity=Gravity.CENTER }
        a.root.addView(center,LinearLayout.LayoutParams(-1,0,1f))
        val illustration=FrameLayout(a).apply {
            background=u.gradient(Color.parseColor("#F1D8FF"),Color.parseColor("#BB9BFF"),40)
        }
        if(step==1 || step==3) {
            val picture=android.widget.ImageView(a).apply {
                setImageResource(if(step==1)R.drawable.snapday_onboard_headphones else R.drawable.snapday_onboard_rocket)
                scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
                contentDescription=if(step==1)"Video platforms onboarding" else "Rocket onboarding"
            }
            illustration.addView(picture,FrameLayout.LayoutParams(-1,-1))
        } else {
            val quality=android.widget.ImageView(a).apply {
                setImageResource(R.drawable.snapday_onboard_quality)
                scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
                contentDescription="HD and 4K video quality selection illustration"
            }
            illustration.addView(quality,FrameLayout.LayoutParams(-1,-1))
        }
        center.addView(illustration,LinearLayout.LayoutParams(-1,u.dp(250)))
        center.addView(u.space(30))
        val headings=listOf("Download Videos\nfrom All Platforms",
            "Choose Your\nVideo Quality","Fast, Safe & Easy")
        val subtitles=listOf(
            "YouTube, Instagram, Facebook, TikTok and more — download where permission allows.",
            "Choose from available video formats and resolutions, from low to high quality.",
            "A lightweight video downloader with built-in player, library and settings."
        )
        center.addView(u.text(headings[step-1].replace("\\n","\n"),26,u.ink,true,Gravity.CENTER))
        center.addView(u.space(12))
        center.addView(u.text(subtitles[step-1],14,u.muted,false,Gravity.CENTER))
        center.addView(u.space(18))
        center.addView(u.text((1..3).joinToString("  "){if(it==step)"●" else "○"},
            23,u.violet,false,Gravity.CENTER))
        val bottom=u.col(18)
        bottom.addView(u.button(if(step==3)"Get Started  →" else "Next  →") {
            if(step==3)a.go("login") else a.go("intro"+(step+1))
        })
        a.root.addView(bottom)
    }
    private fun auth(page:String) {
        a.root.setBackgroundColor(u.white)
        val scroll=ScrollView(a)
        a.root.addView(scroll,LinearLayout.LayoutParams(-1,-1))
        val content=u.col(25).apply {gravity=Gravity.CENTER_HORIZONTAL}
        scroll.addView(content)
        fun centered(text:String,size:Int=15,color:Int=u.ink,bold:Boolean=false) {
            content.addView(u.text(text,size,color,bold,Gravity.CENTER))
        }
        if(page!="login") {
            content.addView(u.text("‹",32,u.ink,true).apply {
                setOnClickListener {a.go("login")}
            })
        } else content.addView(u.space(38))
        content.addView(SnapDayLogo(a),LinearLayout.LayoutParams(u.dp(76),u.dp(76)))
        content.addView(u.space(9))
        centered("SnapDay",28,u.ink,true)
        content.addView(u.space(20))
        when(page) {
            "login" -> {
                centered("Welcome Back",23,u.ink,true)
                centered("Login to sync downloads and watch history (Optional)",13,u.muted)
                content.addView(u.space(24))
                content.addView(u.button("G  Continue with Google",false) {
                    a.message("Google login needs an OAuth backend. Skip for Now to use the app without login.")
                })
                content.addView(u.space(11))
                content.addView(u.button("✉  Continue with Email",false) {a.go("email")})
                content.addView(u.space(23))
                centered("or",13,u.muted)
            }
            "email","signup" -> {
                centered(if(page=="email")"Login with Email" else "Create Account",22,u.ink,true)
                content.addView(u.space(20))
                if(page=="signup") {
                    content.addView(input("Enter Name"))
                    content.addView(u.space(10))
                }
                content.addView(input("Enter Email Address"))
                content.addView(u.space(10))
                val password=input(if(page=="email")"Enter Password" else "Create Password",true)
                content.addView(password)
                content.addView(u.space(11))
                content.addView(u.text("◉  Show password",13,u.violet,true).apply {
                    setOnClickListener {
                        password.inputType=if(password.inputType==129) 1 else 129
                        password.setSelection(password.text.length)
                    }
                })
                if(page=="email") content.addView(CheckBox(a).apply {
                    text="Remember me"
                    isChecked=true
                })
                content.addView(u.space(14))
                content.addView(u.button(if(page=="email") "Login" else "Sign Up") {
                    a.message("Account backend is not connected. No login or registration was performed. You can continue without an account.")
                })
                content.addView(u.space(14))
                if(page=="email") {
                    content.addView(u.text("Forgot Password?",14,u.violet,true,Gravity.CENTER).apply {
                        setOnClickListener {a.message("Password reset requires the future authentication backend.")}
                    })
                    content.addView(u.space(12))
                    content.addView(u.text("Don't have an account? Sign Up",14,u.violet,true,Gravity.CENTER).apply {
                        setOnClickListener {a.go("signup")}
                    })
                } else centered("By continuing, you agree to our Terms & Privacy Policy",12,u.muted)
            }
            "complete" -> {
                centered("🛡",78,u.violet,true)
                centered("You're All Set!",27,u.ink,true)
                centered("You can use SnapDay without login.",14,u.muted)
                content.addView(u.space(22))
                listOf("✓ Download permitted videos & MP3",
                    "✓ Watch saved videos and music","✓ Use all basic features").forEach {
                    centered(it,16,Color.parseColor("#149854"),true)
                    content.addView(u.space(11))
                }
                content.addView(u.space(30))
                content.addView(u.button("Continue") {
                    a.pref.edit().putBoolean("introduced",true).apply()
                    a.go("home")
                })
            }
        }
        if(page!="complete") {
            content.addView(u.space(25))
            content.addView(u.button("Skip for Now  ›",false) {a.go("complete")})
        }
    }
    private fun input(hintText:String,secret:Boolean=false): EditText =
        EditText(a).apply {
            hint=hintText
            textSize=15f
            setSingleLine(true)
            setPadding(u.dp(14),u.dp(13),u.dp(14),u.dp(13))
            background=u.shape(u.white,13,u.line)
            if(secret)inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
}

package com.snapflow.downloader

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Emulator smoke verification that all user-approved SnapDay screens actually render. */
@RunWith(AndroidJUnit4::class)
class SnapDayUiSmokeTest {
    @Test fun onboardingButtonsAndVectorArtworkAreVisible() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(SnapDayActivity::class.java).use { scenario ->
            for(screen in listOf("intro1","intro2","intro3")) {
                scenario.onActivity { a -> a.go(screen) }
                instrumentation.waitForIdleSync()
                scenario.onActivity { a ->
                    val cta=a.findViewById<ViewGroup>(android.R.id.content)
                        .findViewWithTag<android.view.View>("snapday_onboarding_cta")
                    val art=a.findViewById<ViewGroup>(android.R.id.content)
                        .findViewWithTag<android.view.View>("snapday_onboarding_vector")
                    assertTrue("Missing HD vector illustration on "+screen, art is DayIllustration)
                    assertTrue("Missing Next/Get Started on "+screen, cta!=null)
                    val r=android.graphics.Rect()
                    assertTrue("CTA not visible on "+screen, cta.getGlobalVisibleRect(r))
                    assertTrue("CTA too small to tap on "+screen, r.height()>=a.ui.dp(44))
                    assertTrue("CTA covered by bottom system bar on "+screen,
                        r.bottom <= a.window.decorView.rootView.height - a.ui.dp(8))
                }
            }
        }
    }

    @Test fun allStoryboardScreensCanOpen() {
        val pages=listOf("intro1","intro2","intro3","login","email","signup","complete",
            "home","video","quality","floatGuide","audio","downloads","player","files",
            "settings","browser","history","me","premium")
        ActivityScenario.launch(SnapDayActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                pages.forEach { page ->
                    a.go(page)
                    assertEquals("Route should be reachable", page, a.page)
                    val container=a.findViewById<ViewGroup>(android.R.id.content)
                    assertTrue("Route "+page+" has no UI",container.childCount>0)
                }
                a.go("home")
            }
            Thread.sleep(800)
            val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            assertTrue("Home screenshot must not be empty",bitmap.width>200 && bitmap.height>350)
            val samplePoints=listOf(0.18f,0.28f,0.45f,0.68f,0.83f)
            val screenVisible=samplePoints.any { fraction ->
                val pixel=bitmap.getPixel(bitmap.width/2,(bitmap.height*fraction).toInt())
                android.graphics.Color.red(pixel)>100 || android.graphics.Color.green(pixel)>100 || android.graphics.Color.blue(pixel)>100
            }
            assertTrue("SnapDay Home screenshot is blank/black; UI rendering must be fixed",screenVisible)
            scenario.onActivity { a ->
                FileOutputStream(File(a.filesDir,"snapday-home.png")).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,output)
                }
            }
            bitmap.recycle()
        }
    }
}

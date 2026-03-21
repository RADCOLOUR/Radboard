package com.radcolour.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var splashContent: View
    private lateinit var darkOverlay: View
    private lateinit var splashSpinner: ProgressBar
    private lateinit var logoView: LogoView

    private val handler = Handler(Looper.getMainLooper())
    private var loadingComplete = false
    private var animationComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_splash)

        splashContent = findViewById(R.id.splashContent)
        darkOverlay = findViewById(R.id.darkOverlay)
        splashSpinner = findViewById(R.id.splashSpinner)
        logoView = findViewById(R.id.logoView)

        // Start loading in background
        startLoading()

        // Start animation sequence
        startAnimationSequence()
    }

    // -------------------------------------------------------------------------
    // Animation sequence
    // -------------------------------------------------------------------------

    private fun startAnimationSequence() {
        // Step 1: Logo + content fades and scales in
        splashContent.scaleX = 0.6f
        splashContent.scaleY = 0.6f

        splashContent.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(200)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                // Step 2: Spinner fades in after logo appears
                splashSpinner.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        animationComplete = true
                        checkIfReadyToTransition()
                    }
                    .start()
            }
            .start()
    }

    // -------------------------------------------------------------------------
    // Loading — initialise repository and anything else needed
    // -------------------------------------------------------------------------

    private fun startLoading() {
        // Run on background thread
        Thread {
            // Init chord repository — loads builtin_chords.radpack
            ChordRepository.init(this)

            // Small minimum display time so splash doesn't flash too quickly
            Thread.sleep(1200)

            runOnUiThread {
                loadingComplete = true
                checkIfReadyToTransition()
            }
        }.start()
    }

    // -------------------------------------------------------------------------
    // Transition — only proceed when both animation and loading are done
    // -------------------------------------------------------------------------

    private fun checkIfReadyToTransition() {
        if (loadingComplete && animationComplete) {
            beginTransition()
        }
    }

    private fun beginTransition() {
        // Step 1: Fade overlay to black (bold colour → dark)
        darkOverlay.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                // Step 2: Launch MainActivity
                startActivity(Intent(this, MainActivity::class.java))

                // Step 3: Slide up transition
                overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )

                finish()
            }
            .start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}
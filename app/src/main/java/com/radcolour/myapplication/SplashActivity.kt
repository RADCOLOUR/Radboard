package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var splashContent: View
    private lateinit var darkOverlay: View
    private lateinit var splashSpinner: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvAppName: TextView
    private lateinit var downloadProgress: ProgressBar
    private lateinit var tvDownloadPercent: TextView

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
        tvStatus = findViewById(R.id.tvStatus)
        tvAppName = findViewById(R.id.tvAppName)
        downloadProgress = findViewById(R.id.downloadProgress)
        tvDownloadPercent = findViewById(R.id.tvDownloadPercent)

        startAnimationSequence()
    }

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private fun startAnimationSequence() {
        splashContent.scaleX = 0.8f
        splashContent.scaleY = 0.8f

        splashContent.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(150)
            .setInterpolator(OvershootInterpolator(1.1f))
            .withEndAction {
                splashSpinner.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        animationComplete = true
                        startLoading()
                    }
                    .start()
            }
            .start()
    }

    // -------------------------------------------------------------------------
    // Loading sequence
    // -------------------------------------------------------------------------

    private fun startLoading() {
        Thread {
            // Step 1 — Init chord repository
            runOnUiThread { tvStatus.text = "Loading chords..." }
            ChordRepository.init(this)
            Thread.sleep(400)

            // Step 2 — Check internet
            if (!UpdateManager.isInternetAvailable(this)) {
                runOnUiThread { showNoInternetDialog() }
                return@Thread
            }

            // Step 3 — Check for updates
            runOnUiThread { tvStatus.text = "Checking for updates..." }
            val release = UpdateManager.checkForUpdate()

            runOnUiThread {
                when {
                    release == null -> {
                        // Could not reach GitHub — proceed anyway
                        tvStatus.text = "Could not check for updates"
                        Thread.sleep(600)
                        finishLoading()
                    }
                    release.isNewer -> showUpdateDialog(release)
                    else -> finishLoading()
                }
            }
        }.start()
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    private fun showNoInternetDialog() {
        val dialogView = layoutInflater.inflate(
            android.R.layout.select_dialog_singlechoice, null
        )

        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage(
                "Radboard could not connect to the internet.\n\n" +
                        "Update checking and font loading require an internet connection. " +
                        "The app will still work without it."
            )
            .setPositiveButton("Retry") { _, _ ->
                tvStatus.text = "Retrying..."
                startLoading()
            }
            .setNegativeButton("Continue Anyway") { _, _ ->
                finishLoading()
            }
            .setCancelable(false)
            .show()
            .apply {
                // Style to match app
                getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(0xFF7DD6FF.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun showUpdateDialog(release: UpdateManager.ReleaseInfo) {
        AlertDialog.Builder(this)
            .setTitle("Update Available — v${release.tagName}")
            .setMessage(
                "A new version of Radboard is available.\n\n${release.releaseNotes}"
            )
            .setPositiveButton("Update Now") { _, _ ->
                startDownload(release.apkUrl)
            }
            .setNegativeButton("Skip") { _, _ ->
                finishLoading()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    private fun startDownload(apkUrl: String) {
        // Show download UI
        splashSpinner.visibility = View.GONE
        downloadProgress.visibility = View.VISIBLE
        tvDownloadPercent.visibility = View.VISIBLE
        tvStatus.text = "Downloading update..."

        ApkDownloader.download(
            context = this,
            apkUrl = apkUrl,
            onProgress = { percent ->
                downloadProgress.progress = percent
                tvDownloadPercent.text = "$percent%"
            },
            onComplete = {
                tvStatus.text = "Installing..."
                tvDownloadPercent.text = "100%"
                // Install is triggered automatically inside ApkDownloader
            },
            onError = {
                tvStatus.text = "Download failed"
                downloadProgress.visibility = View.GONE
                tvDownloadPercent.visibility = View.GONE
                splashSpinner.visibility = View.VISIBLE
                // Wait a moment then continue to app
                handler.postDelayed({ finishLoading() }, 1500)
            }
        )
    }

    // -------------------------------------------------------------------------
    // Transition to MainActivity
    // -------------------------------------------------------------------------

    private fun finishLoading() {
        loadingComplete = true
        tvStatus.text = ""
        darkOverlay.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(
                    R.anim.slide_in_up,
                    R.anim.slide_out_up
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
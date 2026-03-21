package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SplashActivity : AppCompatActivity() {

    private lateinit var splashContent: View
    private lateinit var darkOverlay: View
    private lateinit var splashSpinner: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvAppName: TextView
    private lateinit var downloadProgress: ProgressBar
    private lateinit var tvDownloadPercent: TextView

    private val handler = Handler(Looper.getMainLooper())

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
                        startLoading()
                    }
                    .start()
            }
            .start()
    }
    private fun startLoading() {
        Thread {
            runOnUiThread { tvStatus.setText(R.string.status_loading_chords) }
            ChordRepository.init(this)
            Thread.sleep(400)

            val existingUpdate = ApkDownloader.findExistingUpdate(this)
            if (existingUpdate != null) {
                val version = existingUpdate.name
                    .removePrefix("radboard-")
                    .removeSuffix(".apk")
                runOnUiThread { showExistingUpdateDialog(existingUpdate, version) }
                return@Thread
            }

            if (!UpdateManager.isInternetAvailable(this)) {
                runOnUiThread { showNoInternetDialog() }
                return@Thread
            }

            runOnUiThread { tvStatus.setText(R.string.status_checking_updates) }
            val release = UpdateManager.checkForUpdate(this)

            runOnUiThread {
                when {
                    release == null -> {
                        tvStatus.setText(R.string.status_update_failed)
                        handler.postDelayed({ finishLoading() }, 600)
                    }
                    release.isNewer -> showUpdateDialog(release)
                    else -> finishLoading()
                }
            }
        }.start()
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_no_internet_title)
            .setMessage(R.string.dialog_no_internet_message)
            .setPositiveButton(R.string.btn_retry) { _, _ ->
                tvStatus.setText(R.string.status_retrying)
                startLoading()
            }
            .setNegativeButton(R.string.btn_continue_anyway) { _, _ ->
                finishLoading()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFF7DD6FF.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun showUpdateDialog(release: UpdateManager.ReleaseInfo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_update_title, release.tagName))
            .setMessage(getString(R.string.dialog_update_message, release.releaseNotes))
            .setPositiveButton(R.string.btn_update_now) { _, _ ->
                startDownload(release.apkUrl, release.tagName)
            }
            .setNegativeButton(R.string.btn_skip) { _, _ ->
                finishLoading()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun showExistingUpdateDialog(apkFile: File, version: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_update_ready_title, version))
            .setMessage(R.string.dialog_update_ready_message)
            .setPositiveButton(R.string.btn_install_now) { _, _ ->
                ApkDownloader.installApk(this, apkFile)
            }
            .setNegativeButton(R.string.btn_skip) { _, _ ->
                apkFile.delete()
                finishLoading()
            }
            .setCancelable(false)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }
    private fun startDownload(apkUrl: String, version: String) {
        splashSpinner.visibility = View.GONE
        downloadProgress.visibility = View.VISIBLE
        tvDownloadPercent.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.status_downloading, version)

        ApkDownloader.download(
            context = this,
            apkUrl = apkUrl,
            version = version,
            onProgress = { percent ->
                downloadProgress.progress = percent
                tvDownloadPercent.text = getString(R.string.download_percent, percent)
            },
            onComplete = {
                tvStatus.setText(R.string.status_installing)
                tvDownloadPercent.text = getString(R.string.download_percent, 100)
            },
            onError = {
                tvStatus.setText(R.string.status_download_failed)
                downloadProgress.visibility = View.GONE
                tvDownloadPercent.visibility = View.GONE
                splashSpinner.visibility = View.VISIBLE
                handler.postDelayed({ finishLoading() }, 1500)
            }
        )
    }
    private fun finishLoading() {
        tvStatus.text = ""
        darkOverlay.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up)
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
        @Suppress("DEPRECATION")
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
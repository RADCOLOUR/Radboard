package com.radcolour.myapplication

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloader {

    private var downloadId = -1L
    private var onProgressUpdate: ((Int) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    fun download(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: () -> Unit
    ) {
        this.onProgressUpdate = onProgress
        this.onComplete = onComplete
        this.onError = onError

        // Delete old APK if exists
        val apkFile = getApkFile(context)
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Radboard Update")
            setDescription("Downloading update...")
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "radboard-update.apk"
            )
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_HIDDEN
            )
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // Register completion receiver
        context.registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        // Start polling progress
        startProgressPolling(context, dm)
    }

    private fun startProgressPolling(context: Context, dm: DownloadManager) {
        Thread {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val total = cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                )
                            )
                            val downloaded = cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )
                            )
                            if (total > 0) {
                                val progress = ((downloaded * 100) / total).toInt()
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    onProgressUpdate?.invoke(progress)
                                }
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL,
                        DownloadManager.STATUS_FAILED -> downloading = false
                    }
                }
                cursor.close()
                Thread.sleep(500)
            }
        }.start()
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                context.unregisterReceiver(this)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        onComplete?.invoke()
                        installApk(context)
                    } else {
                        onError?.invoke()
                    }
                }
                cursor.close()
            }
        }
    }

    fun installApk(context: Context) {
        val apkFile = getApkFile(context)
        if (!apkFile.exists()) {
            onError?.invoke()
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun getApkFile(context: Context): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "radboard-update.apk"
        )
    }
}
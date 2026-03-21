package com.radcolour.myapplication

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloader {

    private var downloadId = -1L
    private var onProgressUpdate: ((Int) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    fun findExistingUpdate(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return null

        return dir.listFiles()
            ?.filter { file ->
                file.name.startsWith("radboard-", ignoreCase = true)
                        && file.name.endsWith(".apk", ignoreCase = true)
                        && file.length() > 0
            }
            ?.filter { file ->
               
                val version = file.name
                    .removePrefix("radboard-")
                    .removeSuffix(".apk")
                UpdateManager.isVersionNewer(version, BuildConfig.VERSION_NAME)
            }
            ?.maxByOrNull { it.lastModified() }
    }

    fun download(
        context: Context,
        apkUrl: String,
        version: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: () -> Unit
    ) {
        this.onProgressUpdate = onProgress
        this.onComplete = onComplete
        this.onError = onError

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        dir?.listFiles()
            ?.filter { it.name.startsWith("radboard-") && it.name.endsWith(".apk") }
            ?.forEach { it.delete() }

        val fileName = "radboard-$version.apk"

        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Radboard Update v$version")
                setDescription("Downloading update...")
                setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or
                            DownloadManager.Request.NETWORK_MOBILE
                )
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            android.util.Log.d("RADBOARD_UPDATE", "Download enqueued: $fileName, id: $downloadId")

            startProgressPolling(context.applicationContext, dm, version)

        } catch (e: Exception) {
            android.util.Log.e("RADBOARD_UPDATE", "Download failed to start: ${e.message}")
            onError()
        }
    }

    private fun startProgressPolling(context: Context, dm: DownloadManager, version: String) {
        Thread {
            var downloading = true
            while (downloading) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)

                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        val total = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        val downloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )

                        android.util.Log.d("RADBOARD_UPDATE", "Status: $status, $downloaded / $total")

                        when (status) {
                            DownloadManager.STATUS_RUNNING,
                            DownloadManager.STATUS_PENDING -> {
                                if (total > 0) {
                                    val progress = ((downloaded * 100) / total).toInt()
                                    Handler(Looper.getMainLooper()).post {
                                        onProgressUpdate?.invoke(progress)
                                    }
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                android.util.Log.d("RADBOARD_UPDATE", "Download successful")
                                downloading = false
                                Handler(Looper.getMainLooper()).post {
                                    onProgressUpdate?.invoke(100)
                                    onComplete?.invoke()
                                }
                                Thread.sleep(800)
                                val apkFile = File(
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                    "radboard-$version.apk"
                                )
                                Handler(Looper.getMainLooper()).post {
                                    installApk(context, apkFile)
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                )
                                android.util.Log.e("RADBOARD_UPDATE", "Download failed, reason: $reason")
                                downloading = false
                                Handler(Looper.getMainLooper()).post {
                                    onError?.invoke()
                                }
                            }
                            DownloadManager.STATUS_PAUSED -> {
                                val reason = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                )
                                android.util.Log.d("RADBOARD_UPDATE", "Paused, reason: $reason")
                            }
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RADBOARD_UPDATE", "Polling error: ${e.message}")
                    downloading = false
                    Handler(Looper.getMainLooper()).post {
                        onError?.invoke()
                    }
                }
                Thread.sleep(500)
            }
        }.start()
    }

    fun installApk(context: Context, apkFile: File) {
        android.util.Log.d("RADBOARD_UPDATE", "installApk: ${apkFile.absolutePath}")
        android.util.Log.d("RADBOARD_UPDATE", "exists: ${apkFile.exists()}, size: ${apkFile.length()}")

        if (!apkFile.exists() || apkFile.length() == 0L) {
            android.util.Log.e("RADBOARD_UPDATE", "APK missing or empty")
            onError?.invoke()
            return
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            android.util.Log.d("RADBOARD_UPDATE", "APK URI: $apkUri")

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
            }

            val canHandle = context.packageManager
                .queryIntentActivities(intent, 0)
                .isNotEmpty()

            android.util.Log.d("RADBOARD_UPDATE", "Can handle: $canHandle")

            if (canHandle) {
                context.startActivity(intent)
            } else {
                android.util.Log.d("RADBOARD_UPDATE", "Falling back to ACTION_VIEW")
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(fallback)
            }

        } catch (e: Exception) {
            android.util.Log.e("RADBOARD_UPDATE", "Install error: ${e.message}")
            onError?.invoke()
        }
    }
}
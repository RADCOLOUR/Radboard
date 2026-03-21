package com.radcolour.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object UpdateManager {

    const val GITHUB_API_URL = "https://api.github.com/repos/RADCOLOUR/Radboard/releases/latest"
    const val GITHUB_RELEASES_URL = "https://github.com/RADCOLOUR/Radboard/releases/latest"
    const val CURRENT_VERSION = "1.0.0"

    data class ReleaseInfo(
        val tagName: String,
        val releaseNotes: String,
        val apkUrl: String,
        val isNewer: Boolean
    )

    // -------------------------------------------------------------------------
    // Internet check
    // -------------------------------------------------------------------------

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // -------------------------------------------------------------------------
    // Version check — call on background thread
    // Returns ReleaseInfo or null if check failed
    // -------------------------------------------------------------------------

    fun checkForUpdate(): ReleaseInfo? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.getString("tag_name").trimStart('v')
            val releaseNotes = json.optString("body", "No release notes available.")

            // Find the APK asset URL
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }

            // If no APK asset found, fall back to releases page
            if (apkUrl.isEmpty()) apkUrl = GITHUB_RELEASES_URL

            ReleaseInfo(
                tagName = tagName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                isNewer = isVersionNewer(tagName, CURRENT_VERSION)
            )

        } catch (e: Exception) {
            null
        }
    }

    // -------------------------------------------------------------------------
    // Version comparison
    // Compares semantic versioning e.g. "1.2.0" vs "1.0.0"
    // -------------------------------------------------------------------------

    fun isVersionNewer(remote: String, current: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
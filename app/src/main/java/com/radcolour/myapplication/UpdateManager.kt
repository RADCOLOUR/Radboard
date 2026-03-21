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
    const val CURRENT_VERSION = "1.0.2"

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

    fun checkForUpdate(context: Context): ReleaseInfo? {
        return try {
            val usePreRelease = SettingsActivity.isPreReleaseEnabled(context)

            // If pre-releases enabled, fetch all releases and find the newest
            // Otherwise use /releases/latest which only returns stable
            val apiUrl = if (usePreRelease)
                "https://api.github.com/repos/RADCOLOUR/Radboard/releases"
            else
                "https://api.github.com/repos/RADCOLOUR/Radboard/releases/latest"

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            // Parse depending on endpoint used
            val (tagName, releaseNotes, apkUrl) = if (usePreRelease) {
                // Returns array — find newest by tag
                val array = org.json.JSONArray(response)
                if (array.length() == 0) return null
                // Array is sorted newest first
                val latest = array.getJSONObject(0)
                Triple(
                    latest.getString("tag_name").trimStart('v'),
                    latest.optString("body", "No release notes available."),
                    findApkUrl(latest)
                )
            } else {
                val json = org.json.JSONObject(response)
                Triple(
                    json.getString("tag_name").trimStart('v'),
                    json.optString("body", "No release notes available."),
                    findApkUrl(json)
                )
            }

            ReleaseInfo(
                tagName = tagName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl.ifEmpty { GITHUB_RELEASES_URL },
                isNewer = isVersionNewer(tagName, BuildConfig.VERSION_NAME)
            )

        } catch (e: Exception) {
            null
        }
    }

    private fun findApkUrl(json: org.json.JSONObject): String {
        val assets = json.optJSONArray("assets") ?: return ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.getString("browser_download_url")
            }
        }
        return ""
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
package com.radcolour.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    const val GITHUB_API_URL = "https://api.github.com/repos/RADCOLOUR/Radboard/releases/latest"
    const val GITHUB_ALL_RELEASES_URL = "https://api.github.com/repos/RADCOLOUR/Radboard/releases"
    const val GITHUB_RELEASES_URL = "https://github.com/RADCOLOUR/Radboard/releases/latest"
    data class ReleaseInfo(
        val tagName: String,
        val releaseNotes: String,
        val apkUrl: String,
        val isNewer: Boolean,
        val isPreRelease: Boolean
    )
    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun checkForUpdate(context: Context): ReleaseInfo? {
        return try {
            val usePreRelease = SettingsActivity.isPreReleaseEnabled(context)
            val currentVersion = getCurrentVersion(context)

            android.util.Log.d("RADBOARD_UPDATE", "Current version: $currentVersion")
            android.util.Log.d("RADBOARD_UPDATE", "Pre-release enabled: $usePreRelease")

            val apiUrl = if (usePreRelease)
                GITHUB_ALL_RELEASES_URL
            else
                GITHUB_API_URL

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                android.util.Log.e("RADBOARD_UPDATE", "API response: ${connection.responseCode}")
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            val (tagName, releaseNotes, apkUrl, isPreRelease) = if (usePreRelease) {
                val array = org.json.JSONArray(response)
                if (array.length() == 0) return null

                android.util.Log.d("RADBOARD_UPDATE", "Total releases found: ${array.length()}")

                var latestPreRelease: org.json.JSONObject? = null
                var latestStable: org.json.JSONObject? = null

                for (i in 0 until array.length()) {
                    val release = array.getJSONObject(i)
                    val isPre = release.getBoolean("prerelease")
                    val isDraft = release.getBoolean("draft")
                    if (isDraft) continue
                    if (isPre && latestPreRelease == null) latestPreRelease = release
                    if (!isPre && latestStable == null) latestStable = release
                    if (latestPreRelease != null && latestStable != null) break
                }

                android.util.Log.d("RADBOARD_UPDATE", "Latest pre-release: ${latestPreRelease?.getString("tag_name") ?: "none"}")
                android.util.Log.d("RADBOARD_UPDATE", "Latest stable: ${latestStable?.getString("tag_name") ?: "none"}")

                val chosen = when {
                    latestPreRelease == null -> Pair(latestStable, false)
                    latestStable == null -> Pair(latestPreRelease, true)
                    else -> {
                        val preTag = latestPreRelease.getString("tag_name").trimStart('v')
                        val stableTag = latestStable.getString("tag_name").trimStart('v')
                        if (isVersionNewer(preTag, stableTag))
                            Pair(latestPreRelease, true)
                        else
                            Pair(latestStable, false)
                    }
                }

                val chosenRelease = chosen.first ?: return null
                val chosenIsPre = chosen.second

                Quadruple(
                    chosenRelease.getString("tag_name").trimStart('v'),
                    chosenRelease.optString("body", "No release notes available."),
                    findApkUrl(chosenRelease),
                    chosenIsPre
                )
            } else {
                val json = org.json.JSONObject(response)
                Quadruple(
                    json.getString("tag_name").trimStart('v'),
                    json.optString("body", "No release notes available."),
                    findApkUrl(json),
                    false
                )
            }

            android.util.Log.d("RADBOARD_UPDATE", "Chosen tag: $tagName, isPreRelease: $isPreRelease")
            android.util.Log.d("RADBOARD_UPDATE", "APK URL: $apkUrl")

            ReleaseInfo(
                tagName = tagName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl.ifEmpty { GITHUB_RELEASES_URL },
                isNewer = isVersionNewer(tagName, currentVersion),
                isPreRelease = isPreRelease
            )

        } catch (e: Exception) {
            android.util.Log.e("RADBOARD_UPDATE", "Update check failed: ${e.message}")
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
    fun isVersionNewer(remote: String, current: String): Boolean {
        return try {
            val remoteStripped = remote.trimStart('v')
            val currentStripped = current.trimStart('v')
            val remoteClean = remoteStripped.split("-", "_", " ")[0]
            val currentClean = currentStripped.split("-", "_", " ")[0]

            android.util.Log.d("RADBOARD_UPDATE", "Comparing remote: $remoteClean vs current: $currentClean")

            val remoteParts = remoteClean.split(".").map { it.toInt() }
            val currentParts = currentClean.split(".").map { it.toInt() }
            val maxLen = maxOf(remoteParts.size, currentParts.size)

            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }


            val remoteHasSuffix = remoteStripped.contains("_") ||
                    remoteStripped.contains("-") ||
                    remoteStripped.contains(" ")
            val currentHasSuffix = currentStripped.contains("_") ||
                    currentStripped.contains("-") ||
                    currentStripped.contains(" ")

            false
        } catch (e: Exception) {
            android.util.Log.e("RADBOARD_UPDATE", "Version compare error: ${e.message}")
            false
        }
    }
    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
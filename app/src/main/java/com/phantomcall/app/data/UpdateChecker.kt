package com.phantomcall.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object UpdateChecker {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/ravYOLO/phantom-call-android/releases/latest"
    private const val USER_AGENT = "PhantomCall"
    private const val TIMEOUT_MILLIS = 10_000
    private const val NOTES_MAX_LENGTH = 500

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            val versionName = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L)).versionName
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionName
            }
            val currentVersion = versionName ?: "0"
            val release = fetchLatestRelease()
            val remoteVersion = release.tagName.removePrefix("v")
            if (compareVersions(remoteVersion, currentVersion) > 0) {
                UpdateInfo(remoteVersion, release.name, release.body.take(NOTES_MAX_LENGTH), release.htmlUrl)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun fetchLatestRelease(): GitHubRelease {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            jsonFormat.decodeFromString<GitHubRelease>(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(remote: String, current: String): Int {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val partCount = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until partCount) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart.compareTo(currentPart)
        }
        return 0
    }

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        val name: String,
        val body: String,
        @SerialName("html_url") val htmlUrl: String
    )
}

data class UpdateInfo(val version: String, val title: String, val notes: String, val url: String)
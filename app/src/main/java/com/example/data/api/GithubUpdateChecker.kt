package com.example.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object GithubUpdateChecker {

    private val client = OkHttpClient()

    data class UpdateInfo(
        val hasUpdate: Boolean = false,
        val latestVersion: String = "",
        val releaseNotes: String = "",
        val downloadUrl: String = "",
        val errorMessage: String? = null
    )

    suspend fun checkForUpdates(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/faizinu61/AlarmGrubing/releases/latest")
                    .header("User-Agent", "AlarmGrupApp")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext if (response.code == 404) {
                            // Anggap sudah up to date jika tidak ada release ditemukan (404)
                            UpdateInfo(hasUpdate = false, errorMessage = null)
                        } else {
                            UpdateInfo(errorMessage = "Gagal memuat update dari GitHub (HTTP ${response.code}).")
                        }
                    }

                    val body = response.body?.string() ?: return@withContext UpdateInfo(errorMessage = "Respon GitHub kosong.")
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "")
                    val htmlUrl = json.optString("html_url", "https://github.com/faizinu61/AlarmGrubing/releases")
                    val bodyNotes = json.optString("body", "")

                    // Clean the version tag (e.g., "v1.0.15" -> "1.0.15")
                    val cleanLatest = tagName.removePrefix("v").trim()
                    val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").trim()

                    // Compare versions
                    val hasUpdate = isNewerVersion(cleanCurrent, cleanLatest)
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        releaseNotes = bodyNotes,
                        downloadUrl = htmlUrl
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateInfo(errorMessage = "Koneksi ke API GitHub gagal atau dibatasi. Silakan periksa jaringan internet Anda.")
            }
        }
    }

    fun canonicalizeVersion(versionStr: String): String {
        return versionStr.removePrefix("v").trim()
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val canonCurrent = canonicalizeVersion(current)
            val canonLatest = canonicalizeVersion(latest)

            val currParts = canonCurrent.split(".").map { it.toIntOrNull() ?: 0 }
            val lateParts = canonLatest.split(".").map { it.toIntOrNull() ?: 0 }

            val minSize = minOf(currParts.size, lateParts.size)
            for (i in 0 until minSize) {
                if (lateParts[i] > currParts[i]) return true
                if (lateParts[i] < currParts[i]) return false
            }
            return lateParts.size > currParts.size
        } catch (e: Exception) {
            return latest != current && latest.isNotEmpty()
        }
    }

    fun openUpdateUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

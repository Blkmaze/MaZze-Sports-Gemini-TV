package com.example.api

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdaterRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val defaultRepo = "Blkmaze/MaZze-Sports-Gemini-TV"

    suspend fun checkForUpdates(customRepo: String? = null): UpdateInfo = withContext(Dispatchers.IO) {
        val repo = if (!customRepo.isNullOrBlank()) customRepo else defaultRepo
        val url = "https://api.github.com/repos/$repo/releases/latest"
        val currentBuild = 1

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "MaZzeSportsTV/1.0")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val str = response.body?.string() ?: ""
                    parseReleaseResponse(str, currentBuild)
                } else {
                    UpdateInfo(currentBuildNumber = currentBuild, errorMessage = "HTTP ${response.code}")
                }
            }
        } catch (e: Throwable) {
            UpdateInfo(currentBuildNumber = currentBuild, errorMessage = e.localizedMessage ?: "Network error")
        }
    }

    private fun parseReleaseResponse(jsonStr: String, currentBuild: Int): UpdateInfo {
        return try {
            val json = JSONObject(jsonStr)
            val tag = json.optString("tag_name", "v1.0")
            val name = json.optString("name", tag)
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val digits = tag.filter { it.isDigit() }
            val latestVersion = digits.toIntOrNull() ?: 1
            val hasUpdate = latestVersion > currentBuild && downloadUrl.isNotBlank()

            UpdateInfo(
                currentBuildNumber = currentBuild,
                latestBuildNumber = latestVersion,
                hasUpdate = hasUpdate,
                releaseTitle = name,
                downloadUrl = downloadUrl
            )
        } catch (e: Throwable) {
            UpdateInfo(currentBuildNumber = currentBuild, errorMessage = "Parse error")
        }
    }

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(downloadUrl).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
                val contentLength = body.contentLength()
                val destFile = File(context.cacheDir, "update.apk")
                if (destFile.exists()) destFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = totalRead.toFloat() / contentLength.toFloat()
                                onProgress(progress)
                            }
                        }
                        output.flush()
                    }
                }
                Result.success(destFile)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun promptInstall(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.mazzesportstv.tvspz.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            // Safe ignore
        }
    }
}

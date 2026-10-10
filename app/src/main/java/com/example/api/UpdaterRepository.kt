package com.example.api

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UpdaterRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val defaultRepo = "Blkmaze/MaZze-Sports-Gemini-TV"

    suspend fun checkForUpdates(customRepo: String? = null): UpdateInfo = withContext(Dispatchers.IO) {
        val repo = if (!customRepo.isNullOrBlank()) customRepo.trim() else defaultRepo
        val url = "https://api.github.com/repos/$repo/releases/tags/Gemini-latest"
        val currentBuild = BuildConfig.VERSION_CODE

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MaZzeSportsTV/1.0")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Try fallback to latest release endpoint
                    val fallbackUrl = "https://api.github.com/repos/$repo/releases/latest"
                    val fallbackReq = Request.Builder()
                        .url(fallbackUrl)
                        .header("User-Agent", "MaZzeSportsTV/1.0")
                        .build()
                    val fallbackResp = client.newCall(fallbackReq).execute()
                    if (!fallbackResp.isSuccessful) {
                        return@withContext UpdateInfo(
                            currentBuildNumber = currentBuild,
                            errorMessage = "Release check returned ${response.code}. You are on build $currentBuild."
                        )
                    }
                    return@withContext parseReleaseResponse(fallbackResp.body?.string() ?: "", currentBuild)
                }

                val body = response.body?.string() ?: ""
                parseReleaseResponse(body, currentBuild)
            }
        } catch (e: Exception) {
            UpdateInfo(
                currentBuildNumber = currentBuild,
                errorMessage = "Network error: ${e.localizedMessage ?: "Unable to contact GitHub"}"
            )
        }
    }

    private fun parseReleaseResponse(jsonStr: String, currentBuild: Int): UpdateInfo {
        if (jsonStr.isBlank()) {
            return UpdateInfo(currentBuildNumber = currentBuild, errorMessage = "Empty response from GitHub")
        }

        return try {
            val json = JSONObject(jsonStr)
            val bodyText = json.optString("body", "")
            val tagName = json.optString("tag_name", "Gemini-latest")
            val releaseName = json.optString("name", "MaZze Sports TV")

            // Parse "Automated build N"
            val pattern = Pattern.compile("Automated build\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(bodyText)
            var latestBuild = 0
            if (matcher.find()) {
                latestBuild = matcher.group(1)?.toIntOrNull() ?: 0
            }

            // Also check tag or title if body didn't specify
            if (latestBuild == 0) {
                val titleMatcher = Pattern.compile("Build\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(releaseName)
                if (titleMatcher.find()) {
                    latestBuild = titleMatcher.group(1)?.toIntOrNull() ?: 0
                }
            }

            // Find APK asset
            var downloadUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val hasUpdate = latestBuild > currentBuild && !downloadUrl.isNullOrEmpty()

            UpdateInfo(
                latestBuildNumber = latestBuild,
                currentBuildNumber = currentBuild,
                hasUpdate = hasUpdate,
                releaseTitle = releaseName.ifBlank { tagName },
                downloadUrl = downloadUrl,
                errorMessage = if (latestBuild <= currentBuild) "App is up to date (Build $currentBuild)" else null
            )
        } catch (e: Exception) {
            UpdateInfo(currentBuildNumber = currentBuild, errorMessage = "Failed to parse release: ${e.localizedMessage}")
        }
    }

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "MaZzeSportsTV/1.0")
                .build()

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val outputFile = File(updateDir, "MaZze-Sports-Gemini.apk")

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Empty download body"))
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        onProgress(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Result.success(outputFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstall(apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

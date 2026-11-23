package com.example.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class ApkVerificationResult {
    data class Valid(val sha256: String, val archiveVersionName: String?) : ApkVerificationResult()
    data class Invalid(val reason: String) : ApkVerificationResult()
}

class ApkDownloaderAndVerifier(private val context: Context) {

    private val updatesDir: File
        get() = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }

    fun getPartialFile(release: ReleaseInfo): File {
        val safeName = (release.apkFileName ?: "karan-buds-${release.versionName}.apk")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(updatesDir, "$safeName.part")
    }

    fun getFinalApkFile(release: ReleaseInfo): File {
        val safeName = (release.apkFileName ?: "karan-buds-${release.versionName}.apk")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(updatesDir, safeName)
    }

    fun clearDownloads() {
        updatesDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    suspend fun downloadWithResume(
        release: ReleaseInfo,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val url = release.apkDownloadUrl
            ?: return@withContext Result.failure(IllegalStateException("This release does not attach an APK file."))
        if (!SemVerComparator.isTrustedGitHubUrl(url)) {
            return@withContext Result.failure(SecurityException("Untrusted update URL blocked."))
        }

        val partFile = getPartialFile(release)
        val finalFile = getFinalApkFile(release)
        if (finalFile.exists() && release.apkSizeBytes != null && finalFile.length() == release.apkSizeBytes) {
            onProgress(finalFile.length(), finalFile.length(), 1f)
            return@withContext Result.success(finalFile)
        }

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        runCatching {
            val conn = openTrustedRedirectedConnection(url, existingBytes)
            val responseCode = conn.responseCode

            val appendMode = responseCode == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L
            if (!appendMode && existingBytes > 0L) {
                existingBytes = 0L
            }

            if (responseCode !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
                conn.disconnect()
                error("Download failed with HTTP $responseCode")
            }

            val contentLength = conn.contentLengthLong
            val totalBytes = when {
                appendMode && contentLength > 0L -> existingBytes + contentLength
                contentLength > 0L -> contentLength
                release.apkSizeBytes != null && release.apkSizeBytes > 0L -> release.apkSizeBytes
                else -> -1L
            }

            var downloaded = existingBytes
            conn.inputStream.use { input ->
                FileOutputStream(partFile, appendMode).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        if (!isActive) throw CancellationException("Paused")
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (totalBytes > 0L) {
                            (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        onProgress(downloaded, totalBytes, progress)
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            finalFile
        }
    }

    fun verifyApk(apkFile: File, release: ReleaseInfo): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() < 1024L) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid("Downloaded file is missing or incomplete.")
        }

        // 1. Compute SHA-256 checksum
        val actualSha256 = computeSha256(apkFile)
        val expectedSha = release.expectedSha256?.trim()?.lowercase()
        if (expectedSha == null) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid(
                "Release is missing an official SHA-256 checksum asset. Auto-install blocked for safety."
            )
        }
        if (actualSha256 != expectedSha) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid(
                "SHA-256 checksum mismatch! Expected ${expectedSha.take(12)}..., got ${actualSha256.take(12)}..."
            )
        }

        // 2. Verify APK structure, packageName & signing certificate compatibility
        val pm = context.packageManager
        val archiveInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
        }

        if (archiveInfo == null) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid("Invalid or corrupted APK archive.")
        }

        if (archiveInfo.packageName != context.packageName) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid(
                "Package ID mismatch (${archiveInfo.packageName} != ${context.packageName})."
            )
        }

        // 3. Signing certificate compatibility check against currently installed app
        val installedDigests = getInstalledSigningDigests()
        val archiveDigests = extractSigningDigests(archiveInfo)
        if (installedDigests.isNotEmpty() && archiveDigests.isNotEmpty()) {
            val matches = installedDigests.any { it in archiveDigests }
            if (!matches) {
                runCatching { apkFile.delete() }
                return ApkVerificationResult.Invalid(
                    "APK signing certificate does not match the installed KARAN BUDS signature."
                )
            }
        } else if (archiveDigests.isEmpty()) {
            runCatching { apkFile.delete() }
            return ApkVerificationResult.Invalid("Downloaded APK is unsigned.")
        }

        return ApkVerificationResult.Valid(
            sha256 = actualSha256,
            archiveVersionName = archiveInfo.versionName
        )
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun buildInstallSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildPackageInstallerIntent(apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun openTrustedRedirectedConnection(initialUrl: String, rangeOffset: Long): HttpURLConnection {
        var current = initialUrl
        repeat(5) {
            if (!SemVerComparator.isTrustedGitHubUrl(current)) {
                throw SecurityException("Redirect to untrusted host blocked: $current")
            }
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "KaranBudsPremium-Updater")
                if (rangeOffset > 0L) {
                    setRequestProperty("Range", "bytes=$rangeOffset-")
                }
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next.isNullOrBlank()) error("Redirect missing Location header")
                current = next
            } else {
                return conn
            }
        }
        error("Too many redirects while downloading update")
    }

    private fun computeSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getInstalledSigningDigests(): Set<String> {
        val pm = context.packageManager
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull() ?: return emptySet()
        return extractSigningDigests(info)
    }

    private fun extractSigningDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
            } else emptyArray()
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }
        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.reset()
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}

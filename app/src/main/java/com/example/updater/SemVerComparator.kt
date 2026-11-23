package com.example.updater

object SemVerComparator {

    private val repoSlugRegex = Regex("^[A-Za-z0-9_.-]{1,64}$")

    fun isValidRepoSlug(value: String): Boolean = repoSlugRegex.matches(value.trim())

    fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
            .trim()
    }

    /**
     * Returns true ONLY if [remoteVersion] is strictly greater than [installedVersion].
     */
    fun isRemoteNewer(
        installedVersion: String,
        remoteVersion: String,
        installedCode: Int? = null,
        remoteCode: Int? = null
    ): Boolean {
        val cmp = compare(remoteVersion, installedVersion)
        if (cmp > 0) return true
        if (cmp < 0) return false
        if (installedCode != null && remoteCode != null && remoteCode > installedCode) {
            return true
        }
        return false
    }

    fun compare(a: String, b: String): Int {
        val partsA = normalizeVersion(a).split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = normalizeVersion(b).split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    fun isTrustedGitHubUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return host == "github.com" ||
            host == "api.github.com" ||
            host == "objects.githubusercontent.com" ||
            host == "release-assets.githubusercontent.com" ||
            host.endsWith(".githubusercontent.com")
    }
}

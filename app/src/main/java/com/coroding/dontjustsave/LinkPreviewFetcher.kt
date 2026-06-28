package com.coroding.dontjustsave

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LinkPreviewResult(
    val previewTitle: String?,
    val previewDescription: String?,
    val previewImageUrl: String?,
    val previewFetchedAt: Long,
    val previewStatus: String,
)

data class PreviewImageCandidate(
    val rawUrl: String,
    val source: String,
    val priority: Int,
)

object LinkPreviewFetcher {
    private const val TAG = "LinkPreviewFetcher"
    private const val TIMEOUT_MILLIS = 7_000
    private const val MAX_REDIRECTS = 5
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
    private val IMAGE_META_KEYS = listOf(
        "og:image:secure_url",
        "og:image",
        "og:image:url",
        "twitter:image",
        "twitter:image:src",
    )

    suspend fun fetch(sourceUrl: String): LinkPreviewResult = withContext(Dispatchers.IO) {
        val fetchedAt = System.currentTimeMillis()
        runCatching {
            val finalUrl = resolveRedirectUrl(sourceUrl)
            val connection = openConnection(finalUrl)

            connection.use {
                val responseCode = it.responseCode
                if (responseCode !in 200..399) {
                    Log.d(TAG, "sourceUrl=$sourceUrl responseCode=$responseCode")
                    failed(fetchedAt)
                } else {
                    val html = it.inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                    val baseUrl = it.url?.toString().orEmpty().ifBlank { finalUrl }
                    val ogTitle = extractMetaContent(html, "og:title")
                    val twitterTitle = extractMetaContent(html, "twitter:title")
                    val pageTitle = extractTitle(html)
                    val description = extractMetaContent(html, "og:description")
                        ?: extractMetaContent(html, "description")
                        ?: extractMetaContent(html, "twitter:description")
                    val imageUrl = extractPreviewImageUrl(html, baseUrl)
                    val title = (ogTitle ?: twitterTitle ?: pageTitle)
                        ?.takeIf { title -> title.isNotBlank() }
                    val cleanDescription = description?.takeIf { text -> text.isNotBlank() }
                    val cleanImageUrl = imageUrl?.takeIf { image -> image.isNotBlank() }

                    Log.d(TAG, "sourceUrl=$sourceUrl finalUrl=$baseUrl")
                    Log.d(TAG, "previewTitle=${title.orEmpty()}")
                    Log.d(TAG, "previewImageUrl=${cleanImageUrl.orEmpty()}")

                    if (title == null && cleanDescription == null && cleanImageUrl == null) {
                        failed(fetchedAt)
                    } else {
                        LinkPreviewResult(
                            previewTitle = title,
                            previewDescription = cleanDescription,
                            previewImageUrl = cleanImageUrl,
                            previewFetchedAt = fetchedAt,
                            previewStatus = "success",
                        )
                    }
                }
            }
        }.getOrElse { error ->
            Log.d(TAG, "preview failed: ${error.message}")
            failed(fetchedAt)
        }
    }

    private fun failed(fetchedAt: Long): LinkPreviewResult = LinkPreviewResult(
        previewTitle = null,
        previewDescription = null,
        previewImageUrl = null,
        previewFetchedAt = fetchedAt,
        previewStatus = "failed",
    )

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }
    }

    private fun HttpURLConnection.use(block: (HttpURLConnection) -> LinkPreviewResult): LinkPreviewResult {
        return try {
            block(this)
        } finally {
            disconnect()
        }
    }

    fun resolveRedirectUrl(url: String): String {
        var currentUrl = url
        repeat(MAX_REDIRECTS) {
            val connection = openConnection(currentUrl).apply {
                instanceFollowRedirects = false
            }
            try {
                val responseCode = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (responseCode in 300..399 && !location.isNullOrBlank()) {
                    currentUrl = normalizeUrl(location, currentUrl) ?: location
                } else {
                    return currentUrl
                }
            } finally {
                connection.disconnect()
            }
        }
        return currentUrl
    }

    private fun extractPreviewImageUrl(
        html: String,
        baseUrl: String,
    ): String? {
        return (
            extractImageFromMeta(html, baseUrl) +
                extractImageFromJsonLd(html, baseUrl) +
                extractFallbackImageFromImgTags(html, baseUrl)
            )
            .sortedBy { it.priority }
            .firstOrNull()
            ?.rawUrl
    }

    fun extractImageFromMeta(
        html: String,
        baseUrl: String,
    ): List<PreviewImageCandidate> {
        val candidates = mutableListOf<PreviewImageCandidate>()
        IMAGE_META_KEYS.forEachIndexed { index, key ->
            val rawImageUrl = extractMetaContent(html, key)
            val imageUrl = rawImageUrl?.let { normalizeUrl(it, baseUrl) }
            if (!imageUrl.isNullOrBlank() && isUsefulImageUrl(imageUrl)) {
                candidates += PreviewImageCandidate(
                    rawUrl = imageUrl,
                    source = "meta:$key",
                    priority = index,
                )
            }
        }
        extractImageSrcLink(html)
            ?.let { normalizeUrl(it, baseUrl) }
            ?.takeIf { isUsefulImageUrl(it) }
            ?.let {
                candidates += PreviewImageCandidate(
                    rawUrl = it,
                    source = "link:image_src",
                    priority = 7,
                )
            }
        return candidates
    }

    fun extractImageFromJsonLd(
        html: String,
        baseUrl: String,
    ): List<PreviewImageCandidate> {
        val scriptRegex = Regex(
            """<script[^>]+type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val imageRegexes = listOf(
            Regex(""""image"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""image"\s*:\s*\[\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""thumbnailUrl"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""thumbnailUrl"\s*:\s*\[\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
        )
        return scriptRegex.findAll(html)
            .flatMap { scriptMatch ->
                val json = decodeHtmlEntities(scriptMatch.groupValues.getOrNull(1).orEmpty())
                imageRegexes.mapNotNull { regex ->
                    regex.find(json)?.groupValues?.getOrNull(1)
                }
            }
            .mapNotNull { normalizeUrl(it, baseUrl) }
            .filter(::isUsefulImageUrl)
            .mapIndexed { index, imageUrl ->
                PreviewImageCandidate(
                    rawUrl = imageUrl,
                    source = "json-ld",
                    priority = 8 + index,
                )
            }
            .toList()
    }

    fun extractFallbackImageFromImgTags(
        html: String,
        baseUrl: String,
    ): List<PreviewImageCandidate> {
        val imgTagRegex = Regex("""<img\s+[^>]*>""", RegexOption.IGNORE_CASE)
        return imgTagRegex.findAll(html)
            .map { it.value }
            .mapNotNull { tag ->
                extractAttribute(tag, "src")
                    ?: extractAttribute(tag, "data-src")
                    ?: extractAttribute(tag, "data-original")
            }
            .mapNotNull { normalizeUrl(it, baseUrl) }
            .filter(::isUsefulImageUrl)
            .take(3)
            .mapIndexed { index, imageUrl ->
                PreviewImageCandidate(
                    rawUrl = imageUrl,
                    source = "img",
                    priority = 20 + index,
                )
            }
            .toList()
    }

    private fun extractMetaContent(html: String, key: String): String? {
        val metaTagRegex = Regex("""<meta\s+[^>]*>""", RegexOption.IGNORE_CASE)
        return metaTagRegex.findAll(html)
            .map { it.value }
            .firstNotNullOfOrNull { tag ->
                val property = extractAttribute(tag, "property") ?: extractAttribute(tag, "name")
                val content = extractAttribute(tag, "content")
                if (property.equals(key, ignoreCase = true) && !content.isNullOrBlank()) {
                    content.let(::decodeHtmlEntities)
                } else {
                    null
                }
            }
    }

    private fun extractImageSrcLink(html: String): String? {
        val linkTagRegex = Regex("""<link\s+[^>]*>""", RegexOption.IGNORE_CASE)
        return linkTagRegex.findAll(html)
            .map { it.value }
            .firstNotNullOfOrNull { tag ->
                val rel = extractAttribute(tag, "rel")
                val href = extractAttribute(tag, "href")
                if (
                    rel?.split(Regex("""\s+"""))
                        ?.any { it.equals("image_src", ignoreCase = true) } == true &&
                    !href.isNullOrBlank()
                ) {
                    decodeHtmlEntities(href)
                } else {
                    null
                }
            }
    }

    private fun extractAttribute(tag: String, name: String): String? {
        val regex = Regex("""\b$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        return regex.find(tag)?.groupValues?.getOrNull(1)
    }

    private fun extractTitle(html: String): String? {
        val regex = Regex(
            """<title[^>]*>(.*?)</title>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeHtmlEntities)
            ?.trim()
    }

    fun normalizeUrl(rawUrl: String, baseUrl: String): String? {
        val trimmed = decodeHtmlEntities(rawUrl).trim()
        if (trimmed.isBlank() || trimmed.startsWith("data:", ignoreCase = true)) {
            return null
        }

        return runCatching {
            when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("//") -> "https:$trimmed"
                else -> URL(URL(baseUrl), trimmed).toString()
            }
        }.getOrNull()
    }

    private fun isUsefulImageUrl(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase()
        if (lower.contains("favicon") || lower.contains("logo") || lower.contains("sprite")) {
            return false
        }
        if (lower.contains("1x1") || lower.contains("pixel") || lower.contains("tracking")) {
            return false
        }
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.contains("image") ||
            lower.contains("cover") ||
            lower.contains("thumb") ||
            lower.contains("poster")
    }

    private fun decodeHtmlEntities(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}

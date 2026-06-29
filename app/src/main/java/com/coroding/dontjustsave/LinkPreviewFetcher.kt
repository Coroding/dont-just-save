package com.coroding.dontjustsave

import android.util.Log
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LinkPreviewResult(
    val previewTitle: String?,
    val previewDescription: String?,
    val previewImageUrl: String?,
    val previewFetchedAt: Long,
    val previewStatus: String,
    val sourceTitle: String? = previewTitle,
    val sourceDescription: String? = previewDescription,
    val sourceAuthor: String? = null,
    val sourceType: String? = null,
    val resolvedUrl: String? = null,
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
                    val title = (
                        extractMetaContent(html, "og:title")
                            ?: extractMetaContent(html, "twitter:title")
                            ?: extractTitle(html)
                            ?: extractH1(html)
                        )
                        ?.takeIf { title -> title.isNotBlank() }
                    val description = (
                        extractMetaContent(html, "og:description")
                        ?: extractMetaContent(html, "description")
                        ?: extractMetaContent(html, "twitter:description")
                        ?: extractFirstParagraph(html)
                        )
                        ?.takeIf { text -> text.isNotBlank() }
                    val author = extractMetaContent(html, "author")
                        ?: extractMetaContent(html, "article:author")
                        ?: extractAuthorFromJsonLd(html)
                    val imageUrl = extractPreviewImageUrl(html, baseUrl)
                    val cleanImageUrl = imageUrl?.takeIf { image -> image.isNotBlank() }
                    val sourceType = determineSourceType(html, baseUrl)

                    Log.d(TAG, "sourceUrl=$sourceUrl finalUrl=$baseUrl")
                    Log.d(TAG, "previewTitle=${title.orEmpty()}")
                    Log.d(TAG, "previewImageUrl=${cleanImageUrl.orEmpty()}")
                    // TODO metrics: link_metadata_extracted and source_title_extracted.

                    if (title == null && description == null && cleanImageUrl == null && author == null) {
                        failed(fetchedAt, baseUrl)
                    } else {
                        LinkPreviewResult(
                            previewTitle = title,
                            previewDescription = description,
                            previewImageUrl = cleanImageUrl,
                            previewFetchedAt = fetchedAt,
                            previewStatus = "success",
                            sourceTitle = title,
                            sourceDescription = description,
                            sourceAuthor = author?.takeIf { value -> value.isNotBlank() },
                            sourceType = sourceType,
                            resolvedUrl = baseUrl,
                        )
                    }
                }
            }
        }.getOrElse { error ->
            Log.d(TAG, "preview failed: ${error.message}")
            failed(fetchedAt)
        }
    }

    private fun failed(
        fetchedAt: Long,
        resolvedUrl: String? = null,
    ): LinkPreviewResult = LinkPreviewResult(
        previewTitle = null,
        previewDescription = null,
        previewImageUrl = null,
        previewFetchedAt = fetchedAt,
        previewStatus = "failed",
        resolvedUrl = resolvedUrl,
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

    private fun extractH1(html: String): String? {
        val regex = Regex(
            """<h1[^>]*>(.*?)</h1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::stripHtmlTags)
            ?.let(::decodeHtmlEntities)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractFirstParagraph(html: String): String? {
        val regex = Regex(
            """<p[^>]*>(.*?)</p>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return regex.findAll(html)
            .map { it.groupValues.getOrNull(1).orEmpty() }
            .map(::stripHtmlTags)
            .map(::decodeHtmlEntities)
            .map { it.trim() }
            .firstOrNull { it.length in 20..180 }
    }

    private fun extractAuthorFromJsonLd(html: String): String? {
        val scriptRegex = Regex(
            """<script[^>]+type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val authorRegexes = listOf(
            Regex(""""author"\s*:\s*\{[^}]*"name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""author"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""creator"\s*:\s*\{[^}]*"name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""channel"\s*:\s*\{[^}]*"name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
        )
        return scriptRegex.findAll(html)
            .map { decodeHtmlEntities(it.groupValues.getOrNull(1).orEmpty()) }
            .firstNotNullOfOrNull { json ->
                authorRegexes.firstNotNullOfOrNull { regex ->
                    regex.find(json)?.groupValues?.getOrNull(1)
                }
            }
            ?.let(::decodeHtmlEntities)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun determineSourceType(html: String, resolvedUrl: String): String {
        val domain = domainFromUrl(resolvedUrl).lowercase()
        val ogType = extractMetaContent(html, "og:type").orEmpty().lowercase()
        return when {
            ogType.contains("video") -> "video"
            domain.contains("bilibili.com") ||
                domain.contains("b23.tv") ||
                domain.contains("youtube.com") ||
                domain.contains("youtu.be") ||
                domain.contains("douyin.com") -> "video"
            domain.contains("mp.weixin.qq.com") ||
                domain.contains("zhihu.com") ||
                domain.contains("medium.com") ||
                ogType.contains("article") ||
                extractFirstParagraph(html) != null -> "article"
            else -> "unknown"
        }
    }

    private fun domainFromUrl(url: String): String {
        return runCatching { URI(url).host }
            .getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: url
    }

    private fun stripHtmlTags(value: String): String {
        return value.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
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

package com.coroding.dontjustsave

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import java.net.URI

data class ShareContent(
    val rawText: String?,
    val sourceUrl: String?,
    val subject: String?,
    val imageUri: String?,
    val localImagePath: String?,
    val sourcePlatform: String,
    val sourceDomain: String?,
    val shareType: String,
) {
    companion object {
        fun manual(): ShareContent = ShareContent(
            rawText = null,
            sourceUrl = null,
            subject = null,
            imageUri = null,
            localImagePath = null,
            sourcePlatform = "手动记录",
            sourceDomain = null,
            shareType = "unknown",
        )
    }
}

object ShareContentParser {
    fun parseClipboardText(text: String): ShareContent? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val sourceUrl = extractFirstUrl(trimmed) ?: return null
        return buildContent(
            rawText = trimmed,
            sourceUrl = sourceUrl,
            subject = null,
            imageUri = null,
        )
    }

    fun parse(intent: Intent): ShareContent? {
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }

        val textParts = mutableListOf<String>()
        val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.trim()
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()?.takeIf {
            it.isNotBlank()
        }?.let(textParts::add)
        subject?.takeIf { it.isNotBlank() }?.let(textParts::add)
        intent.dataString?.trim()?.takeIf { it.isNotBlank() }?.let(textParts::add)

        val clipImageUri = collectClipData(intent.clipData, textParts)
        val streamImageUri = collectStreamUri(intent)
        val imageUri = streamImageUri ?: clipImageUri
        val rawText = textParts
            .distinct()
            .joinToString(separator = "\n")
            .takeIf { it.isNotBlank() }
        val sourceUrl = extractFirstUrl(listOfNotNull(rawText, subject).joinToString("\n"))
        if (rawText == null && sourceUrl == null && imageUri == null && subject == null) {
            return null
        }

        return buildContent(
            rawText = rawText,
            sourceUrl = sourceUrl,
            subject = subject,
            imageUri = imageUri,
        )
    }

    private fun buildContent(
        rawText: String?,
        sourceUrl: String?,
        subject: String?,
        imageUri: String?,
    ): ShareContent {
        val sourceDomain = sourceUrl?.let(::extractDomain)
        val sourcePlatform = detectPlatform(
            sourceUrl = sourceUrl,
            rawText = rawText,
            hasImage = imageUri != null,
        )
        val shareType = detectShareType(
            rawText = rawText,
            sourceUrl = sourceUrl,
            imageUri = imageUri,
        )

        return ShareContent(
            rawText = rawText,
            sourceUrl = sourceUrl,
            subject = subject,
            imageUri = imageUri,
            localImagePath = null,
            sourcePlatform = sourcePlatform,
            sourceDomain = sourceDomain,
            shareType = shareType,
        )
    }

    private fun collectClipData(
        clipData: ClipData?,
        textParts: MutableList<String>,
    ): String? {
        if (clipData == null) return null

        var firstUri: String? = null
        repeat(clipData.itemCount) { index ->
            val item = clipData.getItemAt(index)
            item.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(textParts::add)
            item.uri?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { uri ->
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    textParts.add(uri)
                } else if (firstUri == null) {
                    firstUri = uri
                }
            }
        }
        return firstUri
    }

    private fun collectStreamUri(intent: Intent): String? {
        val single = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString()
        if (!single.isNullOrBlank()) {
            return single
        }

        val multiple = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        return multiple?.firstOrNull()?.toString()
    }

    private fun extractFirstUrl(text: String): String? {
        val match = Regex("""https?://\S+""").find(text) ?: return null
        return match.value.trimEnd(
            '.',
            ',',
            ';',
            ':',
            ')',
            ']',
            '}',
            '。',
            '，',
            '；',
            '：',
            '）',
            '】',
            '》',
        )
    }

    private fun detectPlatform(
        sourceUrl: String?,
        rawText: String?,
        hasImage: Boolean,
    ): String {
        val haystack = listOfNotNull(sourceUrl, rawText).joinToString(" ").lowercase()
        return when {
            "xiaohongshu.com" in haystack || "xhslink.com" in haystack -> "小红书"
            "bilibili.com" in haystack || "b23.tv" in haystack -> "哔哩哔哩"
            "zhihu.com" in haystack -> "知乎"
            "youtube.com" in haystack || "youtu.be" in haystack -> "YouTube"
            "mp.weixin.qq.com" in haystack -> "公众号"
            sourceUrl != null -> "网页"
            hasImage -> "图片"
            else -> "未知来源"
        }
    }

    private fun detectShareType(
        rawText: String?,
        sourceUrl: String?,
        imageUri: String?,
    ): String {
        return when {
            sourceUrl != null && imageUri != null -> "mixed"
            imageUri != null -> "image"
            sourceUrl != null -> "link"
            rawText != null -> "text"
            else -> "unknown"
        }
    }

    private fun extractDomain(sourceUrl: String): String? {
        return runCatching { URI(sourceUrl).host }
            .getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }
}

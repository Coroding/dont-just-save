package com.coroding.dontjustsave

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager

object OpenSourceHelper {
    fun openSource(
        context: Context,
        sourceUrl: String?,
        sourcePlatform: String?,
        sourceDomain: String?,
        onError: (String) -> Unit,
    ) {
        if (sourceUrl.isNullOrBlank()) {
            onError("这条收藏没有可打开的链接")
            return
        }

        val uri = runCatching { Uri.parse(sourceUrl) }.getOrNull()
        if (uri == null || uri.scheme.isNullOrBlank()) {
            onError("没有可打开此链接的应用")
            return
        }

        val packageCandidates = platformPackageCandidates(
            sourceUrl = sourceUrl,
            sourcePlatform = sourcePlatform,
            sourceDomain = sourceDomain,
        )
        packageCandidates.forEach { packageName ->
            val platformIntent = baseViewIntent(context, uri).setPackage(packageName)
            if (tryStartActivity(context, platformIntent)) {
                return
            }
        }

        val viewIntent = baseViewIntent(context, uri)
        if (!hasActivityForIntent(context, viewIntent)) {
            onError("没有可打开此链接的应用")
            return
        }

        val chooserIntent = Intent.createChooser(viewIntent, "选择打开方式").apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        if (!tryStartActivity(context, chooserIntent)) {
            onError("没有可打开此链接的应用")
        }
    }

    private fun baseViewIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun hasActivityForIntent(context: Context, intent: Intent): Boolean {
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    private fun platformPackageCandidates(
        sourceUrl: String,
        sourcePlatform: String?,
        sourceDomain: String?,
    ): List<String> {
        val haystack = listOfNotNull(sourceUrl, sourcePlatform, sourceDomain)
            .joinToString(" ")
            .lowercase()
        return when {
            "xiaohongshu.com" in haystack || "xhslink.com" in haystack ->
                listOf("com.xingin.xhs")

            "bilibili.com" in haystack || "b23.tv" in haystack ->
                listOf("tv.danmaku.bili", "com.bilibili.app.in")

            "zhihu.com" in haystack ->
                listOf("com.zhihu.android")

            "youtube.com" in haystack || "youtu.be" in haystack ->
                listOf("com.google.android.youtube")

            "mp.weixin.qq.com" in haystack ->
                listOf("com.tencent.mm")

            else -> emptyList()
        }
    }
}

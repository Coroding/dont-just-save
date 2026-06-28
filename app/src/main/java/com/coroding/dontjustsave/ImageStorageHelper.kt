package com.coroding.dontjustsave

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStorageHelper {
    fun copyImageToPrivateStorage(context: Context, sourceUri: Uri): String? {
        return runCatching {
            val imageDir = File(context.filesDir, "topic_images").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val targetFile = File(
                imageDir,
                "topic_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg",
            )

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            targetFile.absolutePath
        }.getOrNull()
    }

    fun deleteLocalImageIfExists(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }

        runCatching {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        }
    }

    fun createCroppedCover(
        context: Context,
        localImagePath: String?,
        imageUri: String?,
        aspectRatio: String,
    ): String? {
        return runCatching {
            val bitmap = decodeBitmap(context, localImagePath, imageUri) ?: return null
            val ratio = when (aspectRatio) {
                "16:9" -> 16f / 9f
                "3:4" -> 3f / 4f
                else -> 4f / 3f
            }
            val cropped = centerCrop(bitmap, ratio)
            val imageDir = File(context.filesDir, "topic_images").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val targetFile = File(
                imageDir,
                "cover_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg",
            )
            targetFile.outputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            if (cropped !== bitmap) {
                cropped.recycle()
            }
            bitmap.recycle()
            targetFile.absolutePath
        }.getOrNull()
    }

    private fun decodeBitmap(
        context: Context,
        localImagePath: String?,
        imageUri: String?,
    ): Bitmap? {
        localImagePath?.takeIf { it.isNotBlank() }?.let { path ->
            val file = File(path)
            if (file.exists()) {
                return BitmapFactory.decodeFile(path)
            }
        }
        return imageUri
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
    }

    private fun centerCrop(
        bitmap: Bitmap,
        targetRatio: Float,
    ): Bitmap {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        if (sourceRatio > targetRatio) {
            cropHeight = bitmap.height
            cropWidth = (cropHeight * targetRatio).toInt().coerceAtMost(bitmap.width)
        } else {
            cropWidth = bitmap.width
            cropHeight = (cropWidth / targetRatio).toInt().coerceAtMost(bitmap.height)
        }
        val x = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }
}

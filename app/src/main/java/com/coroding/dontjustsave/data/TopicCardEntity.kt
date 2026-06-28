package com.coroding.dontjustsave.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topic_cards")
data class TopicCardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceText: String?,
    val sourceUrl: String?,
    val sourcePlatform: String,
    val sourceDomain: String?,
    val shareType: String,
    val imageUri: String?,
    val localImagePath: String? = null,
    val croppedImagePath: String? = null,
    val coverAspectRatio: String = "4:3",
    val previewTitle: String? = null,
    val previewDescription: String? = null,
    val previewImageUrl: String? = null,
    val previewFetchedAt: Long? = null,
    val previewStatus: String = "idle",
    val userNote: String,
    val category: String,
    val aiTags: String? = null,
    val aiReusableStructure: String? = null,
    val aiReferenceValue: String? = null,
    val aiReason: String? = null,
    val aiConfidence: Float? = null,
    val nextAction: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

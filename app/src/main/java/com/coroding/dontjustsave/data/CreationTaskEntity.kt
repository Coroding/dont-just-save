package com.coroding.dontjustsave.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creation_tasks")
data class CreationTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceCardIds: String,
    val contentDirection: String,
    val outline: String,
    val materialList: String,
    val nextAction: String,
    val generatedByAi: Boolean = false,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

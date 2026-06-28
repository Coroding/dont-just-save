package com.coroding.dontjustsave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CreationTaskDao {
    @Query("SELECT * FROM creation_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CreationTaskEntity>>

    @Query("SELECT * FROM creation_tasks WHERE id = :taskId LIMIT 1")
    fun observeById(taskId: String): Flow<CreationTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(creationTask: CreationTaskEntity)

    @Update
    suspend fun update(creationTask: CreationTaskEntity)
}

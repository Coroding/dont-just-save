package com.coroding.dontjustsave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicCardDao {
    @Query("SELECT * FROM topic_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TopicCardEntity>>

    @Query("SELECT * FROM topic_cards WHERE id = :cardId LIMIT 1")
    fun observeById(cardId: String): Flow<TopicCardEntity?>

    @Query("SELECT * FROM topic_cards WHERE sourceUrl = :sourceUrl LIMIT 1")
    suspend fun findBySourceUrl(sourceUrl: String): TopicCardEntity?

    @Query(
        """
        UPDATE topic_cards
        SET previewTitle = :previewTitle,
            previewDescription = :previewDescription,
            previewImageUrl = :previewImageUrl,
            previewFetchedAt = :previewFetchedAt,
            previewStatus = :previewStatus,
            updatedAt = :updatedAt
        WHERE id = :cardId
        """,
    )
    suspend fun updateLinkPreview(
        cardId: String,
        previewTitle: String?,
        previewDescription: String?,
        previewImageUrl: String?,
        previewFetchedAt: Long?,
        previewStatus: String,
        updatedAt: Long,
    )

    @Query(
        """
        SELECT * FROM topic_cards
        WHERE createdAt >= :since
        AND status NOT IN ('done', 'dropped')
        ORDER BY createdAt DESC
        """,
    )
    fun observeReviewCandidates(since: Long): Flow<List<TopicCardEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(topicCard: TopicCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(topicCards: List<TopicCardEntity>)

    @Update
    suspend fun update(topicCard: TopicCardEntity)

    @Query("DELETE FROM topic_cards WHERE id = :cardId")
    suspend fun deleteById(cardId: String)
}

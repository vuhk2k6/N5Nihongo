package com.vunv.n5nihongo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vunv.n5nihongo.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE lessonId = :lessonId LIMIT 1")
    fun getProgressByLesson(lessonId: Int): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress ORDER BY lastUpdated DESC")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress ORDER BY lastUpdated DESC")
    suspend fun getAllProgressOnce(): List<UserProgress>

    @Upsert
    suspend fun upsertProgress(progress: UserProgress)

    @Query(
        """
        UPDATE user_progress
        SET score = :score, isCompleted = :isCompleted, lastUpdated = :lastUpdated
        WHERE lessonId = :lessonId
        """
    )
    suspend fun updateProgress(
        lessonId: Int,
        score: Int,
        isCompleted: Boolean,
        lastUpdated: Long
    )
}

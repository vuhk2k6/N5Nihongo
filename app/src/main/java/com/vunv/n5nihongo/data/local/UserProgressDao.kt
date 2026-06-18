package com.vunv.n5nihongo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vunv.n5nihongo.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND lessonId = :lessonId LIMIT 1")
    fun getProgressByLesson(userId: String, lessonId: Int): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getAllProgress(userId: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId ORDER BY lastUpdated DESC")
    suspend fun getAllProgressOnce(userId: String): List<UserProgress>

    /** Legacy: get all progress regardless of user (for sync worker). */
    @Query("SELECT * FROM user_progress ORDER BY lastUpdated DESC")
    suspend fun getAllProgressOnce(): List<UserProgress>

    @Upsert
    suspend fun upsertProgress(progress: UserProgress)

    @Query(
        """
        UPDATE user_progress
        SET score = :score, isCompleted = :isCompleted, lastUpdated = :lastUpdated
        WHERE userId = :userId AND lessonId = :lessonId
        """
    )
    suspend fun updateProgress(
        userId: String,
        lessonId: Int,
        score: Int,
        isCompleted: Boolean,
        lastUpdated: Long
    )

    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun clearProgressForUser(userId: String)
}

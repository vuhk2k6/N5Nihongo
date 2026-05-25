package com.vunv.n5nihongo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vunv.n5nihongo.data.model.Grammar
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarDao {

    @Query("SELECT * FROM grammar WHERE lessonId = :lessonId ORDER BY id ASC")
    fun getGrammarByLesson(lessonId: Int): Flow<List<Grammar>>

    @Query("SELECT * FROM grammar WHERE lessonId = :lessonId ORDER BY id ASC")
    suspend fun getGrammarByLessonOnce(lessonId: Int): List<Grammar>

    @Query("SELECT COUNT(*) FROM grammar")
    suspend fun getGrammarCount(): Int

    @Query("SELECT * FROM grammar")
    suspend fun getAllGrammarOnce(): List<Grammar>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammar(grammarList: List<Grammar>)
}

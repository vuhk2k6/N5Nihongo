package com.vunv.n5nihongo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vunv.n5nihongo.data.model.Kanji
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {

    @Query("SELECT * FROM kanji WHERE lessonId = :lessonId ORDER BY id ASC")
    fun getKanjiByLesson(lessonId: Int): Flow<List<Kanji>>

    @Query("SELECT * FROM kanji WHERE lessonId = :lessonId ORDER BY id ASC")
    suspend fun getKanjiByLessonOnce(lessonId: Int): List<Kanji>

    @Query("SELECT * FROM kanji ORDER BY id ASC")
    fun getAllKanji(): Flow<List<Kanji>>

    @Query("SELECT * FROM kanji WHERE id = :id LIMIT 1")
    fun getKanjiById(id: Int): Flow<Kanji?>

    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun getKanjiCount(): Int

    @Query("SELECT * FROM kanji")
    suspend fun getAllKanjiOnce(): List<Kanji>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanji(kanjiList: List<Kanji>)
}

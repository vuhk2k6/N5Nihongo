package com.vunv.n5nihongo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vunv.n5nihongo.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY lessonId ASC, id ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsOnce(): List<Word>

    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY id ASC")
    fun getWordsByLesson(lessonId: Int): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY id ASC")
    suspend fun getWordsByLessonOnce(lessonId: Int): List<Word>

    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByLesson(lessonId: Int, limit: Int): List<Word>

    @Query("SELECT * FROM words WHERE lessonId = :lessonId AND type = :type ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByLessonAndType(lessonId: Int, type: String, limit: Int): List<Word>

    @Query("SELECT * FROM words WHERE id IN (:ids) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByIds(ids: List<Int>, limit: Int): List<Word>

    @Query("SELECT * FROM words WHERE id IN (:ids) ORDER BY id ASC")
    suspend fun getWordsByIds(ids: List<Int>): List<Word>

    @Query("SELECT COUNT(*) FROM words WHERE lessonId = :lessonId AND type = :type")
    suspend fun getWordCountByLessonAndType(lessonId: Int, type: String): Int

    @Query("DELETE FROM words WHERE lessonId = :lessonId AND type = :type")
    suspend fun deleteWordsByLessonAndType(lessonId: Int, type: String)

    @Query(
        """
        SELECT * FROM words
        WHERE level = :level AND id != :excludeWordId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomWordsByLevelExcluding(
        level: String,
        excludeWordId: Int,
        limit: Int
    ): List<Word>

    @Query(
        """
        SELECT * FROM words
        WHERE level = :level AND type = :type AND id != :excludeWordId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomWordsByLevelAndTypeExcluding(
        level: String,
        type: String,
        excludeWordId: Int,
        limit: Int
    ): List<Word>

    @Query(
        """
        SELECT * FROM words
        WHERE id IN (:ids) AND id != :excludeWordId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomWordsByIdsExcluding(
        ids: List<Int>,
        excludeWordId: Int,
        limit: Int
    ): List<Word>

    @Query("SELECT * FROM words WHERE level = :level ORDER BY id ASC")
    fun getWordsByLevel(level: String): Flow<List<Word>>

    /**
     * Words whose `kanji` or `furigana` column contains [chr] (e.g. all words that use the kanji
     * "一"). Used by the Kanji detail screen to show example vocabulary.
     *
     * Note: parameter is named `chr` rather than `char` because `char` is a Java reserved keyword
     * and Room generates Java code that would otherwise fail to compile.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE kanji LIKE '%' || :chr || '%' OR furigana LIKE '%' || :chr || '%'
        ORDER BY lessonId ASC, id ASC
        """
    )
    fun getWordsContainingChar(chr: String): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Query("UPDATE words SET isFavorite = :isFavorite WHERE id = :wordId")
    suspend fun updateFavoriteStatus(wordId: Int, isFavorite: Boolean)

    @Query("UPDATE words SET level = :level, isFavorite = :isFavorite WHERE id = :wordId")
    suspend fun updateLevelAndFavorite(wordId: Int, level: String, isFavorite: Boolean)
}

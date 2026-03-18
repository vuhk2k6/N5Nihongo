package com.vunv.n5nihongo.data.local

import androidx.room.*
import com.vunv.n5nihongo.data.model.WordEntity

@Dao
interface WordDao {

    // Lấy toàn bộ từ vựng (Dùng cho màn hình danh sách tổng)
    @Query("SELECT * FROM n5_vocabulary")
    fun getAllWords(): List<WordEntity>

    // Lấy từ vựng theo bài học (Rất quan trọng cho logic chọn Lesson của bạn)
    @Query("SELECT * FROM n5_vocabulary WHERE lesson = :lessonNumber")
    fun getWordsByLesson(lessonNumber: Int): List<WordEntity>

    // Lấy những từ chưa thuộc để ôn tập
    @Query("SELECT * FROM n5_vocabulary WHERE isMemorized = 0")
    fun getUnlearnedWords(): List<WordEntity>

    // Cập nhật trạng thái đã thuộc/chưa thuộc khi người dùng bấm nút
    @Update
    fun updateWord(word: WordEntity)

    // Thêm danh sách từ vựng (Dùng khi nạp dữ liệu từ file JSON lần đầu)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(words: List<WordEntity>)
}
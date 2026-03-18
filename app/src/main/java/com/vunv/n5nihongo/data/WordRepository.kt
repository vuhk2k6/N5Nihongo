package com.vunv.n5nihongo.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vunv.n5nihongo.data.local.WordDao
import com.vunv.n5nihongo.data.model.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(private val wordDao: WordDao) {

    // Hàm lấy từ vựng theo bài học
    fun getWordsByLesson(lesson: Int) = wordDao.getWordsByLesson(lesson)

    // Logic quan trọng: Đọc JSON và đổ vào Database
    suspend fun populateDatabaseFromJSON(context: Context) {
        withContext(Dispatchers.IO) {
            // Kiểm tra xem database đã có dữ liệu chưa
            val existingWords = wordDao.getAllWords()
            if (existingWords.isEmpty()) {
                // 1. Đọc file từ thư mục assets
                val jsonString = context.assets.open("n5_vocabulary.json")
                    .bufferedReader().use { it.readText() }

                // 2. Chuyển đổi JSON thành danh sách WordEntity
                val listType = object : TypeToken<List<WordEntity>>() {}.type
                val words: List<WordEntity> = Gson().fromJson(jsonString, listType)

                // 3. Lưu vào Database
                wordDao.insertAll(words)
            }
        }
    }
}
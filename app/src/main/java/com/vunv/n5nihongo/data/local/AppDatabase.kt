package com.vunv.n5nihongo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vunv.n5nihongo.data.model.Grammar
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Lesson
import com.vunv.n5nihongo.data.model.UserProgress
import com.vunv.n5nihongo.data.model.Word

@Database(
    entities = [Word::class, UserProgress::class, Lesson::class, Grammar::class, Kanji::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun lessonDao(): LessonDao
    abstract fun grammarDao(): GrammarDao
    abstract fun kanjiDao(): KanjiDao
}

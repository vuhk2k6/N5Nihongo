package com.vunv.n5nihongo.data.repository

import android.content.Context
import com.google.gson.Gson
import com.vunv.n5nihongo.data.local.GrammarDao
import com.vunv.n5nihongo.data.local.KanjiDao
import com.vunv.n5nihongo.data.local.LessonDao
import com.vunv.n5nihongo.data.local.WordDao
import com.vunv.n5nihongo.data.model.Grammar
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Lesson
import com.vunv.n5nihongo.data.model.LessonDataJson
import com.vunv.n5nihongo.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class DataRepository(
    private val context: Context,
    private val lessonDao: LessonDao,
    private val wordDao: WordDao,
    private val grammarDao: GrammarDao,
    private val kanjiDao: KanjiDao
) {
    private val gson = Gson()

    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val count = lessonDao.getLessonCount()
        if (count > 0) return@withContext

        val specialLessons = listOf(
            Lesson(id = 1, title = "Bảng chữ cái Hiragana", description = "Làm quen với Hiragana", orderIndex = 1),
            Lesson(id = 2, title = "Bảng chữ cái Katakana", description = "Làm quen với Katakana", orderIndex = 2),
            Lesson(id = 3, title = "Số đếm & Thời gian", description = "Nền tảng số đếm", orderIndex = 3)
        )
        lessonDao.insertLessons(specialLessons)

        seedAlphabetWords()

        for (i in 1..25) {
            seedLessonIfNeeded(i, i + 3)
        }
    }

    private suspend fun seedAlphabetWords() {
        val hiraganaData = listOf(
            "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
            "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
            "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
            "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
            "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
            "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
            "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
            "や" to "ya", "ゆ" to "yu", "よ" to "yo",
            "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
            "わ" to "wa", "を" to "wo", "ん" to "n"
        )
        val hiraganaWords = hiraganaData.mapIndexed { index, (kana, romaji) ->
            Word(id = 5000 + index, lessonId = 1, kanji = "", furigana = kana, meaning = romaji, type = "hiragana", level = "n5", isFavorite = false)
        }

        val katakanaData = listOf(
            "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
            "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
            "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
            "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
            "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
            "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
            "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
            "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
            "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
            "ワ" to "wa", "ヲ" to "wo", "ン" to "n"
        )
        val katakanaWords = katakanaData.mapIndexed { index, (kana, romaji) ->
            Word(id = 6000 + index, lessonId = 2, kanji = "", furigana = kana, meaning = romaji, type = "katakana", level = "n5", isFavorite = false)
        }

        wordDao.insertWords(hiraganaWords + katakanaWords)
    }

    suspend fun seedLessonIfNeeded(jsonLessonId: Int, dbLessonId: Int) = withContext(Dispatchers.IO) {
        val lessonDataJson = loadLessonData(jsonLessonId) ?: return@withContext

        // Insert Lesson
        val lesson = Lesson(
            id = dbLessonId,
            title = "Bài $jsonLessonId: ${lessonDataJson.lesson.title}",
            description = lessonDataJson.lesson.description,
            orderIndex = dbLessonId
        )
        lessonDao.insertLessons(listOf(lesson))

        // Insert Words
        val words = lessonDataJson.vocabulary.map { v ->
            Word(
                id = v.wordId + (dbLessonId * 1000), // Ensure unique IDs
                lessonId = dbLessonId,
                kanji = v.kanji,
                furigana = v.furigana,
                romaji = v.romaji,
                meaning = v.meaning,
                type = v.type,
                level = "n5",
                isFavorite = false
            )
        }
        if (words.isNotEmpty()) {
            wordDao.insertWords(words)
        }

        // Insert Grammar
        val grammars = lessonDataJson.grammar.map { g ->
            Grammar(
                id = g.grammarId + (dbLessonId * 1000), // Ensure unique IDs
                lessonId = dbLessonId,
                title = g.title,
                structure = g.structure,
                explanation = g.explanation,
                examples = gson.toJson(g.examples)
            )
        }
        if (grammars.isNotEmpty()) {
            grammarDao.insertGrammar(grammars)
        }

        // Insert Kanji
        val kanjis = lessonDataJson.kanji.map { k ->
            Kanji(
                id = k.kanjiId + (dbLessonId * 1000), // Ensure unique IDs
                lessonId = dbLessonId,
                character = k.character,
                onyomi = k.onyomi,
                kunyomi = k.kunyomi,
                meaning = k.meaning,
                strokeCount = k.strokeCount
            )
        }
        if (kanjis.isNotEmpty()) {
            kanjiDao.insertKanji(kanjis)
        }
    }

    private fun loadLessonData(lessonId: Int): LessonDataJson? {
        return try {
            val fileName = "lessons/lesson_$lessonId.json"
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, LessonDataJson::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

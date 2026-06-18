package com.vunv.n5nihongo.data.repository

import com.vunv.n5nihongo.data.local.GrammarDao
import com.vunv.n5nihongo.data.local.KanjiDao
import com.vunv.n5nihongo.data.local.UserProgressDao
import com.vunv.n5nihongo.data.local.WordDao
import com.vunv.n5nihongo.data.model.UserProgress
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.data.quiz.GeneratedQuizQuestion
import com.vunv.n5nihongo.data.quiz.LESSON_QUIZ_PASS_PERCENT
import com.vunv.n5nihongo.data.quiz.LessonQuizGenerator

class QuizRepository(
    private val wordDao: WordDao,
    private val grammarDao: GrammarDao,
    private val kanjiDao: KanjiDao,
    private val userProgressDao: UserProgressDao,
    private val quizGenerator: LessonQuizGenerator = LessonQuizGenerator()
) {

    /**
     * Đề kiểm tra bài: 10 từ vựng + toàn bộ ngữ pháp (nghĩa + cấu trúc) + 5 kanji.
     * Nguồn: `assets/lessons/lesson_*.json`.
     */
    suspend fun buildLessonQuiz(
        lessonId: Int,
        selectedWordIds: List<Int> = emptyList()
    ): List<GeneratedQuizQuestion> {
        val words = if (selectedWordIds.isNotEmpty()) {
            wordDao.getWordsByIds(selectedWordIds)
        } else {
            wordDao.getWordsByLessonOnce(lessonId)
        }
        val grammars = grammarDao.getGrammarByLessonOnce(lessonId)
        val kanjis = kanjiDao.getKanjiByLessonOnce(lessonId)
        return quizGenerator.buildQuiz(
            lessonId = lessonId,
            words = words,
            grammars = grammars,
            kanjis = kanjis,
            selectedWordIds = selectedWordIds
        )
    }

    /** Giữ tương thích nếu chỉ cần danh sách từ cho flashcard / logic cũ. */
    suspend fun getQuizWords(
        lessonId: Int,
        totalQuestions: Int = 10,
        selectedWordIds: List<Int> = emptyList()
    ): List<Word> {
        if (selectedWordIds.isNotEmpty()) {
            return wordDao.getRandomWordsByIds(ids = selectedWordIds, limit = totalQuestions)
        }
        if (lessonId == 1) {
            return wordDao.getRandomWordsByLessonAndType(lessonId, "hiragana", totalQuestions)
        }
        if (lessonId == 2) {
            return wordDao.getRandomWordsByLessonAndType(lessonId, "katakana", totalQuestions)
        }
        return wordDao.getRandomWordsByLesson(lessonId, totalQuestions)
    }

    suspend fun updateLessonScore(
        userId: String,
        lessonId: Int,
        correctCount: Int,
        totalQuestions: Int
    ) {
        val scorePercent = if (totalQuestions == 0) 0 else ((correctCount * 100f) / totalQuestions).toInt()
        val now = System.currentTimeMillis()
        userProgressDao.upsertProgress(
            UserProgress(
                userId = userId,
                lessonId = lessonId,
                score = scorePercent,
                isCompleted = scorePercent >= LESSON_QUIZ_PASS_PERCENT,
                lastUpdated = now
            )
        )
    }
}

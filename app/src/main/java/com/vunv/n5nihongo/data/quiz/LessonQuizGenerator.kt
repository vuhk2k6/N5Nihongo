package com.vunv.n5nihongo.data.quiz

import com.vunv.n5nihongo.data.model.Grammar
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Word
import kotlin.random.Random

/** Ngưỡng hoàn thành bài học khi làm quiz đầy đủ (Minna no Nihongo). */
const val LESSON_QUIZ_PASS_PERCENT = 80

const val LESSON_QUIZ_VOCAB_COUNT = 10
const val LESSON_QUIZ_KANJI_COUNT = 5

enum class QuizCategory {
    VOCABULARY,
    GRAMMAR,
    KANJI
}

enum class QuizPromptType {
    WORD_TO_MEANING,
    MEANING_TO_WORD,
    LISTENING,
    /** Nghĩa tiếng Việt → chọn mẫu ngữ pháp tiếng Nhật. */
    GRAMMAR_MEANING_TO_PATTERN,
    /** Mẫu ngữ pháp tiếng Nhật → chọn nghĩa tiếng Việt. */
    GRAMMAR_PATTERN_TO_MEANING,
    GRAMMAR_EXAMPLE,
    KANJI_MEANING,
    KANJI_READING
}

data class GeneratedQuizQuestion(
    val category: QuizCategory,
    val promptType: QuizPromptType,
    val prompt: String,
    val options: List<String>,
    val correctAnswer: String,
    val audioText: String = "",
    val word: Word? = null
)

class LessonQuizGenerator {

    fun buildQuiz(
        lessonId: Int,
        words: List<Word>,
        grammars: List<Grammar>,
        kanjis: List<Kanji>,
        @Suppress("UNUSED_PARAMETER") totalQuestions: Int = 15,
        selectedWordIds: List<Int> = emptyList()
    ): List<GeneratedQuizQuestion> {
        if (lessonId == 1 || lessonId == 2) {
            val pool = if (selectedWordIds.isNotEmpty()) {
                words.filter { it.id in selectedWordIds }
            } else {
                val typed = if (lessonId == 1) "hiragana" else "katakana"
                words.filter { it.type == typed && it.furigana.isNotBlank() }
            }
            return pool.shuffled()
                .mapNotNull { buildVocabMeaningQuestion(it, pool) }
        }

        if (selectedWordIds.isNotEmpty()) {
            val pool = words.filter { it.id in selectedWordIds }.ifEmpty { words }
            return pool.shuffled()
                .take(LESSON_QUIZ_VOCAB_COUNT.coerceAtMost(pool.size))
                .mapNotNull { buildVocabMeaningQuestion(it, pool) }
        }

        return buildFullLessonQuiz(words, grammars, kanjis)
    }

    /**
     * Đề kiểm tra bài: 10 từ (hỏi nghĩa) + toàn bộ ngữ pháp (nghĩa + chọn cấu trúc) + 5 kanji (nghĩa).
     */
    private fun buildFullLessonQuiz(
        words: List<Word>,
        grammars: List<Grammar>,
        kanjis: List<Kanji>
    ): List<GeneratedQuizQuestion> {
        val vocabPool = words.filter {
            it.meaning.isNotBlank() && (it.furigana.isNotBlank() || it.kanji.isNotBlank())
        }
        val vocabQs = vocabPool.shuffled()
            .take(LESSON_QUIZ_VOCAB_COUNT.coerceAtMost(vocabPool.size))
            .mapNotNull { buildVocabMeaningQuestion(it, vocabPool) }

        val grammarQs = grammars.flatMap { buildGrammarExamQuestions(it, grammars) }

        val kanjiPool = kanjis.filter { it.meaning.isNotBlank() }
        val kanjiQs = kanjiPool.shuffled()
            .take(LESSON_QUIZ_KANJI_COUNT.coerceAtMost(kanjiPool.size))
            .mapNotNull { buildKanjiMeaningQuestion(it, kanjiPool) }

        return (vocabQs + grammarQs + kanjiQs).shuffled()
    }

    /** Chỉ hỏi nghĩa: hiện từ Nhật → chọn nghĩa tiếng Việt. */
    private fun buildVocabMeaningQuestion(word: Word, pool: List<Word>): GeneratedQuizQuestion? {
        if (word.meaning.isBlank()) return null
        val wordText = if (word.kanji.isNotBlank()) word.kanji else word.furigana
        if (wordText.isBlank()) return null

        return GeneratedQuizQuestion(
            category = QuizCategory.VOCABULARY,
            promptType = QuizPromptType.WORD_TO_MEANING,
            prompt = wordText,
            options = fourOptions(word.meaning, pool.map { it.meaning }),
            correctAnswer = word.meaning,
            audioText = word.furigana.ifBlank { word.kanji },
            word = word
        )
    }

    /**
     * Mỗi mẫu ngữ pháp: 1 câu — hoặc cho nghĩa (Việt) chọn mẫu Nhật, hoặc ngược lại.
     * VD: 「～ほうがいいです」 ↔ "nên làm gì đó".
     */
    private fun buildGrammarExamQuestions(grammar: Grammar, pool: List<Grammar>): List<GeneratedQuizQuestion> {
        val japanesePattern = grammarJapanesePattern(grammar)
        val vietnameseMeaning = grammarVietnameseMeaning(grammar)
        if (japanesePattern.isBlank() || vietnameseMeaning.isBlank()) return emptyList()

        val others = pool.filter { it.id != grammar.id }
        val patternOptions = others.map { grammarJapanesePattern(it) }.filter { it.isNotBlank() }
        val meaningOptions = others.map { grammarVietnameseMeaning(it) }.filter { it.isNotBlank() }

        val meaningToPattern = Random.nextBoolean()
        return listOf(
            if (meaningToPattern) {
                GeneratedQuizQuestion(
                    category = QuizCategory.GRAMMAR,
                    promptType = QuizPromptType.GRAMMAR_MEANING_TO_PATTERN,
                    prompt = vietnameseMeaning,
                    options = fourOptions(japanesePattern, patternOptions),
                    correctAnswer = japanesePattern
                )
            } else {
                GeneratedQuizQuestion(
                    category = QuizCategory.GRAMMAR,
                    promptType = QuizPromptType.GRAMMAR_PATTERN_TO_MEANING,
                    prompt = japanesePattern,
                    options = fourOptions(vietnameseMeaning, meaningOptions),
                    correctAnswer = vietnameseMeaning
                )
            }
        )
    }

    /** Mẫu tiếng Nhật hiển thị trong đáp án (ưu tiên cấu trúc). */
    private fun grammarJapanesePattern(grammar: Grammar): String {
        val structure = grammar.structure.trim()
        if (structure.isNotBlank()) return structure
        val title = grammar.title.trim()
        if (title.isNotBlank() && looksJapaneseGrammar(title)) return title
        return structure
    }

    /** Nghĩa tiếng Việt ngắn gọn của mẫu (title nếu là tiếng Việt, không thì câu đầu explanation). */
    private fun grammarVietnameseMeaning(grammar: Grammar): String {
        val title = grammar.title.trim()
        if (title.isNotBlank() && !looksJapaneseGrammar(title)) {
            return shortenOption(title, maxLen = 96)
        }
        val explanation = grammar.explanation.trim()
        if (explanation.isBlank()) return ""
        val firstSentence = explanation
            .split(Regex("[.。!?\n]+"))
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return shortenOption(firstSentence.ifBlank { explanation }, maxLen = 96)
    }

    private fun looksJapaneseGrammar(text: String): Boolean {
        if (text.contains('～')) return true
        val japaneseMarkers = listOf("です", "ます", "ない", "は", "が", "を", "に", "で", "と", "の", "へ", "から", "より")
        if (japaneseMarkers.any { text.contains(it) }) return true
        return text.any { ch ->
            ch.code in 0x3040..0x309F || ch.code in 0x30A0..0x30FF || ch.code in 0x4E00..0x9FFF
        }
    }

    private fun buildKanjiMeaningQuestion(kanji: Kanji, pool: List<Kanji>): GeneratedQuizQuestion? {
        if (kanji.meaning.isBlank()) return null
        val meanings = pool.filter { it.id != kanji.id }.map { it.meaning }
        return GeneratedQuizQuestion(
            category = QuizCategory.KANJI,
            promptType = QuizPromptType.KANJI_MEANING,
            prompt = kanji.character,
            options = fourOptions(kanji.meaning, meanings),
            correctAnswer = kanji.meaning,
            audioText = bestReading(kanji)
        )
    }

    private fun shortenOption(text: String, maxLen: Int = 72): String {
        val t = text.trim().replace("\n", " ")
        return if (t.length <= maxLen) t else t.take(maxLen - 1) + "…"
    }

    private fun bestReading(kanji: Kanji): String {
        val kun = kanji.kunyomi.replace(".", "").replace("-", "").trim()
        if (kun.isNotBlank()) return kun
        val on = kanji.onyomi.replace(".", "").trim()
        if (on.isNotBlank()) return on.split("/").first().trim()
        return kanji.character
    }

    private fun fourOptions(correct: String, pool: List<String>): List<String> {
        val distractors = pool.filter { it != correct && it.isNotBlank() }.distinct().shuffled().take(3)
        return (listOf(correct) + distractors).distinct().let { base ->
            if (base.size < 4) base + List(4 - base.size) { "—" } else base
        }.shuffled().take(4)
    }
}

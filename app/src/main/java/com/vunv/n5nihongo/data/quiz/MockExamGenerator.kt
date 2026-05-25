package com.vunv.n5nihongo.data.quiz

import com.vunv.n5nihongo.data.model.Grammar
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Word
import kotlin.random.Random

class MockExamGenerator {

    /**
     * Sinh đề thi thử chuẩn JLPT N5 gồm 30 câu hỏi:
     * - 12 câu Từ vựng (Vocabulary)
     * - 10 câu Ngữ pháp (Grammar)
     * - 8 câu Chữ Hán (Kanji)
     */
    fun buildMockExam(
        allWords: List<Word>,
        allGrammars: List<Grammar>,
        allKanjis: List<Kanji>
    ): List<GeneratedQuizQuestion> {
        // 1. Sinh 12 câu hỏi Từ vựng
        val vocabPool = allWords.filter {
            it.meaning.isNotBlank() && (it.furigana.isNotBlank() || it.kanji.isNotBlank())
        }
        val vocabQuestions = vocabPool.shuffled()
            .take(12.coerceAtMost(vocabPool.size))
            .mapNotNull { buildVocabQuestion(it, vocabPool) }

        // 2. Sinh 10 câu hỏi Ngữ pháp
        val grammarPool = allGrammars.filter {
            it.structure.isNotBlank() && it.explanation.isNotBlank()
        }
        val grammarQuestions = grammarPool.shuffled()
            .take(10.coerceAtMost(grammarPool.size))
            .flatMap { buildGrammarQuestions(it, grammarPool) }
            .take(10)

        // 3. Sinh 8 câu hỏi Chữ Hán
        val kanjiPool = allKanjis.filter { it.character.isNotBlank() && it.meaning.isNotBlank() }
        val kanjiQuestions = kanjiPool.shuffled()
            .take(8.coerceAtMost(kanjiPool.size))
            .mapNotNull { buildKanjiQuestion(it, kanjiPool) }

        // Trộn chung toàn bộ đề thi
        return (vocabQuestions + grammarQuestions + kanjiQuestions).shuffled()
    }

    private fun buildVocabQuestion(word: Word, pool: List<Word>): GeneratedQuizQuestion? {
        if (word.meaning.isBlank()) return null
        val wordText = if (word.kanji.isNotBlank()) word.kanji else word.furigana
        if (wordText.isBlank()) return null

        val isWordToMeaning = Random.nextBoolean()

        return if (isWordToMeaning) {
            GeneratedQuizQuestion(
                category = QuizCategory.VOCABULARY,
                promptType = QuizPromptType.WORD_TO_MEANING,
                prompt = "Từ \"$wordText\" có nghĩa tiếng Việt là gì?",
                options = fourOptions(word.meaning, pool.map { it.meaning }),
                correctAnswer = word.meaning,
                audioText = word.furigana.ifBlank { word.kanji },
                word = word
            )
        } else {
            GeneratedQuizQuestion(
                category = QuizCategory.VOCABULARY,
                promptType = QuizPromptType.MEANING_TO_WORD,
                prompt = "Từ tiếng Nhật mang nghĩa \"${word.meaning}\" là gì?",
                options = fourOptions(wordText, pool.map { if (it.kanji.isNotBlank()) it.kanji else it.furigana }),
                correctAnswer = wordText,
                audioText = word.furigana.ifBlank { word.kanji },
                word = word
            )
        }
    }

    private fun buildGrammarQuestions(grammar: Grammar, pool: List<Grammar>): List<GeneratedQuizQuestion> {
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
                    prompt = "Mẫu câu ngữ pháp tiếng Nhật mang nghĩa \"$vietnameseMeaning\" là:",
                    options = fourOptions(japanesePattern, patternOptions),
                    correctAnswer = japanesePattern
                )
            } else {
                GeneratedQuizQuestion(
                    category = QuizCategory.GRAMMAR,
                    promptType = QuizPromptType.GRAMMAR_PATTERN_TO_MEANING,
                    prompt = "Mẫu câu ngữ pháp \"$japanesePattern\" có nghĩa tiếng Việt là:",
                    options = fourOptions(vietnameseMeaning, meaningOptions),
                    correctAnswer = vietnameseMeaning
                )
            }
        )
    }

    private fun buildKanjiQuestion(kanji: Kanji, pool: List<Kanji>): GeneratedQuizQuestion? {
        if (kanji.meaning.isBlank()) return null
        val isKanjiToMeaning = Random.nextBoolean()

        return if (isKanjiToMeaning) {
            val meanings = pool.filter { it.id != kanji.id }.map { it.meaning }
            GeneratedQuizQuestion(
                category = QuizCategory.KANJI,
                promptType = QuizPromptType.KANJI_MEANING,
                prompt = "Chữ Hán \"${kanji.character}\" mang ý nghĩa là gì?",
                options = fourOptions(kanji.meaning, meanings),
                correctAnswer = kanji.meaning,
                audioText = bestReading(kanji)
            )
        } else {
            val readings = pool.filter { it.id != kanji.id }.map { bestReading(it) }
            val correctAnswer = bestReading(kanji)
            GeneratedQuizQuestion(
                category = QuizCategory.KANJI,
                promptType = QuizPromptType.KANJI_READING,
                prompt = "Cách đọc Onyomi/Kunyomi tiêu biểu của chữ Hán \"${kanji.character}\" là:",
                options = fourOptions(correctAnswer, readings),
                correctAnswer = correctAnswer,
                audioText = correctAnswer
            )
        }
    }

    private fun grammarJapanesePattern(grammar: Grammar): String {
        val structure = grammar.structure.trim()
        if (structure.isNotBlank()) return structure
        val title = grammar.title.trim()
        if (title.isNotBlank()) return title
        return ""
    }

    private fun grammarVietnameseMeaning(grammar: Grammar): String {
        val title = grammar.title.trim()
        if (title.isNotBlank() && !looksJapanese(title)) {
            return shortenOption(title, maxLen = 80)
        }
        val explanation = grammar.explanation.trim()
        if (explanation.isBlank()) return ""
        val firstSentence = explanation
            .split(Regex("[.。!?\n]+"))
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return shortenOption(firstSentence.ifBlank { explanation }, maxLen = 80)
    }

    private fun looksJapanese(text: String): Boolean {
        if (text.contains('～')) return true
        return text.any { ch ->
            ch.code in 0x3040..0x309F || ch.code in 0x30A0..0x30FF || ch.code in 0x4E00..0x9FFF
        }
    }

    private fun bestReading(kanji: Kanji): String {
        val kun = kanji.kunyomi.replace(".", "").replace("-", "").trim()
        if (kun.isNotBlank()) return kun.split("/").first().trim()
        val on = kanji.onyomi.replace(".", "").trim()
        if (on.isNotBlank()) return on.split("/").first().trim()
        return kanji.character
    }

    private fun shortenOption(text: String, maxLen: Int = 72): String {
        val t = text.trim().replace("\n", " ")
        return if (t.length <= maxLen) t else t.take(maxLen - 1) + "…"
    }

    private fun fourOptions(correct: String, pool: List<String>): List<String> {
        val distractors = pool.filter { it != correct && it.isNotBlank() }.distinct().shuffled().take(3)
        return (listOf(correct) + distractors).distinct().let { base ->
            if (base.size < 4) base + List(4 - base.size) { "—" } else base
        }.shuffled().take(4)
    }
}

package com.vunv.n5nihongo.data.model



data class LessonDataJson(
    val lesson: LessonJson,
    val vocabulary: List<VocabularyJson>,
    val grammar: List<GrammarJson>,
    val kanji: List<KanjiJson>
)

data class LessonJson(
    val id: Int,
    val title: String,
    val description: String = ""
)

data class VocabularyJson(
    val wordId: Int = 0,
    val lessonId: Int = 0,
    val kanji: String = "",
    val furigana: String = "",
    val romaji: String = "",
    val meaning: String = "",
    val type: String = ""
)

data class GrammarJson(
    val grammarId: Int = 0,
    val lessonId: Int = 0,
    val title: String = "",
    val structure: String = "",
    val explanation: String = "",
    val examples: List<GrammarExampleJson> = emptyList()
)

data class GrammarExampleJson(
    val japanese: String = "",
    val vietnamese: String = ""
)

data class KanjiJson(
    val kanjiId: Int = 0,
    val lessonId: Int = 0,
    val character: String = "",
    val onyomi: String = "",
    val kunyomi: String = "",
    val meaning: String = "",
    val strokeCount: Int = 0
)

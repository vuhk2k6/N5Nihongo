package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["lessonId"]), Index(value = ["level"])]
)
data class Word(
    @PrimaryKey
    val id: Int,
    val lessonId: Int,
    val kanji: String,
    val furigana: String,
    val romaji: String = "",
    val meaning: String,
    val type: String,
    val level: String,
    val isFavorite: Boolean = false
)

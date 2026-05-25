package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kanji",
    indices = [Index(value = ["lessonId"])]
)
data class Kanji(
    @PrimaryKey
    val id: Int,
    val lessonId: Int,
    val character: String,
    val onyomi: String,
    val kunyomi: String,
    val meaning: String,
    val strokeCount: Int
)

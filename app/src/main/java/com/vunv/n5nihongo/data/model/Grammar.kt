package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grammar",
    indices = [Index(value = ["lessonId"])]
)
data class Grammar(
    @PrimaryKey
    val id: Int,
    val lessonId: Int,
    val title: String,
    val structure: String,
    val explanation: String,
    val examples: String // JSON string
)

data class Example(
    val japanese: String,
    val vietnamese: String
)

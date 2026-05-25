package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String = "",
    val orderIndex: Int
)

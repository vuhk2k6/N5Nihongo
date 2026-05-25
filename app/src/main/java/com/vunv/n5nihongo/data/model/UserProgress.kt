package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val lessonId: Int,
    val score: Int,
    val isCompleted: Boolean,
    val lastUpdated: Long
)

package com.vunv.n5nihongo.data.model

import androidx.room.Entity

@Entity(tableName = "user_progress", primaryKeys = ["userId", "lessonId"])
data class UserProgress(
    val userId: String = "",
    val lessonId: Int,
    val score: Int,
    val isCompleted: Boolean,
    val lastUpdated: Long
)

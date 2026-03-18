package com.vunv.n5nihongo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "n5_vocabulary")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerializedName("kanji")
    val kanji: String?,

    @SerializedName("hiragana")
    val hiragana: String,

    @SerializedName("romaji")
    val romaji: String,

    @SerializedName("meaning")
    val meaning: String,

    @SerializedName("lesson")
    val lesson: Int,

    // Các biến phục vụ logic học tập (không cần có trong JSON)
    var isMemorized: Boolean = false,
    var difficulty: Int = 0
)
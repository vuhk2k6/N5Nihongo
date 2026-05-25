package com.vunv.n5nihongo.ui.flashcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlashcardUiState(
    val currentWord: Word? = null,
    val remainingCount: Int = 0,
    val reviewedCount: Int = 0,
    val isCompleted: Boolean = false
)

class FlashcardViewModel(
    application: Application,
    private val lessonId: Int? = null,
    private val selectedWordIds: List<Int> = emptyList()
) : AndroidViewModel(application) {

    private val database = AppDatabaseProvider.getDatabase(application)
    private val wordDao = database.wordDao()
    private val reviewQueue = mutableListOf<Word>()
    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var reviewedCount = 0

    init {
        viewModelScope.launch {
            loadFlashcardsFromPreparedSet()
        }
    }

    private suspend fun loadFlashcardsFromPreparedSet() {


        val sourceWords = when {
            selectedWordIds.isNotEmpty() -> wordDao.getWordsByIds(selectedWordIds)
            lessonId != null -> wordDao.getWordsByLessonOnce(lessonId)
            else -> {
                val allWords = wordDao.getWordsByIds((1..6).toList())
                if (allWords.isEmpty()) {
                    seedSampleWords()
                    wordDao.getWordsByIds((1..6).toList())
                } else {
                    allWords
                }
            }
        }

        reviewQueue.clear()
        reviewedCount = 0

        if (sourceWords.isEmpty()) {
            updateUiState()
            return
        }

        reviewQueue.addAll(sourceWords.shuffled())
        updateUiState()
    }

    fun handleKnownWord() {
        val currentWord = _uiState.value.currentWord ?: return

        if (reviewQueue.isEmpty()) {
            return
        }

        reviewQueue.removeAt(0)
        reviewedCount += 1
        updateUiState()

        viewModelScope.launch {
            wordDao.updateLevelAndFavorite(
                wordId = currentWord.id,
                level = "mastered",
                isFavorite = true
            )
        }
    }

    fun handleUnknownWord() {
        if (reviewQueue.isEmpty()) {
            return
        }

        val currentWord = reviewQueue.removeAt(0)
        val insertIndex = minOf(5, reviewQueue.size)
        reviewQueue.add(insertIndex, currentWord)
        reviewedCount += 1
        updateUiState()
    }

    private fun updateUiState() {
        val current = reviewQueue.firstOrNull()
        _uiState.value = FlashcardUiState(
            currentWord = current,
            remainingCount = reviewQueue.size,
            reviewedCount = reviewedCount,
            isCompleted = current == null
        )
    }

    private suspend fun seedSampleWords() {
        wordDao.insertWords(
            listOf(
                Word(1, 1, "日", "にち", "", "Ngày, mặt trời", "kanji", "n5", false),
                Word(2, 1, "月", "げつ", "", "Tháng, mặt trăng", "kanji", "n5", false),
                Word(3, 1, "火", "か", "", "Lửa", "kanji", "n5", false),
                Word(4, 1, "水", "すい", "", "Nước", "kanji", "n5", false),
                Word(5, 1, "木", "もく", "", "Cây", "kanji", "n5", false),
                Word(6, 1, "金", "きん", "", "Vàng, tiền", "kanji", "n5", false)
            )
        )
    }

    companion object {
        fun factory(
            lessonId: Int? = null,
            selectedWordIds: List<Int> = emptyList()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return FlashcardViewModel(application, lessonId, selectedWordIds) as T
            }
        }
    }
}

package com.vunv.n5nihongo.ui.alphabet

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

data class AlphabetUiState(
    val words: List<Word> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

class AlphabetViewModel(
    application: Application,
    private val lessonId: Int
) : AndroidViewModel(application) {

    private val wordDao = AppDatabaseProvider.getDatabase(application).wordDao()
    
    private val _uiState = MutableStateFlow(AlphabetUiState())
    val uiState: StateFlow<AlphabetUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            wordDao.getWordsByLesson(lessonId).collect { words ->
                _uiState.value = _uiState.value.copy(
                    words = words,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSelection(wordId: Int) {
        val currentSelected = _uiState.value.selectedIds
        val newSelected = if (currentSelected.contains(wordId)) {
            currentSelected - wordId
        } else {
            currentSelected + wordId
        }
        _uiState.value = _uiState.value.copy(selectedIds = newSelected)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    companion object {
        fun factory(lessonId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return AlphabetViewModel(application, lessonId) as T
            }
        }
    }
}

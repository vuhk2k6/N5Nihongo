package com.vunv.n5nihongo.ui.kanji

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class KanjiDetailUiState(
    val isLoading: Boolean = true,
    val kanji: Kanji? = null,
    val examples: List<Word> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class KanjiDetailViewModel(
    application: Application,
    private val kanjiId: Int
) : AndroidViewModel(application) {

    private val db = AppDatabaseProvider.getDatabase(application)
    private val kanjiDao = db.kanjiDao()
    private val wordDao = db.wordDao()

    private val kanjiFlow = kanjiDao.getKanjiById(kanjiId)

    val uiState: StateFlow<KanjiDetailUiState> = kanjiFlow
        .flatMapLatest { kanji ->
            if (kanji == null) {
                kotlinx.coroutines.flow.flowOf(KanjiDetailUiState(isLoading = false, kanji = null))
            } else {
                wordDao.getWordsContainingChar(kanji.character).map { words ->
                    KanjiDetailUiState(isLoading = false, kanji = kanji, examples = words)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KanjiDetailUiState(isLoading = true)
        )

    companion object {
        fun factory(kanjiId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return KanjiDetailViewModel(application, kanjiId) as T
            }
        }
    }
}

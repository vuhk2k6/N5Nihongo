package com.vunv.n5nihongo.ui.kanji

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Kanji
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class KanjiListUiState(
    val kanjiList: List<Kanji> = emptyList(),
    val isLoading: Boolean = true
)

class KanjiListViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val kanjiDao = AppDatabaseProvider.getDatabase(application).kanjiDao()

    private val _uiState = MutableStateFlow(KanjiListUiState())
    val uiState: StateFlow<KanjiListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            kanjiDao.getAllKanji().collectLatest { list ->
                _uiState.value = KanjiListUiState(
                    kanjiList = list,
                    isLoading = false
                )
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return KanjiListViewModel(application) as T
            }
        }
    }
}

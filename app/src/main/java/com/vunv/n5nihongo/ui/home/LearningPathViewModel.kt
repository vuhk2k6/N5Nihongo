package com.vunv.n5nihongo.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Lesson
import com.vunv.n5nihongo.data.model.UserProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonUiModel(
    val lessonId: Int,
    val lessonTitle: String,
    val progress: Float,
    val isCompleted: Boolean,
    val isEnabled: Boolean
)

class LearningPathViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabaseProvider.getDatabase(application)
    private val lessonDao = database.lessonDao()
    private val userProgressDao = database.userProgressDao()
    private val dataRepository = com.vunv.n5nihongo.data.repository.DataRepository(
        context = application,
        lessonDao = lessonDao,
        wordDao = database.wordDao(),
        grammarDao = database.grammarDao(),
        kanjiDao = database.kanjiDao()
    )

    val lessonsUiState: StateFlow<List<LessonUiModel>> = combine(
        lessonDao.getAllLessons(),
        userProgressDao.getAllProgress()
    ) { lessons, progressList ->
        lessons.toUiModels(progressList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        seedLessonsIfEmpty()
    }

    private fun seedLessonsIfEmpty() {
        viewModelScope.launch {
            dataRepository.seedDatabase()
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return LearningPathViewModel(application) as T
            }
        }
    }
}

private fun List<Lesson>.toUiModels(progressList: List<UserProgress>): List<LessonUiModel> {
    val progressByLessonId = progressList.associateBy { it.lessonId }
    var previousCompleted = true // First lesson is always enabled
    
    return this.sortedBy { it.orderIndex }.map { lesson ->
        val progressData = progressByLessonId[lesson.id]
        val clampedScore = (progressData?.score ?: 0).coerceIn(0, 100)
        val isCompleted = progressData?.isCompleted == true
        
        // Temporarily unlock all lessons for development/testing
        val isEnabled = true 
        previousCompleted = isCompleted
        
        LessonUiModel(
            lessonId = lesson.id,
            lessonTitle = lesson.title,
            progress = clampedScore / 100f,
            isCompleted = isCompleted,
            isEnabled = isEnabled
        )
    }
}

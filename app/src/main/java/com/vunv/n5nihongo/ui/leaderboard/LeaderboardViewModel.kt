package com.vunv.n5nihongo.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vunv.n5nihongo.data.auth.AuthRepository
import com.vunv.n5nihongo.data.auth.UserDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val topUsers: List<UserDocument> = emptyList(),
    val currentUserDoc: UserDocument? = null,
    val currentUserRank: Int = -1,
    val errorMessage: String? = null
)

class LeaderboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // 1. Fetch real online users from the Firestore database
                val querySnapshot = firestore.collection("users")
                    .get()
                    .await()
                
                val allUsers = querySnapshot.toObjects(UserDocument::class.java).toMutableList()

                // 2. Fetch current user info (online or local guest)
                val currentUser = authRepository.getCurrentUser()
                var currentUserDoc: UserDocument? = null
                var currentUserRank = -1

                if (currentUser != null) {
                    val userDocResult = authRepository.getUserDocument(currentUser.uid)
                    currentUserDoc = userDocResult.getOrNull()
                } else if (authRepository.isGuestMode(getApplication())) {
                    currentUserDoc = authRepository.getGuestUserDocument(getApplication())
                }

                // Add current user to list if not already present in the database records
                if (currentUserDoc != null && allUsers.none { it.uid == currentUserDoc.uid }) {
                    allUsers.add(currentUserDoc)
                }

                // Sort in memory to guarantee 100% inclusion of everyone from the database
                val sortedUsers = allUsers.sortedByDescending { it.totalXp }
                val topUsers = sortedUsers.take(20)

                if (currentUserDoc != null) {
                    // Calculate exact rank from the complete sorted list
                    val existingIndex = sortedUsers.indexOfFirst { it.uid == currentUserDoc.uid }
                    currentUserRank = if (existingIndex != -1) {
                        existingIndex + 1
                    } else {
                        sortedUsers.count { it.totalXp > currentUserDoc.totalXp } + 1
                    }
                }

                _uiState.value = LeaderboardUiState(
                    isLoading = false,
                    topUsers = topUsers,
                    currentUserDoc = currentUserDoc,
                    currentUserRank = currentUserRank
                )
            } catch (e: Exception) {
                _uiState.value = LeaderboardUiState(
                    isLoading = false,
                    errorMessage = "Không thể tải bảng xếp hạng: ${e.localizedMessage ?: "Lỗi không xác định"}"
                )
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return LeaderboardViewModel(application) as T
            }
        }
    }
}

package com.vunv.n5nihongo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.vunv.n5nihongo.data.auth.AuthRepository
import com.vunv.n5nihongo.data.auth.UserDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val displayNameInput: String = "",
    val currentUser: FirebaseUser? = null,
    val userDocument: UserDocument? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayNameInput = value, errorMessage = null) }
    }

    fun showAuthMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null, isLoading = false) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isLoginMode = !it.isLoginMode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun checkCurrentUser(context: android.content.Context? = null) {

        if (context != null && authRepository.isGuestMode(context)) {
            val guestDoc = authRepository.getGuestUserDocument(context)
            _uiState.update {
                it.copy(
                    currentUser = null,
                    userDocument = guestDoc,
                    isLoading = false
                )
            }
        } else {
            val current = authRepository.getCurrentUser()
            _uiState.update { it.copy(currentUser = current) }
            if (current != null) {
                refreshUserProfile(current)
            }
        }
    }

    fun submitAuth() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email và mật khẩu không được để trống") }
            return
        }
        if (!state.isLoginMode && state.displayNameInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên hiển thị") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = if (state.isLoginMode) {
                authRepository.loginWithEmailPassword(state.email.trim(), state.password)
            } else {
                authRepository.registerWithEmailPassword(
                    email = state.email.trim(),
                    password = state.password,
                    displayName = state.displayNameInput.trim()
                )
            }

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            email = "",
                            password = "",
                            displayNameInput = "",
                            successMessage = if (state.isLoginMode) "Đăng nhập thành công" else "Đăng ký thành công"
                        )
                    }
                    refreshUserProfile(user)
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Xác thực thất bại"
                        )
                    }
                }
            )
        }
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.loginWithGoogleIdToken(idToken).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            successMessage = "Đăng nhập Google thành công"
                        )
                    }
                    refreshUserProfile(user)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Đăng nhập Google thất bại") }
                }
            )
        }
    }

    fun loginWithFacebookToken(accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.loginWithFacebookAccessToken(accessToken).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            successMessage = "Đăng nhập Facebook thành công"
                        )
                    }
                    refreshUserProfile(user)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Đăng nhập Facebook thất bại") }
                }
            )
        }
    }

    fun logout(context: android.content.Context? = null) {
        authRepository.signOut()
        if (context != null) {
            authRepository.clearGuestMode(context)
        }
        _uiState.update {
            it.copy(
                currentUser = null,
                userDocument = null,
                successMessage = "Đã đăng xuất"
            )
        }
    }

    fun loginAsGuest(context: android.content.Context, nickname: String) {
        if (nickname.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên khách") }
            return
        }
        authRepository.saveGuestNickname(context, nickname.trim())
        val guestDoc = authRepository.getGuestUserDocument(context)
        _uiState.update {
            it.copy(
                currentUser = null,
                userDocument = guestDoc,
                successMessage = "Đăng nhập khách thành công"
            )
        }
    }

    private fun refreshUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            authRepository.getUserDocument(user.uid).fold(
                onSuccess = { document ->
                    if (document != null) {
                        _uiState.update { it.copy(userDocument = document) }
                        return@launch
                    }
                    authRepository.syncUserProfile(user, user.authProviderLabel()).fold(
                        onSuccess = { synced ->
                            _uiState.update { it.copy(userDocument = synced) }
                        },
                        onFailure = { throwable ->
                            _uiState.update {
                                it.copy(errorMessage = throwable.message ?: "Không lưu được hồ sơ người dùng")
                            }
                        }
                    )
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message ?: "Không tải được hồ sơ") }
                }
            )
        }
    }

    private fun FirebaseUser.authProviderLabel(): String {
        val providerId = providerData.firstOrNull()?.providerId.orEmpty()
        return when {
            providerId.contains("google", ignoreCase = true) -> AuthRepository.AUTH_PROVIDER_GOOGLE
            providerId.contains("facebook", ignoreCase = true) -> AuthRepository.AUTH_PROVIDER_FACEBOOK
            else -> AuthRepository.AUTH_PROVIDER_EMAIL
        }
    }

    fun getSavedGuestNickname(context: android.content.Context): String? {
        return authRepository.getGuestUserDocument(context)?.displayName
    }
}

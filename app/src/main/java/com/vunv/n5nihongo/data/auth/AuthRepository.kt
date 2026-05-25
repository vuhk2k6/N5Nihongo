package com.vunv.n5nihongo.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserDocument(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val authProvider: String = "",
    val totalXp: Int = 0,
    val streak: Int = 0
)

class AuthRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {

    suspend fun loginWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email/password must not be blank"))
        }
        val auth = getAuthOrNull()
            ?: return Result.failure(IllegalStateException("Firebase chưa được khởi tạo"))

        return runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw IllegalStateException("Login succeeded but user is null")
            syncUserProfile(user, AUTH_PROVIDER_EMAIL)
            user
        }.mapError()
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google ID token không hợp lệ"))
        }
        val auth = getAuthOrNull()
            ?: return Result.failure(IllegalStateException("Firebase chưa được khởi tạo"))
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Đăng nhập Google thất bại")
            syncUserProfile(user, AUTH_PROVIDER_GOOGLE)
            user
        }.mapError()
    }

    suspend fun loginWithFacebookAccessToken(accessToken: String): Result<FirebaseUser> {
        if (accessToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Facebook access token không hợp lệ"))
        }
        val auth = getAuthOrNull()
            ?: return Result.failure(IllegalStateException("Firebase chưa được khởi tạo"))
        return runCatching {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Đăng nhập Facebook thất bại")
            syncUserProfile(user, AUTH_PROVIDER_FACEBOOK)
            user
        }.mapError()
    }

    suspend fun registerWithEmailPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Email/password/displayName must not be blank")
            )
        }
        val auth = getAuthOrNull()
            ?: return Result.failure(IllegalStateException("Firebase chưa được khởi tạo"))
        val firestore = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firestore chưa được khởi tạo"))

        return runCatching {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: throw IllegalStateException("Registration succeeded but user is null")

            val profileRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileRequest).await()

            val userDocument = UserDocument(
                uid = user.uid,
                displayName = displayName,
                email = email.trim(),
                photoUrl = user.photoUrl?.toString().orEmpty(),
                authProvider = AUTH_PROVIDER_EMAIL,
                totalXp = 0,
                streak = 0
            )

            // Không làm fail toàn bộ đăng ký nếu bước sync Firestore lỗi tạm thời.
            runCatching {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(userDocument)
                    .await()
            }

            user
        }.mapError()
    }

    suspend fun updateUserProgress(xpToAdd: Int): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(IllegalStateException("User not logged in"))
        val firestore = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firestore chưa được khởi tạo"))

        return runCatching {
            val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentXp = snapshot.getLong("totalXp")?.toInt() ?: 0
                
                transaction.update(docRef, "totalXp", currentXp + xpToAdd)
                // Basic streak logic: just increment for now, or you could add date checks
                // For now let's just keep it simple as requested
            }.await()
            Unit
        }
    }

    fun getCurrentUser(): FirebaseUser? = getAuthOrNull()?.currentUser

    fun isUserLoggedIn(): Boolean = getAuthOrNull()?.currentUser != null

    /**
     * Creates or updates `users/{uid}` from [FirebaseUser] after any sign-in.
     * Preserves existing XP/streak; fills missing display name, email, and photo.
     */
    suspend fun syncUserProfile(user: FirebaseUser, authProvider: String): Result<UserDocument> {
        val firestore = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firestore chưa được khởi tạo"))

        return runCatching {
            val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
            val snapshot = docRef.get().await()
            val resolvedName = resolveDisplayName(user)
            val email = user.email.orEmpty()
            val photoUrl = user.photoUrl?.toString().orEmpty()

            if (!snapshot.exists()) {
                val created = UserDocument(
                    uid = user.uid,
                    displayName = resolvedName,
                    email = email,
                    photoUrl = photoUrl,
                    authProvider = authProvider,
                    totalXp = 0,
                    streak = 0
                )
                docRef.set(created).await()
                created
            } else {
                val existing = snapshot.toObject(UserDocument::class.java)
                    ?: UserDocument(uid = user.uid)
                val updates = linkedMapOf<String, Any>()
                if (existing.displayName.isBlank() && resolvedName.isNotBlank()) {
                    updates["displayName"] = resolvedName
                }
                if (existing.email.isBlank() && email.isNotBlank()) {
                    updates["email"] = email
                }
                if (existing.photoUrl.isBlank() && photoUrl.isNotBlank()) {
                    updates["photoUrl"] = photoUrl
                }
                if (existing.authProvider.isBlank() && authProvider.isNotBlank()) {
                    updates["authProvider"] = authProvider
                }
                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                }
                docRef.get().await().toObject(UserDocument::class.java) ?: existing
            }
        }
    }

    suspend fun getUserDocument(uid: String): Result<UserDocument?> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("Uid must not be blank"))
        }
        val firestore = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firestore chưa được khởi tạo"))

        return runCatching {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
                .toObject(UserDocument::class.java)
        }
    }

    fun signOut() {
        getAuthOrNull()?.signOut()
        runCatching { com.facebook.login.LoginManager.getInstance().logOut() }
    }

    private fun resolveDisplayName(user: FirebaseUser): String {
        return user.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: user.email?.substringBefore("@")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Bạn học Nihongo"
    }

    private fun getAuthOrNull(): FirebaseAuth? = runCatching { authProvider() }.getOrNull()

    private fun getFirestoreOrNull(): FirebaseFirestore? = runCatching { firestoreProvider() }.getOrNull()

    private fun <T> Result<T>.mapError(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            Result.failure(IllegalStateException(getReadableErrorMessage(throwable)))
        }
    )

    private fun getReadableErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseNetworkException -> "Mạng không ổn định. Vui lòng kiểm tra kết nối Internet."
            is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký."
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không hợp lệ."
            is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa."
            is FirebaseAuthException -> when (throwable.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "Mật khẩu quá yếu, vui lòng dùng mật khẩu mạnh hơn."
                "ERROR_INVALID_EMAIL" -> "Email không đúng định dạng."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Phương thức đăng nhập này chưa được bật trong Firebase."
                "ERROR_CONFIGURATION_NOT_FOUND" -> "Firebase Authentication chưa cấu hình đúng. Kiểm tra lại dự án Firebase."
                else -> throwable.localizedMessage ?: "Đã có lỗi nội bộ khi xác thực."
            }
            else -> throwable.localizedMessage ?: "Đã có lỗi nội bộ khi xác thực."
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        const val AUTH_PROVIDER_EMAIL = "email"
        const val AUTH_PROVIDER_GOOGLE = "google"
        const val AUTH_PROVIDER_FACEBOOK = "facebook"
    }
}

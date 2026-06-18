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
    val streak: Int = 0,
    val lastActiveDate: Long = 0L
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

    fun isGuestMode(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("nihongo_guest_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("guest_active", false)
    }

    fun getGuestUserDocument(context: android.content.Context): UserDocument? {
        val prefs = context.getSharedPreferences("nihongo_guest_prefs", android.content.Context.MODE_PRIVATE)
        val name = prefs.getString("guest_nickname", null) ?: return null
        var uid = prefs.getString("guest_uid", null)
        if (uid == null) {
            uid = "GUEST_" + java.util.UUID.randomUUID().toString()
            prefs.edit().putString("guest_uid", uid).apply()
        }
        val xp = prefs.getInt("guest_xp", 0)
        val streak = prefs.getInt("guest_streak", 0)
        val lastActive = prefs.getLong("guest_last_active", 0L)
        return UserDocument(
            uid = uid,
            displayName = name,
            email = "local_guest@nihongo.local",
            photoUrl = "",
            authProvider = "local_guest",
            totalXp = xp,
            streak = streak,
            lastActiveDate = lastActive
        )
    }

    fun saveGuestNickname(context: android.content.Context, nickname: String) {
        val prefs = context.getSharedPreferences("nihongo_guest_prefs", android.content.Context.MODE_PRIVATE)
        var uid = prefs.getString("guest_uid", null)
        if (uid == null) {
            uid = "GUEST_" + java.util.UUID.randomUUID().toString()
        }
        prefs.edit()
            .putString("guest_nickname", nickname)
            .putString("guest_uid", uid)
            .putBoolean("guest_active", true)
            .apply()

        // Also asynchronously save/sync guest profile to Firebase Firestore users database
        val guestDoc = getGuestUserDocument(context)
        if (guestDoc != null) {
            val firestore = getFirestoreOrNull()
            firestore?.collection(USERS_COLLECTION)?.document(guestDoc.uid)?.set(guestDoc)
        }
    }

    fun clearGuestMode(context: android.content.Context) {
        val prefs = context.getSharedPreferences("nihongo_guest_prefs", android.content.Context.MODE_PRIVATE)
        // Set guest_active to false so session is inactive on logout, but keep all progress & data!
        prefs.edit().putBoolean("guest_active", false).apply()
    }

    suspend fun updateUserProgress(xpToAdd: Int, context: android.content.Context? = null): Result<Unit> {
        if (context != null && isGuestMode(context)) {
            val prefs = context.getSharedPreferences("nihongo_guest_prefs", android.content.Context.MODE_PRIVATE)
            val currentXp = prefs.getInt("guest_xp", 0)
            val currentStreak = prefs.getInt("guest_streak", 0)
            val lastActive = prefs.getLong("guest_last_active", 0L)
            
            val now = System.currentTimeMillis()
            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date(now))
            val lastStr = if (lastActive > 0) sdf.format(java.util.Date(lastActive)) else ""
            
            val yesterdayCal = java.util.Calendar.getInstance()
            yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(yesterdayCal.time)
            
            val newStreak = when (lastStr) {
                todayStr -> currentStreak
                yesterdayStr -> currentStreak + 1
                else -> 1
            }
            
            prefs.edit()
                .putInt("guest_xp", currentXp + xpToAdd)
                .putInt("guest_streak", newStreak)
                .putLong("guest_last_active", now)
                .apply()

            // Asynchronously sync the updated guest progress to the online database
            val guestDoc = getGuestUserDocument(context)
            if (guestDoc != null) {
                val firestore = getFirestoreOrNull()
                firestore?.collection(USERS_COLLECTION)?.document(guestDoc.uid)?.set(guestDoc)
            }
            return Result.success(Unit)
        }

        val user = getCurrentUser() ?: return Result.failure(IllegalStateException("User not logged in"))
        val firestore = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firestore chưa được khởi tạo"))

        return runCatching {
            val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val now = System.currentTimeMillis()
                
                // Formatted date check in local timezone
                val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                val todayStr = sdf.format(java.util.Date(now))
                
                if (!snapshot.exists()) {
                    val resolvedName = resolveDisplayName(user)
                    val email = user.email.orEmpty()
                    val photoUrl = user.photoUrl?.toString().orEmpty()
                    
                    val created = UserDocument(
                        uid = user.uid,
                        displayName = resolvedName,
                        email = email,
                        photoUrl = photoUrl,
                        authProvider = user.authProviderLabel(),
                        totalXp = xpToAdd,
                        streak = 1,
                        lastActiveDate = now
                    )
                    transaction.set(docRef, created)
                } else {
                    val currentXp = snapshot.getLong("totalXp")?.toInt() ?: 0
                    val currentStreak = snapshot.getLong("streak")?.toInt() ?: 0
                    val lastActive = snapshot.getLong("lastActiveDate") ?: 0L
                    val lastStr = if (lastActive > 0) sdf.format(java.util.Date(lastActive)) else ""
                    
                    val yesterdayCal = java.util.Calendar.getInstance()
                    yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdf.format(yesterdayCal.time)
                    
                    val newStreak = when (lastStr) {
                        todayStr -> currentStreak // Already practiced today, keep streak
                        yesterdayStr -> currentStreak + 1 // Practiced yesterday, increment
                        else -> 1 // Gap or first time, reset/start with 1
                    }
                    
                    transaction.update(docRef, "totalXp", currentXp + xpToAdd)
                    transaction.update(docRef, "streak", newStreak)
                    transaction.update(docRef, "lastActiveDate", now)
                }
            }.await()
            Unit
        }
    }

    private fun FirebaseUser.authProviderLabel(): String {
        val providerId = providerData.firstOrNull()?.providerId.orEmpty()
        return when {
            providerId.contains("google", ignoreCase = true) -> AUTH_PROVIDER_GOOGLE
            providerId.contains("facebook", ignoreCase = true) -> AUTH_PROVIDER_FACEBOOK
            else -> AUTH_PROVIDER_EMAIL
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

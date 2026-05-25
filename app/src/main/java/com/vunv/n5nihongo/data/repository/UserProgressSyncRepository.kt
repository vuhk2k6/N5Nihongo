package com.vunv.n5nihongo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.vunv.n5nihongo.data.local.UserProgressDao
import com.vunv.n5nihongo.data.model.UserProgress
import kotlinx.coroutines.tasks.await

class UserProgressSyncRepository(
    private val userProgressDao: UserProgressDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun syncProgressForUser(userId: String) {
        val localProgress = userProgressDao.getAllProgressOnce()
        for (localItem in localProgress) {
            syncSingleProgress(userId = userId, localItem = localItem)
        }
    }

    private suspend fun syncSingleProgress(userId: String, localItem: UserProgress) {
        val docId = "${userId}_${localItem.lessonId}"
        val docRef = firestore.collection(USERS_PROGRESS_COLLECTION).document(docId)
        val remoteSnapshot = docRef.get().await()
        val remote = remoteSnapshot.toObject(RemoteUserProgress::class.java)

        if (remote == null || localItem.lastUpdated >= remote.lastUpdated) {
            docRef.set(
                RemoteUserProgress(
                    uid = userId,
                    lessonId = localItem.lessonId,
                    score = localItem.score,
                    isCompleted = localItem.isCompleted,
                    lastUpdated = localItem.lastUpdated
                )
            ).await()
            return
        }

        userProgressDao.upsertProgress(
            UserProgress(
                lessonId = remote.lessonId,
                score = remote.score,
                isCompleted = remote.isCompleted,
                lastUpdated = remote.lastUpdated
            )
        )
    }

    data class RemoteUserProgress(
        val uid: String = "",
        val lessonId: Int = 0,
        val score: Int = 0,
        val isCompleted: Boolean = false,
        val lastUpdated: Long = 0L
    )

    companion object {
        private const val USERS_PROGRESS_COLLECTION = "users_progress"
    }
}

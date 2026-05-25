package com.vunv.n5nihongo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.repository.UserProgressSyncRepository

class UserProgressSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return runCatching {
            val database = AppDatabaseProvider.getDatabase(applicationContext)
            val repository = UserProgressSyncRepository(database.userProgressDao())
            repository.syncProgressForUser(uid)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

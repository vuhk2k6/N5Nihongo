package com.vunv.n5nihongo.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vunv.n5nihongo.MainActivity
import com.vunv.n5nihongo.data.auth.AuthRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val authRepository = AuthRepository()
        val currentUser = authRepository.getCurrentUser()

        var hasLearnedToday = false
        var currentStreak = 0

        if (currentUser != null) {
            val userDocResult = authRepository.getUserDocument(currentUser.uid)
            val userDoc = userDocResult.getOrNull()
            if (userDoc != null) {
                currentStreak = userDoc.streak
                
                val now = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val todayStr = sdf.format(Date(now))
                val lastActiveStr = if (userDoc.lastActiveDate > 0L) sdf.format(Date(userDoc.lastActiveDate)) else ""
                
                if (todayStr == lastActiveStr) {
                    hasLearnedToday = true
                }
            }
        } else if (authRepository.isGuestMode(applicationContext)) {
            val userDoc = authRepository.getGuestUserDocument(applicationContext)
            if (userDoc != null) {
                currentStreak = userDoc.streak
                
                val now = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val todayStr = sdf.format(Date(now))
                val lastActiveStr = if (userDoc.lastActiveDate > 0L) sdf.format(Date(userDoc.lastActiveDate)) else ""
                
                if (todayStr == lastActiveStr) {
                    hasLearnedToday = true
                }
            }
        }

        // Only send notification if user hasn't completed practice today
        if (!hasLearnedToday) {
            sendNotification(currentStreak)
        }

        return Result.success()
    }

    private fun sendNotification(streak: Int) {
        val channelId = "daily_study_reminder"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở học tập hàng ngày",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh gửi thông báo nhắc nhở học tiếng Nhật hàng ngày"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Đến giờ học tiếng Nhật rồi! 🇯🇵"
        val message = if (streak > 0) {
            "Duy trì chuỗi học tập 🔥 $streak ngày của bạn ngay hôm nay. Chỉ cần 5 phút luyện tập thôi!"
        } else {
            "Bắt đầu ngay hôm nay để tạo thói quen học tập hàng ngày nhé. Đề thi thử đang chờ bạn! 📚"
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(2026, notification)
    }
}

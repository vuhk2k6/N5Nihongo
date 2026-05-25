package com.vunv.n5nihongo.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vunv.n5nihongo.R
import java.time.LocalDateTime

class TimeReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val now = LocalDateTime.now()
        val day = now.dayOfMonth
        val message = buildMessage(now.hour, now.minute, day)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("N5 Nihongo")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(
                now.minute + day * 100,
                notification
            )
        }
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nhắc nhở thời gian tiếng Nhật",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Nhắc theo giờ và ngày đặc biệt"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildMessage(hour: Int, minute: Int, day: Int): String {
        val timeLine = "いま ${hour}じ ${minute}ふん です。"
        val special = when (day) {
            1 -> "Hôm nay là mùng 1: ついたち."
            20 -> "Hôm nay là ngày 20: はつか."
            else -> ""
        }
        return if (special.isBlank()) timeLine else "$timeLine $special"
    }

    companion object {
        const val CHANNEL_ID = "time_reminder_channel"
    }
}


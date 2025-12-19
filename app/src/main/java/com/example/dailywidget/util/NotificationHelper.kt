package com.example.dailywidget.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailywidget.R
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DataStoreManager
import com.example.dailywidget.ui.activity.MainActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 알림 헬퍼
 * - 채널 생성
 * - 일일 문장 개수 알림 표시
 */
object NotificationHelper {
    private const val CHANNEL_ID = "daily_sentence_notification"
    private const val CHANNEL_NAME = "일일 문장 알림"
    private const val CHANNEL_DESCRIPTION = "오늘의 문장 개수를 알려줍니다"
    private const val NOTIFICATION_ID = 1001

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 오늘의 문장 개수 알림 표시
     */
    suspend fun showDailySentenceNotification(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dailySentenceDao()
        val dataStoreManager = DataStoreManager(context)

        // 오늘 날짜
        val today = SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())
        val allSentences = dao.getSentencesByDate(today)

        // 장르별 개수 계산
        val genreCounts = mutableMapOf<String, Int>()
        val allGenres = dataStoreManager.getAllGenres()

        allGenres.forEach { genre ->
            val count = allSentences.count {
                it.genre.equals(genre.id, ignoreCase = true)
            }
            if (count > 0) {
                genreCounts[genre.displayName] = count
            }
        }

        // 알림 내용 생성
        val totalCount = allSentences.size
        val contentText = if (genreCounts.isEmpty()) {
            "오늘의 문장이 없습니다"
        } else {
            genreCounts.entries.joinToString(", ") { "${it.key} ${it.value}개" }
        }

        // 알림 클릭 시 앱 열기
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_menu_book_24)
            .setContentTitle("오늘의 문장 (총 ${totalCount}개)")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 알림 표시
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, notification)
        }
    }
}
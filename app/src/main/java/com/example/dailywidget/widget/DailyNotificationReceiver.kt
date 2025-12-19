package com.example.dailywidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dailywidget.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * 일일 알림 Receiver
 * - 설정한 시간에 오늘의 문장 개수 알림
 * - 재부팅 시 알람 재설정
 */
class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_DAILY_NOTIFICATION = "com.example.dailywidget.ACTION_DAILY_NOTIFICATION"

        /**
         * 일일 알림 스케줄링
         * @param hourOfDay 시 (0-23)
         * @param minute 분 (0-59)
         */
        fun scheduleNotification(context: Context, hourOfDay: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
                action = ACTION_DAILY_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000, // 위젯 업데이트(0)와 다른 requestCode
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 다음 알림 시간 계산
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // 이미 지난 시간이면 다음날로
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Android 버전별 알람 설정
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        /**
         * 알림 취소
         */
        fun cancelNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
                action = ACTION_DAILY_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY_NOTIFICATION -> {
                // 알림 표시
                CoroutineScope(Dispatchers.IO).launch {
                    NotificationHelper.showDailySentenceNotification(context)
                }

                // 다음날 알림 재설정
                CoroutineScope(Dispatchers.IO).launch {
                    val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)
                    val config = dataStoreManager.getNotificationConfig()
                    if (config.enabled) {
                        scheduleNotification(context, config.hour, config.minute)
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 재부팅 후 알림 재설정
                CoroutineScope(Dispatchers.IO).launch {
                    val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(context)
                    val config = dataStoreManager.getNotificationConfig()
                    if (config.enabled) {
                        scheduleNotification(context, config.hour, config.minute)
                    }
                }
            }
        }
    }
}
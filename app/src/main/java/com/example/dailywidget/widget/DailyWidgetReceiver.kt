package com.example.dailywidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

/**
 * 자정에 위젯 업데이트를 위한 Receiver
 */
class DailyWidgetReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_MIDNIGHT_UPDATE = "com.example.dailywidget.ACTION_MIDNIGHT_UPDATE"

        // ⭐ 자정 알람 설정
        fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyWidgetReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 다음 자정 계산
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 자정마다 반복
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            ACTION_MIDNIGHT_UPDATE -> {
                // 위젯 업데이트
                DailyWidgetProvider.updateAllWidgets(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 재부팅 후 알람 재설정
                scheduleMidnightUpdate(context)
            }
        }
    }
}
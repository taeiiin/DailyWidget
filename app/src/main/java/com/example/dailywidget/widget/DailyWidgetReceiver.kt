package com.example.dailywidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

/**
 * 자정 위젯 업데이트 Receiver
 * - 매일 자정 00:00에 위젯 업데이트
 * - 날짜 변경, 재부팅, 앱 업데이트 시 알람 재설정
 * - Android 버전별 알람 API 대응 (API 23+, 31+)
 */
class DailyWidgetReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyWidgetReceiver"
        private const val ACTION_MIDNIGHT_UPDATE = "com.example.dailywidget.ACTION_MIDNIGHT_UPDATE"

        /**
         * 자정 알람 설정 (정확한 시간)
         * Android 버전별로 적절한 알람 API 사용
         */
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

                // 이미 자정이 지났으면 다음날 자정
                if (get(Calendar.HOUR_OF_DAY) >= 0 && get(Calendar.MINUTE) >= 1) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Android 버전별 알람 설정
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ (API 31+)
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        // 권한 없으면 근사치 알람
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6+ (API 23+)
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Android 5 이하
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // 권한 없으면 근사치 알람
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        /**
         * 알람 취소
         */
        fun cancelMidnightUpdate(context: Context) {
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
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            ACTION_MIDNIGHT_UPDATE -> {
                // 위젯 업데이트
                DailyWidgetProvider.updateAllWidgets(context)

                // 다음 자정 알람 재설정
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_DATE_CHANGED -> {
                DailyWidgetProvider.updateAllWidgets(context)
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 재부팅 후 알람 재설정
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 앱 업데이트 후 알람 재설정
                scheduleMidnightUpdate(context)
            }
        }
    }
}
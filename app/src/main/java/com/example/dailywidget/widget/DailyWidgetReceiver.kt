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
 * 자정에 위젯 업데이트를 위한 Receiver
 */
class DailyWidgetReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyWidgetReceiver"
        private const val ACTION_MIDNIGHT_UPDATE = "com.example.dailywidget.ACTION_MIDNIGHT_UPDATE"

        /**
         * ⭐ 자정 알람 설정 (정확한 시간)
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

                // ⭐ 이미 자정이 지났으면 다음날 자정
                if (get(Calendar.HOUR_OF_DAY) >= 0 && get(Calendar.MINUTE) >= 1) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            Log.d(TAG, "Scheduling midnight update at: ${calendar.time}")

            // ⭐ Android 버전별 알람 설정
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ (API 31+)
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Exact alarm scheduled (Android 12+)")
                    } else {
                        // 권한 없으면 근사치 알람
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.w(TAG, "Exact alarm permission denied, using approximate")
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6+ (API 23+)
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Exact alarm scheduled (Android 6+)")
                } else {
                    // Android 5 이하
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Exact alarm scheduled (Android 5)")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to schedule alarm: ${e.message}")
                // 권한 없으면 근사치 알람
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        /**
         * ⭐ 알람 취소
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
            Log.d(TAG, "Midnight update cancelled")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_MIDNIGHT_UPDATE -> {
                Log.d(TAG, "Midnight update triggered")
                // 위젯 업데이트
                DailyWidgetProvider.updateAllWidgets(context)

                // ⭐ 다음 자정 알람 재설정
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_DATE_CHANGED -> {
                Log.d(TAG, "Date changed")
                DailyWidgetProvider.updateAllWidgets(context)
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed")
                // 재부팅 후 알람 재설정
                scheduleMidnightUpdate(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App updated")
                // 앱 업데이트 후 알람 재설정
                scheduleMidnightUpdate(context)
            }
        }
    }
}
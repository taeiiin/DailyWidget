package com.example.dailywidget

import android.app.Application
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.util.NotificationHelper
import com.example.dailywidget.widget.DailyWidgetReceiver
import kotlinx.coroutines.launch

class MyApp : Application() {

    // 데이터베이스 인스턴스 (lazy 초기화)
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    // Repository 인스턴스 (lazy 초기화)
    val repository: DailySentenceRepository by lazy {
        DailySentenceRepository(database.dailySentenceDao())
    }

    override fun onCreate() {
        super.onCreate()
        // 앱 초기화 작업
        // 초기 데이터 로드
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.dailywidget.util.InitialLoadHelper.loadInitialData(this@MyApp)
        }

        // ⭐ 앱 시작 시 자정 알람 설정
        DailyWidgetReceiver.scheduleMidnightUpdate(this)

        // 알림 채널 생성
        NotificationHelper.createNotificationChannel(this)
    }
}
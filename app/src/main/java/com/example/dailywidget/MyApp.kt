package com.example.dailywidget

import android.app.Application
import com.example.dailywidget.data.db.AppDatabase
import com.example.dailywidget.data.repository.DailySentenceRepository
import com.example.dailywidget.util.NotificationHelper
import com.example.dailywidget.util.InitialLoadHelper
import com.example.dailywidget.util.calculateNewSentenceCount
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

        // 기존 초기 데이터 로드 코드 삭제하고 아래로 교체
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val dataStoreManager = com.example.dailywidget.data.repository.DataStoreManager(this@MyApp)

            // 버전 체크
            val currentVersion = BuildConfig.VERSION_CODE
            val lastVersion = dataStoreManager.getLastLoadedVersion()

            android.util.Log.d("MyApp", "Current version: $currentVersion, Last version: $lastVersion")

            if (lastVersion == 0) {
                // 최초 실행 - 초기 데이터 로드
                com.example.dailywidget.util.InitialLoadHelper.loadInitialData(this@MyApp)
                dataStoreManager.saveLastLoadedVersion(currentVersion)
            } else if (currentVersion > lastVersion) {
                // 앱 업데이트 감지 - 새 문장 개수 계산
                val newCount = calculateNewSentenceCount(this@MyApp)

                if (newCount > 0) {
                    // 업데이트 플래그 설정
                    dataStoreManager.setUpdateAvailable(true, newCount)
                    android.util.Log.d("MyApp", "New sentences available: $newCount")
                } else {
                    // 새 문장 없으면 버전만 업데이트
                    dataStoreManager.saveLastLoadedVersion(currentVersion)
                }
            }
        }

        // 앱 시작 시 자정 알람 설정
        DailyWidgetReceiver.scheduleMidnightUpdate(this)

        // 알림 채널 생성
        NotificationHelper.createNotificationChannel(this)
    }
}
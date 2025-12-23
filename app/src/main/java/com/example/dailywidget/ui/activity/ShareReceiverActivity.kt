package com.example.dailywidget.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * 외부 앱에서 텍스트 공유 시 수신하는 Activity
 * 리디북스, 알라딘, 예스24 등에서 공유한 텍스트를 받아서 편집 화면으로 전달
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

                    // MainActivity로 텍스트 전달
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        putExtra("navigate_to", "list")
                        putExtra("shared_text", sharedText)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(mainIntent)
                }
            }
        }

        // ShareReceiverActivity는 바로 종료
        finish()
    }
}
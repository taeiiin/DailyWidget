package com.example.dailywidget.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import com.example.dailywidget.R

/**
 * 위젯 배경 리소스 제공자
 * 배경 ID 문자열을 파싱하여 Drawable로 변환
 * - 단색: solid:#RRGGBB,alpha:0.7
 * - 이미지: image:drawable_name
 */
class ResourceProvider(private val context: Context) {

    /**
     * 배경 ID로부터 Drawable 생성
     * @param backgroundId 배경 ID (null이면 흰색 반환)
     * @return ColorDrawable 또는 Drawable
     */
    fun getBackgroundDrawable(backgroundId: String?): Any {
        if (backgroundId.isNullOrEmpty()) {
            return ColorDrawable(Color.WHITE)
        }

        return when {
            backgroundId.startsWith("solid:") -> {
                // 단색 배경: solid:#RRGGBB,alpha:0.7
                val parts = backgroundId.split(",")
                val colorHex = parts[0].removePrefix("solid:")
                val alphaPart = parts.getOrNull(1)?.removePrefix("alpha:") ?: "1.0"

                val alpha = (alphaPart.toFloatOrNull() ?: 1f)
                val color = Color.parseColor(colorHex)
                val colorWithAlpha = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )

                ColorDrawable(colorWithAlpha)
            }

            backgroundId.startsWith("image:") -> {
                // 이미지 배경
                val resName = backgroundId.removePrefix("image:")
                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                ContextCompat.getDrawable(context, if (resId != 0) resId else R.drawable.widget_bg_default)
                    ?: ColorDrawable(Color.WHITE)
            }

            else -> ColorDrawable(Color.WHITE)
        }
    }

    companion object {
        /** 선택 가능한 이미지 리스트 */
        val selectableImages = listOf(
            "widget_bg_1"
        )
    }
}
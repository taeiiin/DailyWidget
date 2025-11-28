package com.example.dailywidget.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import com.example.dailywidget.R

/**
 * 위젯 배경 처리 클래스
 */
class ResourceProvider(private val context: Context) {

    fun getBackgroundDrawable(backgroundId: String?): Any {
        if (backgroundId.isNullOrEmpty()) {
            return ColorDrawable(Color.WHITE)
        }

        return when {
            backgroundId.startsWith("solid:") -> {
                // solid:#RRGGBB,alpha:0.7
                val parts = backgroundId.split(",")
                val colorHex = parts[0].removePrefix("solid:")
                val alphaPart = parts.getOrNull(1)?.removePrefix("alpha:") ?: "1.0"

                val alpha = (alphaPart.toFloatOrNull() ?: 1f)
                val color = Color.parseColor(colorHex)
                val colorWithAlpha = Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

                ColorDrawable(colorWithAlpha)
            }

            backgroundId.startsWith("image:") -> {
                val resName = backgroundId.removePrefix("image:")
                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                ContextCompat.getDrawable(context, if (resId != 0) resId else R.drawable.widget_bg_default)
                    ?: ColorDrawable(Color.WHITE)
            }

            else -> ColorDrawable(Color.WHITE)
        }
    }

    companion object {
        val selectableImages = listOf(
            "widget_bg_1",
            //"widget_bg_2",
            //"widget_bg_3"
        )
    }
}

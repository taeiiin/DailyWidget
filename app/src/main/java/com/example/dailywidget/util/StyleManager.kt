package com.example.dailywidget.util

import android.graphics.Color
import android.view.Gravity
import androidx.compose.ui.text.style.TextAlign

/**
 * 위젯 스타일 관리자
 */
object StyleManager {

    const val DEFAULT_STYLE_ID = 1
    const val DEFAULT_BACKGROUND_ID = "default"

    /**
     * 텍스트 스타일
     */
    data class TextStyle(
        val color: androidx.compose.ui.graphics.Color,
        val align: TextAlign,
        val isBold: Boolean = false
    ) {
        fun toAndroidColor(): Int {
            return android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }

        fun toGravity(): Int {
            return when (align) {
                TextAlign.Left -> Gravity.START
                TextAlign.Center -> Gravity.CENTER_HORIZONTAL
                TextAlign.Right -> Gravity.END
                else -> Gravity.START
            }
        }
    }

    /**
     * 위젯 스타일
     */
    data class WidgetStyle(
        val id: Int,
        val name: String,
        val textStyle: TextStyle,
        val sourceStyle: TextStyle,
        val extraStyle: TextStyle
    )

    /**
     * 사용 가능한 스타일 목록
     */
    private val styles = listOf(
        WidgetStyle(
            id = 1,
            name = "기본 (검정)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.Black,
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.DarkGray,
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.Gray,
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 2,
            name = "모던 (진한 회색)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF2C3E50),
                align = TextAlign.Center,
                isBold = true
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF7F8C8D),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF95A5A6),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 3,
            name = "따뜻한 (갈색)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF5D4037),
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF8D6E63),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFBCAAA4),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 4,
            name = "차분한 (네이비)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF1A237E),
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF5C6BC0),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF9FA8DA),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 5,
            name = "우아한 (보라)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF4A148C),
                align = TextAlign.Center,
                isBold = true
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF8E24AA),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFBA68C8),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 6,
            name = "생동감 (초록)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF81C784),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 7,
            name = "활기찬 (주황)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFE65100),
                align = TextAlign.Center,
                isBold = true
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFFFB74D),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 8,
            name = "부드러운 (핑크)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFC2185B),
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFE91E63),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFF48FB1),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 9,
            name = "시원한 (청록)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF006064),
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF00ACC1),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF4DD0E1),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 10,
            name = "고급스러운 (금색)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFF9C6B00),
                align = TextAlign.Center,
                isBold = true
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFC5A252),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color(0xFFD4AF37),
                align = TextAlign.End,
                isBold = false
            )
        ),
        WidgetStyle(
            id = 11,
            name = "어두운 배경용 (흰색)",
            textStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.White,
                align = TextAlign.Center,
                isBold = false
            ),
            sourceStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                align = TextAlign.End,
                isBold = false
            ),
            extraStyle = TextStyle(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                align = TextAlign.End,
                isBold = false
            )
        )
    )

    /**
     * 스타일 ID로 스타일 가져오기
     */
    fun getWidgetStyle(styleId: Int): WidgetStyle {
        return styles.find { it.id == styleId } ?: styles.first()
    }

    /**
     * 모든 스타일 목록 가져오기
     */
    fun getAllStyles(): List<WidgetStyle> {
        return styles
    }

    /**
     * 모든 스타일 ID 목록
     */
    fun getAllStyleIds(): List<Int> {
        return styles.map { it.id }
    }

    /**
     * 스타일 설명 가져오기
     */
    fun getStyleDescription(styleId: Int): String {
        return styles.find { it.id == styleId }?.name?.substringBefore(" (") ?: "기본"
    }
}
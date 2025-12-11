package com.example.dailywidget.widget

import com.example.dailywidget.util.ThemeManager

/**
 * 배경 설정 데이터 클래스
 * 3가지 배경 타입 지원: 단색, 이미지, 그라디언트
 */
data class BackgroundConfig(
    val isSolid: Boolean = false,
    val isImage: Boolean = false,
    val isGradient: Boolean = false,
    val hexColor: String = "#FFFFFF",
    val imageName: String? = null,
    val isThemeImage: Boolean = false,
    val themeId: String? = null,
    val themeFileName: String? = null,
    val gradientStartColor: String? = null,
    val gradientEndColor: String? = null,
    val gradientDirection: String? = null,
    val alpha: Float = 1.0f
)

/**
 * backgroundId 문자열을 BackgroundConfig로 파싱
 *
 * 지원 형식:
 * - solid:#FFFFFF,alpha:0.5
 * - image:theme:city/img1.jpg,alpha:0.5
 * - image:file://abc.jpg,alpha:0.5
 * - gradient:#FF6B9D,#FFC371,horizontal,alpha:0.5
 */
fun parseBackgroundId(backgroundId: String): BackgroundConfig {
    // alpha 추출
    val alpha = backgroundId.split(",").find { it.startsWith("alpha:") }
        ?.substringAfter("alpha:")
        ?.toFloatOrNull() ?: 1.0f

    return when {
        backgroundId.startsWith("solid:") -> {
            val hex = backgroundId.substringAfter("solid:").substringBefore(",alpha:")
            BackgroundConfig(
                isSolid = true,
                hexColor = hex,
                alpha = alpha
            )
        }
        backgroundId.startsWith("image:") -> {
            val imagePath = backgroundId.substringAfter("image:").substringBefore(",alpha:")

            if (imagePath.startsWith("theme:")) {
                // 테마 이미지: theme:city/img1.jpg
                val parsed = ThemeManager.parseThemeImagePath(imagePath)
                BackgroundConfig(
                    isImage = true,
                    isThemeImage = true,
                    themeId = parsed?.first,
                    themeFileName = parsed?.second,
                    imageName = imagePath,  // "theme:city/img1.jpg" 그대로 저장
                    alpha = alpha
                )
            } else {
                // 사용자 이미지: file://abc.jpg
                BackgroundConfig(
                    isImage = true,
                    isThemeImage = false,
                    imageName = imagePath,
                    alpha = alpha
                )
            }
        }
        backgroundId.startsWith("gradient:") -> {
            // 그라디언트: gradient:#FF6B9D,#FFC371,horizontal,alpha:0.5
            val gradientPart = backgroundId.substringAfter("gradient:").substringBefore(",alpha:")
            val colors = gradientPart.split(",")

            BackgroundConfig(
                isGradient = true,
                gradientStartColor = colors.getOrNull(0),
                gradientEndColor = colors.getOrNull(1),
                gradientDirection = colors.getOrNull(2) ?: "horizontal",
                alpha = alpha
            )
        }
        else -> BackgroundConfig(alpha = alpha)
    }
}
package com.example.dailywidget.util

import android.content.Context

/**
 * 테마 이미지 관리자
 * assets/themes/ 폴더의 이미지들을 카테고리별로 관리
 * 7개 테마: 도시, 하늘, 바다, 자연, 건축, 그림, 기타
 */
object ThemeManager {

    /** 테마 정보 */
    data class Theme(
        val id: String,
        val displayName: String,
        val description: String
    )

    /** 테마 이미지 정보 */
    data class ThemeImage(
        val themeId: String,
        val fileName: String,
        val path: String
    )

    /** 사용 가능한 테마 목록 (7개) */
    private val themes = listOf(
        Theme("city", "도시", "도시 풍경"),
        Theme("sky", "하늘", "하늘, 구름"),
        Theme("water", "바다", "바다, 해변, 물"),
        Theme("nature", "자연", "자연, 꽃, 나무"),
        Theme("struct", "건축", "성, 건축"),
        Theme("paintings", "그림", "명화, 풍경"),
        Theme("etc", "기타", "소품, 오브제, 기타 등등")
    )

    /** 모든 테마 가져오기 */
    fun getAllThemes(): List<Theme> = themes

    /** 특정 테마 가져오기 */
    fun getTheme(themeId: String): Theme? {
        return themes.find { it.id == themeId }
    }

    /**
     * 테마별 이미지 목록 가져오기
     * assets/themes/{themeId}/ 폴더에서 이미지 파일 조회
     */
    fun getThemeImages(context: Context, themeId: String): List<ThemeImage> {
        return try {
            val files = context.assets.list("themes/$themeId") ?: emptyArray()
            files
                .filter { it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") }
                .sorted()
                .map { fileName ->
                    ThemeImage(
                        themeId = themeId,
                        fileName = fileName,
                        path = "themes/$themeId/$fileName"
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** 테마에 이미지가 있는지 확인 */
    fun hasImages(context: Context, themeId: String): Boolean {
        return getThemeImages(context, themeId).isNotEmpty()
    }

    /**
     * 테마 이미지 경로 파싱
     * "theme:city/image1.jpg" → Pair("city", "image1.jpg")
     */
    fun parseThemeImagePath(path: String): Pair<String, String>? {
        if (!path.startsWith("theme:")) return null

        val pathParts = path.removePrefix("theme:").split("/")
        if (pathParts.size != 2) return null

        return Pair(pathParts[0], pathParts[1])
    }

    /**
     * 테마 이미지 경로 생성
     * ("city", "image1.jpg") → "theme:city/image1.jpg"
     */
    fun buildThemeImagePath(themeId: String, fileName: String): String {
        return "theme:$themeId/$fileName"
    }

    /**
     * Assets 경로로 변환
     * "theme:city/image1.jpg" → "themes/city/image1.jpg"
     */
    fun getAssetPath(themeImagePath: String): String? {
        val parsed = parseThemeImagePath(themeImagePath) ?: return null
        return "themes/${parsed.first}/${parsed.second}"
    }
}
package com.example.dailywidget.util


import android.content.Context
object ThemeManager {
    data class Theme(
        val id: String,
        val displayName: String,
        val description: String
    )

    data class ThemeImage(
        val themeId: String,
        val fileName: String,
        val path: String
    )

    private val themes = listOf(
        Theme("city", "도시", "도시 풍경"),
        Theme("sky", "하늘", "하늘, 구름"),
        Theme("water", "바다", "바다, 해변, 물"),
        Theme("nature", "자연", "자연, 꽃, 나무"),
        Theme("struct", "건축", "성, 건축"),
        Theme("paintings", "그림", "명화, 풍경"),
        Theme("etc", "기타", "소품, 오브제, 기타 등등"),
    )

    fun getAllThemes(): List<Theme> = themes

    fun getTheme(themeId: String): Theme? {
        return themes.find { it.id == themeId }
    }

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

    fun hasImages(context: Context, themeId: String): Boolean {
        return getThemeImages(context, themeId).isNotEmpty()
    }

    fun parseThemeImagePath(path: String): Pair<String, String>? {
        if (!path.startsWith("theme:")) return null

        val pathParts = path.removePrefix("theme:").split("/")
        if (pathParts.size != 2) return null

        return Pair(pathParts[0], pathParts[1])
    }

    fun buildThemeImagePath(themeId: String, fileName: String): String {
        return "theme:$themeId/$fileName"
    }

    fun getAssetPath(themeImagePath: String): String? {
        val parsed = parseThemeImagePath(themeImagePath) ?: return null
        return "themes/${parsed.first}/${parsed.second}"
    }
}
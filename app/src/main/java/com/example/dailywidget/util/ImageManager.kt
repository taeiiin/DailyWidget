package com.example.dailywidget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 사용자 이미지 관리자
 * - 갤러리에서 선택한 이미지 저장
 * - 자동 리사이즈 (최대 1080px)
 * - 최대 10개, 50MB 제한
 */
object ImageManager {

    private const val IMAGES_DIR = "widget_backgrounds"
    private const val MAX_IMAGE_SIZE = 1080 // 최대 가로/세로 크기
    private const val MAX_IMAGES = 10
    private const val MAX_TOTAL_SIZE_MB = 50L

    /**
     * 이미지 저장 디렉토리 가져오기
     */
    private fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 갤러리에서 선택한 이미지 저장
     * @return 저장된 파일명 (예: "bg_1732701234567.jpg") 또는 null
     */
    fun saveImageFromUri(context: Context, uri: Uri): String? {
        return try {
            // 1. 용량 체크
            if (!canAddImage(context)) {
                return null
            }

            // 2. Bitmap으로 변환
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 3. 리사이즈
            val resizedBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_SIZE)

            // 4. 파일명 생성 (타임스탬프)
            val fileName = "bg_${System.currentTimeMillis()}.jpg"
            val file = File(getImagesDir(context), fileName)

            // 5. 저장
            FileOutputStream(file).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // 6. 메모리 정리
            if (originalBitmap != resizedBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            fileName
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Bitmap 리사이즈 (비율 유지)
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 이미지 추가 가능 여부 확인
     */
    fun canAddImage(context: Context): Boolean {
        val images = getUserImages(context)

        // 개수 제한
        if (images.size >= MAX_IMAGES) {
            return false
        }

        // 용량 제한
        val totalSize = getTotalImageSize(context)
        if (totalSize >= MAX_TOTAL_SIZE_MB * 1024 * 1024) {
            return false
        }

        return true
    }

    /**
     * 사용자 이미지 목록 가져오기
     */
    fun getUserImages(context: Context): List<String> {
        val dir = getImagesDir(context)
        return dir.listFiles()
            ?.filter { it.extension == "jpg" || it.extension == "png" }
            ?.map { it.name }
            ?.sortedDescending() // 최신순
            ?: emptyList()
    }

    /**
     * 이미지 파일 가져오기
     */
    fun getImageFile(context: Context, fileName: String): File? {
        val file = File(getImagesDir(context), fileName)
        return if (file.exists()) file else null
    }

    /**
     * 이미지 삭제
     */
    fun deleteImage(context: Context, fileName: String): Boolean {
        val file = File(getImagesDir(context), fileName)
        return file.delete()
    }

    /**
     * 전체 이미지 용량 계산 (bytes)
     */
    fun getTotalImageSize(context: Context): Long {
        val dir = getImagesDir(context)
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * 전체 이미지 용량 (MB 단위)
     */
    fun getTotalImageSizeMB(context: Context): Float {
        return getTotalImageSize(context) / (1024f * 1024f)
    }

    /**
     * 모든 사용자 이미지 삭제
     */
    fun clearAllImages(context: Context) {
        val dir = getImagesDir(context)
        dir.listFiles()?.forEach { it.delete() }
    }
}
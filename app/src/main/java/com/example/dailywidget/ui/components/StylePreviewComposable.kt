package com.example.dailywidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailywidget.util.StyleManager

/**
 * 스타일 프리뷰 컴포넌트
 * 텍스트 스타일(색상, 정렬, 굵기)을 시각적으로 미리보기
 */
@Composable
fun StylePreview(
    styleId: Int,
    modifier: Modifier = Modifier
) {
    val style = StyleManager.getWidgetStyle(styleId)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 스타일 제목
            Text(
                text = "스타일 $styleId - ${StyleManager.getStyleDescription(styleId)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 메인 문장 프리뷰
            Text(
                text = "메인 문장 예시",
                color = style.textStyle.color,
                fontSize = 20.sp,
                textAlign = style.textStyle.align,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 출처/작가 프리뷰
            Text(
                text = "- 출처, 작가",
                color = style.sourceStyle.color,
                fontSize = 14.sp,
                textAlign = style.sourceStyle.align,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 특이사항 프리뷰
            Text(
                text = "특이사항",
                color = style.extraStyle.color,
                fontSize = 12.sp,
                textAlign = style.extraStyle.align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 모든 스타일 비교 프리뷰
 * 전체 스타일 목록을 한 번에 확인
 */
@Composable
fun StyleComparisonPreview(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        StyleManager.getAllStyleIds().forEach { styleId ->
            StylePreview(styleId = styleId)
        }
    }
}
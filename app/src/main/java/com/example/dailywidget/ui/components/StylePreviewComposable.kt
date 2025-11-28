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
 *
 * 사용처:
 * - SentenceEditorScreen에서 스타일 선택 시
 * - 설정 화면에서 스타일 미리보기
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
                .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5))
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 스타일 제목
            Text(
                text = "스타일 $styleId - ${StyleManager.getStyleDescription(styleId)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // text 프리뷰 (고정 폰트 크기: 20sp)
            Text(
                text = "메인 문장 예시",
                color = style.textStyle.color,
                fontSize = 20.sp,
                textAlign = style.textStyle.align,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // source 프리뷰 (고정 폰트 크기: 14sp)
            Text(
                text = "- 출처, 작가",
                color = style.sourceStyle.color,
                fontSize = 14.sp,
                textAlign = style.sourceStyle.align,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // extra 프리뷰 (고정 폰트 크기: 12sp)
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
 * 여러 스타일을 한번에 비교하는 프리뷰
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
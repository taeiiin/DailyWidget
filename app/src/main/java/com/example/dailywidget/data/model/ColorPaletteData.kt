package com.example.dailywidget.data.model

import androidx.compose.ui.graphics.Color

/**
 * 100개 색상 팔레트 데이터
 * 10가지 컬러 × 10단계 쉐이드
 */
object ColorPaletteData {

    data class ColorInfo(
        val hex: String,
        val name: String
    ) {
        val color: Color get() = Color(android.graphics.Color.parseColor(hex))
    }

    /**
     * 10가지 기본 컬러 (각각 8단계 쉐이드)
     */
    val palette: List<List<ColorInfo>> = listOf(
        // 1. 블러쉬 핑크 (Blush Pink)
        listOf(
            ColorInfo("#FCF2F5", "블러쉬 핑크 50"),
            ColorInfo("#F4C2D2", "블러쉬 핑크 100"),
            ColorInfo("#EB92AF", "블러쉬 핑크 200"),
            ColorInfo("#E3638C", "블러쉬 핑크 300"),
            ColorInfo("#DA336A", "블러쉬 핑크 400"),
            ColorInfo("#B52051", "블러쉬 핑크 500"),
            ColorInfo("#86173B", "블러쉬 핑크 600"),
            ColorInfo("#560F26", "블러쉬 핑크 800")
        ),

        // 2. 애프리콧 베이지 (Apricot Beige)
        listOf(
            ColorInfo("#FDF6F1", "애프리콧 베이지 50"),
            ColorInfo("#F7D7BE", "애프리콧 베이지 100"),
            ColorInfo("#F1B88C", "애프리콧 베이지 200"),
            ColorInfo("#EC995A", "애프리콧 베이지 300"),
            ColorInfo("#E67A27", "애프리콧 베이지 400"),
            ColorInfo("#C06015", "애프리콧 베이지 500"),
            ColorInfo("#8D4610", "애프리콧 베이지 600"),
            ColorInfo("#5B2D0A", "애프리콧 베이지 800")
        ),

        // 3. 트랜스페런트 옐로우 (Transparent Yellow)
        listOf(
            ColorInfo("#FCFAF2", "트랜스페런트 옐로우 50"),
            ColorInfo("#F4ECC2", "트랜스페런트 옐로우 100"),
            ColorInfo("#EBDD92", "트랜스페런트 옐로우 200"),
            ColorInfo("#E2CE63", "트랜스페런트 옐로우 300"),
            ColorInfo("#DABF33", "트랜스페런트 옐로우 400"),
            ColorInfo("#B59D20", "트랜스페런트 옐로우 500"),
            ColorInfo("#857418", "트랜스페런트 옐로우 600"),
            ColorInfo("#564B0F", "트랜스페런트 옐로우 800")
        ),

        // 4. 세이지 그린 (Sage Green)
        listOf(
            ColorInfo("#F6F9F5", "세이지 그린 50"),
            ColorInfo("#D6E3D2", "세이지 그린 100"),
            ColorInfo("#B6CEAF", "세이지 그린 200"),
            ColorInfo("#96B98D", "세이지 그린 300"),
            ColorInfo("#76A36A", "세이지 그린 400"),
            ColorInfo("#5C8451", "세이지 그린 500"),
            ColorInfo("#43623C", "세이지 그린 600"),
            ColorInfo("#2B3F26", "세이지 그린 800")
        ),

        // 5. 더스트 틸 (Dust Teal)
        listOf(
            ColorInfo("#F5F9F9", "더스트 틸 50"),
            ColorInfo("#D2E3E4", "더스트 틸 100"),
            ColorInfo("#AFCECF", "더스트 틸 200"),
            ColorInfo("#8BB9BA", "더스트 틸 300"),
            ColorInfo("#68A3A5", "더스트 틸 400"),
            ColorInfo("#4F8486", "더스트 틸 500"),
            ColorInfo("#3A6163", "더스트 틸 600"),
            ColorInfo("#263F40", "더스트 틸 800")
        ),

        // 6. 미스트 블루 (Mist Blue)
        listOf(
            ColorInfo("#EFF5FF", "미스트 블루 50"),
            ColorInfo("#B7D2FF", "미스트 블루 100"),
            ColorInfo("#7FB0FE", "미스트 블루 200"),
            ColorInfo("#478DFF", "미스트 블루 300"),
            ColorInfo("#0F6AFF", "미스트 블루 400"),
            ColorInfo("#0051D6", "미스트 블루 500"),
            ColorInfo("#003C9E", "미스트 블루 600"),
            ColorInfo("#002665", "미스트 블루 800")
        ),

        // 7. 페리윙클 (Periwinkle)
        listOf(
            ColorInfo("#F1F2FD", "페리윙클 50"),
            ColorInfo("#C0C5F6", "페리윙클 100"),
            ColorInfo("#8E97F0", "페리윙클 200"),
            ColorInfo("#5C6AE9", "페리윙클 300"),
            ColorInfo("#2B3DE2", "페리윙클 400"),
            ColorInfo("#1928BD", "페리윙클 500"),
            ColorInfo("#121E8B", "페리윙클 600"),
            ColorInfo("#0B135A", "페리윙클 800")
        ),

        // 8. 모브 라벤더 (Mauve Lavender)
        listOf(
            ColorInfo("#F7F4FA", "모브 라벤더 50"),
            ColorInfo("#DDCCEA", "모브 라벤더 100"),
            ColorInfo("#C3A4DA", "모브 라벤더 200"),
            ColorInfo("#A87CC9", "모브 라벤더 300"),
            ColorInfo("#8E54B9", "모브 라벤더 400"),
            ColorInfo("#713D98", "모브 라벤더 500"),
            ColorInfo("#532D70", "모브 라벤더 600"),
            ColorInfo("#361D48", "모브 라벤더 800")
        ),

        // 9. 그레이지 (Greige)
        listOf(
            ColorInfo("#F8F7F6", "그레이지 50"),
            ColorInfo("#DEDCD8", "그레이지 100"),
            ColorInfo("#C5C0B9", "그레이지 200"),
            ColorInfo("#ABA59A", "그레이지 300"),
            ColorInfo("#92897C", "그레이지 400"),
            ColorInfo("#746D61", "그레이지 500"),
            ColorInfo("#565047", "그레이지 600"),
            ColorInfo("#37342E", "그레이지 800")
        ),

        // 10. 차콜 틴트 (Charcoal Tint)
        listOf(
            ColorInfo("#F7F7F7", "차콜 틴트 50"),
            ColorInfo("#D9DADC", "차콜 틴트 100"),
            ColorInfo("#BCBDC1", "차콜 틴트 200"),
            ColorInfo("#9FA0A6", "차콜 틴트 300"),
            ColorInfo("#82848B", "차콜 틴트 400"),
            ColorInfo("#67686F", "차콜 틴트 500"),
            ColorInfo("#4C4D51", "차콜 틴트 600"),
            ColorInfo("#313134", "차콜 틴트 800")
        )
    )

    /**
     * 흰색/검정 (특별 색상)
     */
    val specialColors = listOf(
        ColorInfo("#FFFFFF", "흰색"),
        ColorInfo("#000000", "검정")
    )

    /**
     * 전체 색상 개수
     */
    val totalColors: Int = palette.sumOf { it.size } + specialColors.size
}
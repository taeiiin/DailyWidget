package com.example.dailywidget.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 회색 팔레트 (#F5F5F5 기준) ====================

val Gray50 = Color(0xFFFAFAFA)   // 가장 밝은 회색 (거의 흰색)
val Gray100 = Color(0xFFF5F5F5)  // 기준 색상
val Gray200 = Color(0xFFEEEEEE)  // 약간 진한 회색
val Gray300 = Color(0xFFE0E0E0)  // 구분선용
val Gray400 = Color(0xFFBDBDBD)  // 비활성 요소
val Gray500 = Color(0xFF9E9E9E)  // 중간 회색
val Gray600 = Color(0xFF757575)  // 보조 텍스트
val Gray700 = Color(0xFF616161)  // 진한 텍스트
val Gray800 = Color(0xFF424242)  // 매우 진한 회색
val Gray900 = Color(0xFF212121)  // 거의 검정

// ==================== Primary Colors (파란색) ====================

val Blue = Color(0xFF007AFF)           // iOS 블루
val BlueLight = Color(0xFF5AC8FA)      // 연한 블루
val BlueDark = Color(0xFF0A84FF)       // 진한 블루
val BlueVeryLight = Color(0xFF64D2FF)  // 매우 연한 블루

// ==================== Success/Error ====================

val Green = Color(0xFF34C759)
val Red = Color(0xFFFF3B30)
val Orange = Color(0xFFFF9500)

// ==================== Light Mode ====================

val BackgroundLight = Color.White           // 전체 배경: 순백색
val SurfaceLight = Color.White              // 카드 배경: 순백색
val SurfaceVariantLight = Gray100           // 강조 카드: #F5F5F5
val SurfaceContainerLight = Gray50          // 컨테이너: 가장 밝은 회색

val TextPrimary = Color.Black               // 주요 텍스트: 검정
val TextSecondary = Gray600                 // 보조 텍스트: 중간 회색
val TextTertiary = Gray400                  // 3차 텍스트: 밝은 회색
val TextDisabled = Gray300                  // 비활성: 매우 밝은 회색

val DividerLight = Gray300                  // 구분선: #E0E0E0
val OutlineLight = Gray400                  // 테두리: #BDBDBD

// ==================== Dark Mode ====================

val BackgroundDark = Gray900                // 다크 배경
val SurfaceDark = Gray800                   // 다크 카드
val SurfaceVariantDark = Gray700            // 다크 강조
val SurfaceContainerDark = Gray800          // 다크 컨테이너

val TextPrimaryDark = Color.White           // 다크 주요 텍스트
val TextSecondaryDark = Gray400             // 다크 보조 텍스트
val TextTertiaryDark = Gray500              // 다크 3차 텍스트

val DividerDark = Gray700                   // 다크 구분선
val OutlineDark = Gray600                   // 다크 테두리

// ==================== 레거시 색상 (호환성) ====================

val Purple80 = BlueLight
val PurpleGrey80 = Gray400
val Pink80 = BlueVeryLight

val Purple40 = Blue
val PurpleGrey40 = Gray500
val Pink40 = BlueLight
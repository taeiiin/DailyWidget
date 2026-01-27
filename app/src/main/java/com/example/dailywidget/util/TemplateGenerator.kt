package com.example.dailywidget.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 템플릿 파일 생성기
 * JSON/CSV 템플릿 및 가이드 생성
 */
object TemplateGenerator {

    /**
     * JSON 템플릿 생성
     */
    fun generateJsonTemplate(): String {
        return """[
  {
    "date": "0101",
    "text": "새해 첫날의 문장입니다.",
    "source": "작품 제목",
    "writer": "작가 이름",
    "extra": "특이사항 (선택사항)",
    "genre": "your_genre_id"
  },
  {
    "date": "0102",
    "text": "1월 2일의 문장입니다.",
    "source": "작품 제목",
    "writer": "작가 이름",
    "extra": "",
    "genre": "your_genre_id"
  },
  {
    "date": "0103",
    "text": "여러 문장을 추가할 수 있습니다.",
    "source": "작품 제목",
    "writer": "작가 이름",
    "extra": "이곳에 메모를 남길 수 있습니다",
    "genre": "your_genre_id"
  }
]"""
    }

    /**
     * CSV 템플릿 생성
     */
    fun generateCsvTemplate(): String {
        return """date,text,source,writer,extra,genre
0101,새해 첫날의 문장입니다.,작품 제목,작가 이름,특이사항 (선택사항),your_genre_id
0102,1월 2일의 문장입니다.,작품 제목,작가 이름,,your_genre_id
0103,여러 문장을 추가할 수 있습니다.,작품 제목,작가 이름,이곳에 메모를 남길 수 있습니다,your_genre_id"""
    }

    /**
     * 사용 가이드 생성
     */
    fun generateGuideText(): String {
        return """━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DailyWidget 문장 파일 템플릿 사용 가이드
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📌 파일 형식
────────────────────────────────
- JSON 형식 (.json 확장자) 또는 CSV 형식 (.csv 확장자)
- UTF-8 인코딩 필수
- 텍스트 편집기나 엑셀에서 편집 가능

📌 필수 항목
────────────────────────────────
✓ date: 4자리 숫자 (MMDD 형식)
  예: "0101" = 1월 1일
      "1225" = 12월 25일

✓ text: 표시할 문장 내용
  - 비워둘 수 없음
  - 최대 500자 권장

✓ genre: 장르 ID
  - 영문 소문자, 숫자, 언더스코어(_)만 가능
  - 예: my_quotes, classic, wisdom

📌 선택 항목
────────────────────────────────
○ source: 출처 (예: 책 제목, 영화 제목)
○ writer: 작가 또는 저자
○ extra: 특이사항, 메모, 설명 등

📌 JSON 형식 작성 예시
────────────────────────────────
{
  "date": "1225",
  "text": "메리 크리스마스!",
  "source": "크리스마스 캐럴",
  "writer": "찰스 디킨스",
  "extra": "크리스마스 특집",
  "genre": "classic"
}

📌 CSV 형식 작성 예시
────────────────────────────────
date,text,source,writer,extra,genre
1225,메리 크리스마스!,크리스마스 캐럴,찰스 디킨스,크리스마스 특집,classic

⚠️ CSV 주의사항:
- 쉼표(,)가 포함된 내용은 큰따옴표로 감싸기
  예: "안녕, 세상아",작품명,작가,,poem
- 엑셀에서 편집 시 인코딩을 UTF-8로 저장

📌 특수문자 처리
────────────────────────────────
- JSON: 큰따옴표는 \"로 입력
  예: "text": "그는 \"안녕\"이라고 말했다"

- JSON: 줄바꿈은 \n으로 입력
  예: "text": "첫 번째 줄\n두 번째 줄"

- CSV: 큰따옴표는 ""로 입력
  예: "그는 ""안녕""이라고 말했다"

📌 날짜 형식 상세
────────────────────────────────
✓ 올바른 형식:
  0101, 0215, 0630, 1225

✗ 잘못된 형식:
  1/1 (슬래시 사용 불가)
  101 (4자리 필수)
  1301 (13월 불가능)
  0230 (2월 30일 불가능)

📌 장르 ID 규칙
────────────────────────────────
✓ 사용 가능:
  my_quotes
  classic_literature
  movie_quote_2024

✗ 사용 불가:
  나의 명언 (한글 불가)
  my quotes (공백 불가)
  my-quotes (하이픈 불가)
  My_Quotes (대문자 불가)

📌 파일 가져오기 방법
────────────────────────────────
1. 앱 실행 → 설정 메뉴
2. "문장 파일 가져오기" 섹션
3. JSON 또는 CSV 파일 선택
4. 장르 ID와 표시명 입력
5. 검증 결과 확인 후 가져오기

📌 자주 묻는 질문
────────────────────────────────
Q: 한 파일에 여러 장르를 섞을 수 있나요?
A: 가능하지만, 가져올 때 하나의 장르로 통일됩니다.
   장르별로 파일을 나누는 것을 권장합니다.

Q: 같은 날짜에 여러 문장을 넣을 수 있나요?
A: 가능합니다. 위젯에서 무작위로 하나씩 표시됩니다.

Q: 기존 문장과 중복되면 어떻게 되나요?
A: 같은 날짜 + 같은 장르 + 같은 문장 내용이면
   중복으로 판단하여 건너뜁니다.

Q: 엑셀로 작성할 수 있나요?
A: CSV 형식을 선택하면 엑셀로 편집 가능합니다.
   저장 시 "CSV UTF-8"로 저장하세요.

📌 문의 및 지원
────────────────────────────────
앱 내 설정 → 개발자 정보 참조

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
작성일: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}
버전: 1.0.0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"""
    }

    /**
     * Downloads 폴더에 파일 저장
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        content: String
    ): Result<String> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            file.writeText(content, Charsets.UTF_8)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("파일 저장 실패: ${e.message}"))
        }
    }

    /**
     * 템플릿 파일명 생성
     */
    fun generateTemplateFileName(type: TemplateType): String {
        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return when (type) {
            TemplateType.JSON -> "dailywidget_template_$timestamp.json"
            TemplateType.CSV -> "dailywidget_template_$timestamp.csv"
            TemplateType.GUIDE -> "dailywidget_guide_$timestamp.txt"
        }
    }

    enum class TemplateType {
        JSON, CSV, GUIDE
    }
}
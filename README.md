# DailyWidget

<div align="center">

**매일 새로운 문장을 전하는 안드로이드 위젯 앱**

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

</div>

---

## 📋 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [스크린샷](#-스크린샷)
- [빠른 시작](#-빠른-시작)
- [사용 가이드](#-사용-가이드)
- [설치 방법](#-설치-방법)
- [기술 스택](#-기술-스택)
- [라이선스](#-라이선스)
- [개발자](#-개발자)

---

## 📱 프로젝트 소개

**DailyWidget**은 사용자가 선택한 장르의 일일 명언과 문장을 홈 화면 위젯으로 제공하는 안드로이드 앱입니다.

### 💎 핵심 특징

> 📖 **매일의 영감**: 날짜별로 다른 문장을 자동으로 제공  
> 🎨 **완벽한 커스터마이징**: 10가지 스타일, 다양한 배경, 투명도 조절  
> 🏷️ **유연한 장르 시스템**: 기본 장르 + 사용자 정의 장르 + 복수 장르 선택  
> 🔄 **간편한 관리**: 백업/복원, 문장 CRUD, 자정 자동 업데이트

### 📅 개발 기간
**2025.11.24 ~ 2025.12.10** (약 3주)

---

## ✨ 주요 기능

### 🎯 위젯 기능

#### 일일 문장 표시
- 날짜(MMdd)별로 필터링된 문장 자동 표시
- 자정(00:00)에 자동으로 다음 날 문장 업데이트
- 문장이 여러 개인 경우 랜덤 선택

#### 장르 시스템
- **기본 장르**: 소설, 판타지, 시
- **사용자 정의 장르**: 무제한 추가 가능
- **복수 장르 선택**: 여러 장르를 조합하여 하나의 위젯에 표시
  - 예시: **"소설 & 판타지"** 위젯 → 두 장르의 문장을 모두 표시

#### 커스터마이징

**텍스트 스타일**
- 10가지 조합: 색상, 정렬, 굵기, 폰트

**배경 선택**
- **단색**: 팔레트(8가지 색상 × 8단계 명도) + 커스텀 HSV 컬러 피커
- **그라디언트**: 4가지 방향(가로, 세로, 대각선 2종), 시작/끝 색상 선택
- **이미지**: 테마 이미지(7개 카테고리) + 사용자 이미지(최대 10개, 총 50MB)

**투명도**
- 범위: 10%~100%, 단위: 5% 스냅

**폰트 크기**
- 메인 텍스트: 8~25sp (기본 16sp)
- 출처&작가: 6~20sp (기본 13sp)
- 특이사항: 5~15sp (기본 10sp)

#### 위젯 터치 동작
1. **다음 문장** (기본값) - 같은 날짜 내에서 다른 문장으로 변경
2. **앱 열기** - 메인 화면으로 이동
3. **공유하기** - 문장 공유 다이얼로그
4. **위젯 설정** - 위젯 편집 화면으로
5. **목록 화면** - 문장 목록 화면으로

---

### 📝 앱 내 기능

#### 문장 관리 (CRUD)
- ✅ **생성**: 새 문장 추가
- ✅ **조회**: 전체 목록 및 필터링 (텍스트, 출처, 작가 통합 검색)
- ✅ **수정**: 기존 문장 편집
- ✅ **삭제**: 문장 제거

**필수 필드**: 텍스트, 장르, 날짜 (MMdd)  
**선택 필드**: 출처, 작가, 특이사항

#### 장르 관리
- **추가**: 장르 ID(영문 소문자, 숫자, 언더스코어) + 표시명(한글 가능)
- **삭제**: 사용자 정의 장르만 삭제 가능 (기본 장르는 삭제 불가)

#### 백업 및 복원
- **백업**: JSON 형식, 모든 문장 데이터 + 사용자 정의 장르 포함
- **복원**: 미리보기 제공 (총 개수, 신규 개수, 중복 개수)
- **중복 처리**: 건너뛰기(권장) / 덮어쓰기 / 모두 추가

#### 표시 설정
- 출처, 작가, 특이사항 표시 켜기/끄기
- 홈 화면 뷰 모드: 카드 뷰 / 리스트 뷰

---

## 📸 스크린샷

### 위젯 예시

| 다양한 스타일 | 배경 예시 | 복수 장르 |
|:------------:|:---------:|:---------:|
| ![위젯 스타일](https://via.placeholder.com/300x200?text=Widget+Styles) | ![위젯 배경](https://via.placeholder.com/300x200?text=Backgrounds) | ![복수 장르](https://via.placeholder.com/300x200?text=Multi+Genre) |

### 앱 주요 화면

| 홈 화면 | 문장 목록 | 문장 편집 | 설정 화면 |
|:-------:|:---------:|:---------:|:---------:|
| ![홈](https://via.placeholder.com/200x350?text=Home) | ![목록](https://via.placeholder.com/200x350?text=List) | ![편집](https://via.placeholder.com/200x350?text=Editor) | ![설정](https://via.placeholder.com/200x350?text=Settings) |

### 위젯 설정 과정

| 1. 장르 선택 | 2. 스타일 선택 | 3. 배경 선택 | 4. 투명도 조절 |
|:------------:|:--------------:|:------------:|:--------------:|
| ![장르](https://via.placeholder.com/200x350?text=Genre) | ![스타일](https://via.placeholder.com/200x350?text=Style) | ![배경](https://via.placeholder.com/200x350?text=Background) | ![투명도](https://via.placeholder.com/200x350?text=Alpha) |

---

## 🚀 빠른 시작

### 위젯 추가하기

#### 1단계: 위젯 배치
1. 홈 화면 빈 공간을 **길게 누르기**
2. **"위젯"** 메뉴 선택
3. 앱 목록에서 **"DailyWidget"** 찾기
4. 위젯을 홈 화면으로 **드래그**

#### 2단계: 장르 선택
- 원하는 장르 선택 (체크박스)
- **복수 선택 가능**: 여러 개 체크
- **다음** 버튼으로 진행

> 💡 **Tip**: 소설만 선택 → "소설 위젯" / 소설 + 판타지 선택 → "소설 & 판타지 위젯"

#### 3단계: 스타일 & 배경 설정
1. **스타일**: 10가지 스타일 중 선택
2. **배경**: 컬러 / 그라디언트 / 이미지 탭에서 선택
3. **투명도**: 슬라이더로 조절 (10%~100%)
4. **터치 동작**: 원하는 동작 선택 (기본: 다음 문장)

#### 4단계: 완료
- **"완료"** 버튼 클릭
- 위젯이 홈 화면에 표시됨

---

## 📖 사용 가이드

### 위젯 커스터마이징

<details>
<summary><b>기존 위젯 편집</b></summary>

1. 위젯을 **길게 누르기**
2. **"편집"** 또는 **설정 아이콘** 선택
3. 스타일, 배경, 투명도, 터치 동작 수정
4. "완료" 클릭

> ⚠️ **주의**: 장르는 변경할 수 없습니다. 장르를 바꾸려면 위젯을 삭제하고 다시 추가하세요.

</details>

<details>
<summary><b>배경 선택 가이드</b></summary>

#### 🎨 컬러
- **팔레트**: 흰색/검정 + 8가지 색상 × 8단계 명도 그리드
- **커스텀**: HSV 컬러 피커로 자유로운 색상 선택

#### 🌈 그라디언트
1. 시작 색상 선택
2. 끝 색상 선택
3. 방향 선택: 가로(→), 세로(↓), 대각선(↘, ↙)

#### 🖼️ 이미지
- **테마 이미지**: 7개 카테고리 (도시, 하늘, 바다, 자연, 건축, 그림, 기타)
- **내 이미지**: 최대 10개, 총 50MB, 자동 최적화

</details>

---

### 문장 관리

<details>
<summary><b>문장 추가</b></summary>

1. 앱 실행 → 하단 네비게이션 **"목록"** 탭
2. 우측 하단 **FAB(+)** 버튼 클릭
3. 필드 입력:
   - 텍스트 (필수)
   - 출처, 작가, 특이사항 (선택)
   - 장르 선택 (필수)
   - 날짜 선택 (필수, MMdd)
4. **"저장"** 버튼 클릭

</details>

<details>
<summary><b>문장 편집/삭제</b></summary>

1. **목록** 탭에서 문장 클릭
2. 내용 수정 후 **"저장"** 또는 **"삭제"**

</details>

---

### 장르 관리

<details>
<summary><b>장르 추가</b></summary>

1. **설정** 탭 → **"장르 관리"** 섹션
2. **"장르 추가"** 클릭
3. 입력:
   - **장르 ID**: 영문 소문자, 숫자, 언더스코어 (예: `movie_quote`)
   - **장르 표시명**: 한글 가능 (예: `영화 대사`)
4. **"추가"** 버튼 클릭

</details>

<details>
<summary><b>장르 삭제</b></summary>

- 사용자 정의 장르만 삭제 가능
- 기본 장르(소설, 판타지, 시)는 삭제 불가
- 삭제 시 해당 장르의 문장은 유지됨

</details>

---

### 백업 및 복원

<details>
<summary><b>백업하기</b></summary>

1. **설정** 탭 → **"백업 및 복원"** 섹션
2. **"백업하기"** 클릭
3. 백업 정보 확인 (총 문장 개수, 장르별 개수)
4. **"백업하기"** 확인
5. 저장 위치 선택 (다운로드 폴더 권장)

**파일명**: `backup_YYYY_MM_DD_HHMMSS.json`

</details>

<details>
<summary><b>복원하기</b></summary>

1. **설정** 탭 → **"백업 및 복원"** 섹션
2. **"복원하기"** 클릭
3. 백업 파일 선택 (JSON)
4. 미리보기 확인:
   - 총 문장 개수
   - 신규 문장 개수
   - 중복 문장 개수
5. 중복 처리 방법 선택:
   - **건너뛰기** (권장): 중복 무시, 새 문장만 추가
   - **덮어쓰기**: 중복된 문장을 새 문장으로 교체
   - **모두 추가**: 중복 상관없이 전부 추가
6. **"복원하기"** 확인

> ⚠️ **중복 기준**: 날짜 + 장르 + 텍스트 모두 일치

</details>

---

### 기타 설정

<details>
<summary><b>폰트 크기 조정</b></summary>

1. **설정** 탭 → **"폰트 크기 설정"** 섹션
2. 각 요소별 슬라이더 조정:
   - 메인 문장: 8~25sp
   - 출처&작가: 6~20sp
   - 특이사항: 5~15sp
3. 실시간 미리보기 확인
4. **"기본값으로 초기화"** 버튼으로 리셋 가능

</details>

<details>
<summary><b>표시 설정</b></summary>

- 출처 표시 켜기/끄기
- 작가 표시 켜기/끄기
- 특이사항 표시 켜기/끄기
- 홈 화면 뷰 모드: 카드 뷰 / 리스트 뷰

</details>

---

## 💻 설치 방법

### 요구사항

| 항목 | 버전 |
|------|------|
| **Android Studio** | Hedgehog (2023.1.1) 이상 |
| **Minimum SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Gradle** | 8.0+ |
| **Kotlin** | 1.9.0+ |

### 빌드 방법

```bash
# 저장소 클론
git clone https://github.com/yourusername/DailyWidget.git
cd DailyWidget

# Gradle 동기화
./gradlew sync

# 빌드
./gradlew build

# 앱 설치 (디바이스/에뮬레이터 연결 후)
./gradlew installDebug
```

### 초기 데이터 설정

#### 기본 문장 데이터
- 위치: `app/src/main/assets/sentences.json`
- 앱 최초 실행 시 자동으로 Room DB에 로드됨

#### 테마 이미지 추가 (선택)

테마 이미지를 추가하려면 다음 구조로 배치:

```
app/src/main/assets/themes/
├── city/       # 도시 테마
├── sky/        # 하늘 테마
├── water/      # 바다 테마
├── nature/     # 자연 테마
├── struct/     # 건축 테마
├── paintings/  # 그림 테마
└── etc/        # 기타 테마
```

> 💡 각 폴더에 `.jpg`, `.png` 이미지 파일 추가

---

## 🛠 기술 스택

### 언어 및 프레임워크
- **Kotlin** 1.9.0+ (100% Kotlin)
- **Jetpack Compose** - 앱 내 UI
- **RemoteViews** - 위젯 UI

### 주요 라이브러리
- **Room** - 로컬 데이터베이스 (SQLite)
- **DataStore** - 설정 데이터 저장
- **Coroutines & Flow** - 비동기 처리 및 반응형 데이터
- **Coil** - 이미지 로딩
- **Kotlinx Serialization** - JSON 직렬화
- **ClassicColorPicker** - HSV 컬러 피커

### Android 컴포넌트
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **AppWidget** - 홈 화면 위젯
- **AlarmManager** - 자정 자동 업데이트

> 📖 **개발자 문서**: 아키텍처 및 구현 세부사항은 [ARCHITECTURE.md](ARCHITECTURE.md) 참조

---

## 📄 라이선스

이 프로젝트는 **MIT License** 하에 배포됩니다.

<details>
<summary><b>MIT License 전문 보기</b></summary>

```
MIT License

Copyright (c) 2025 Taeiiin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

</details>

---

## 👨‍💻 개발자

<div align="center">

### Taeiiin

[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/taeiiin)
[![Email](https://img.shields.io/badge/Email-EA4335?style=for-the-badge&logo=gmail&logoColor=white)](mailto:taein.k0103@gmail.com)

</div>

---

<div align="center">

**Built with ❤️ using Kotlin & Jetpack Compose**

</div>

# 헬싯 폰트 디자인 선택

## 권장: Pretendard

- 가독성이 좋고 한글/숫자/영어 혼용 UI에 적합한 산세리프 계열.
- 앱 분위기(크림 배경, 보라색 포인트, 코랄 보조색)와 잘 어울림.
- 추천 두께: 제목 Bold(700), 강조 SemiBold(600), 본문 Regular(400), 보조 설명 Medium(500).
- 대안: SUIT(더 단정한 느낌), Noto Sans KR(안정적이고 익숙함).

## 이번 v4 실제 상태

**안정적인 빌드를 위해 기본 Android 시스템 Sans Serif 사용**, 크기·두께·자간·색상은 `Theme.kt`에서 통일.
글꼴 리소스는 포함되어 있지 않음. 인터넷 글꼴을 런타임에 다운로드하지도 않음.

## 직접 Pretendard를 적용하려면

1. 공식 Pretendard 배포처에서 라이선스와 함께 제공되는 Android 사용 가능한 `.ttf` 파일을 직접 내려받기.
2. `app/src/main/res/font/` 폴더를 만든 뒤 적절한 폰트 파일을 넣고 이름을 `pretendard_regular.ttf`, `pretendard_medium.ttf`, `pretendard_semibold.ttf`, `pretendard_bold.ttf`로 지정.
3. `ui/theme/Theme.kt`의 `FontFamily.SansSerif` 자리에 `AppFont`를 사용. 아래 코드를 추가:

```kotlin
import androidx.compose.ui.text.font.Font
import com.healtheat.app.R

private val AppFont = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)
```

4. `AppTypography`의 `fontFamily = FontFamily.SansSerif`를 `fontFamily = AppFont`로 교체.
5. `Build > Make Project` 후 한글 잘림/줄바꿈을 확인. 기존 `Text(..., fontWeight = FontWeight.ExtraBold)`는 가장 가까운 사용 가능 두께로 합성될 수 있으므로 700으로 맞추는 것도 추천.

**주의:** 다운로드한 폰트의 라이선스 조건을 확인해 앱 배포 요건을 따르세요.

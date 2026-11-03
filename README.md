# Healtheat Android — OpenAPI 점검 및 입력창 디자인 반영 (Android only v4)

> 이 ZIP에는 Android Studio 프로젝트만 포함됩니다. **백엔드 파일은 수정하거나 포함하지 않았습니다.**
> 기존 Compose 크림/라벤더/코랄 UI는 유지하며, 백엔드 원본 `healtheat-api-main.zip`의 컨트롤러/DTO를 기준으로 연동합니다.

## 1) 중요한 로그인 조건 (원본 백엔드)

원본 `AuthController`가 `access_token`을 `Secure=true` 쿠키로 발급하고, `SecurityConfig`가 CSRF 쿠키도 `Secure=true`로 고정합니다.

따라서 `http://10.0.2.2:8080/`로 Android 에뮬레이터에서 호출하면 서버가 떠 있어도 **쿠키 인증이 작동하지 않습니다**. Android에서 Secure 쿠키를 무시하거나 HTTP로 강제 전송하는 방식은 사용하지 않았습니다.

**해결:** 백엔드 소스 수정 없이, 백엔드 개발자에게 **실제 안드로이드에서 접근 가능한 HTTPS 엔드포인트**를 요청하세요. 정상적인 TLS 인증서가 있는 HTTPS 리버스 프록시나 접근을 제한한 HTTPS 테스트 터널을 이용할 수 있습니다. 공개 테스트 터널에 실제 사용자 건강정보/비밀번호를 넣지 말고 테스트 계정만 사용하세요.

`gradle.properties`의 예시 (프로젝트 루트):

```properties
healtheatApiUrl=https://your-verified-api-domain.example/
```

또는 다음과 같이 Gradle 프로퍼티로 주입하세요:

```powershell
.\gradlew.bat assembleDebug -PhealtheatApiUrl=https://your-verified-api-domain.example/
```

**기본값**은 아직 `http://10.0.2.2:8080/`입니다. 서버 URL이 정해지기 전에도 프로젝트를 열고 UI 소스를 확인할 수 있도록 남긴 값입니다. 원본 백엔드에 대한 로그인 시에는 앱이 HTTPS 필요 안내를 표시하고 비밀번호를 HTTP로 전송하지 않습니다.

URL 뒤에는 `/`를 붙여도 되고 없어도 됩니다. Android 앱은 API base URL만 받고 내부에서 `/api/...` 경로를 붙입니다.

## 2) Android Studio 실행

1. `HealtheatAndroid` 폴더를 Android Studio에서 Open
2. Gradle Sync 및 SDK 설치 (compileSdk 35, JDK 17, AGP 8.9.2)
3. 정상적인 HTTPS 백엔드 URL을 `healtheatApiUrl`로 설정
4. Run → 회원가입 → 로그인 → 온보딩 → 식단 생성 → 재추천 → 레시피 → 기록 → 챗봇 확인
5. Build > Make Project 성공 후 테스트 APK 생성

## 3) API 연동 구현

- CSRF: `GET /api/auth/csrf` JSON의 `headerName`/`token`을 다음 쓰기 요청 헤더로 전송
- HttpOnly Secure JWT: OkHttp `CookieJar`에 메모리로만 보유, 401 발생 시 쿠키 삭제·로그인 화면 이동
- 계정: signup / login / logout / me
- 온보딩: 기본정보 GET/PUT, 선호도 GET/PATCH (`allergens`), 건강 분석/확인/조회
- 식단: 오늘 204 처리, 명시적 POST 생성, 한 끼 재추천, 레시피 상세
- 식사 기록: 조회/추천 간편 기록/직접 기록, 동일 날짜·끼니 중복 방지(409)
- 채팅: 세션 생성/조회/삭제 및 메시지 전송. 건강정보 변경은 백엔드 정책대로 별도 승인 없이 결과를 표시

## 4) 제한 사항

- 이 환경에 Android SDK/Gradle 다운로드가 없어 **전체 APK 빌드·실제 HTTPS 서버 통신은 검증하지 못했습니다.**
- 검증되지 않은 AI 응답/식사 기록을 성공한 것으로 꾸미는 Mock 대체는 사용하지 않습니다.
- CSRF, JWT, 쿠키는 반드시 원본 백엔드와 같은 HTTPS 호스트에서 테스트합니다.
- 프로젝트 내에 키·비밀번호를 넣지 마세요. 운영용 URL과 서비스 비밀키는 구분하세요.

`BACKEND_DEVELOPER_REQUESTS.md`에는 백엔드 개발자에게 보낼 **확인 및 요청사항만** 정리했습니다. 코드는 수정하지 않았습니다.

## 5) Swagger 및 폰트

- `SWAGGER_AUDIT.md`: 원본 Swagger/Java DTO 대조와 백엔드 담당자 요청사항.
- `FONT_GUIDE.md`: Pretendard 추천과 직접 리소스를 적용하는 방법. 폰트 파일 자체는 포함하지 않으며 이번 빌드는 기본 시스템 Sans Serif 사용.
- 입력창 색상은 `ui/theme/Theme.kt`의 `healtheatTextFieldColors()`에서 한번에 관리.

- `api_contract/healtheat-openapi.json`: 전달받은 Swagger(OpenAPI 3.1) 원본 사본.

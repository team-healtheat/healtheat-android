# 백엔드 담당자 전달사항 (백엔드 코드는 수정하지 않음)

## 1. 반드시 필요한 항목 — Android 실제 API 연동용 HTTPS 주소

현재 전달된 `healtheat-api-main.zip` 기준:

- `AuthController.accessCookie()`가 인증 쿠키를 `Secure(true)`로 발급합니다.
- `SecurityConfig`에서 `XSRF-TOKEN` 쿠키도 `secure(true)`로 발급합니다.
- 따라서 에뮬레이터의 `http://10.0.2.2:8080` 및 일반 HTTP 테스트 주소는 **안드로이드 정상 인증 테스트에 적합하지 않습니다**.

백엔드 **소스 변경을 요청하는 것은 아닙니다.** 원본 Spring Boot를 HTTPS 역방향 프록시 뒤에서 실행하거나, 접근 제어된 HTTPS 테스트 주소로 연결할 수 있도록 부탁드립니다.

**받아야 할 정보**

1. 휴대폰/에뮬레이터에서 접근 가능한 HTTPS API Base URL (예: `https://api.example.com/`)
2. HTTPS 인증서가 Android에서 정상 신뢰되는지 여부 (자체서명·인증서 경고가 없어야 함)
3. API 서버 실행 상태와 포트, Swagger 또는 `/v3/api-docs` 응답 가능 여부
4. 테스트 계정(실제 개인 건강정보 미포함), 식단 생성용 AI 환경변수 설정 여부
5. 로그인 후 같은 HTTPS 호스트로 쿠키가 유지되고 `/api/users/me`가 200을 반환하는지 확인

앱은 HTTPS URL만 연결하며, 기존 Spring Boot 코드 변경을 전제로 하지 않습니다. **네이티브 Android의 OkHttp 요청은 브라우저 CORS 정책의 적용 대상이 아닙니다.**

## 2. 백엔드 상태 확인 요청 (현재 소스 기준)

- `POST /api/meal-plans/generate`와 `GET /api/meal-plans/today`가 실제 OpenAI 키·사용 가능한 모델을 사용하여 응답하는지
- `POST /api/meal-plans/{planId}/meals/{mealType}/regenerate`가 3끼 전체 식단을 반환하는지
- `GET /api/recipes/{recipeId}`의 레시피 생성 결과가 실제로 저장/조회되는지
- `POST /api/chat/sessions`와 `POST /api/chat/messages`가 사용 가능한 AI 모델로 응답하는지
- 개발 문서에 적힌 식단 30일 자동정리 기능은 원본 ZIP에 통합되었는지 (제공된 ZIP에는 식사 기록/채팅 정리 코드는 있으나 식단 정리는 별도 PR 병합 전이라는 설명이 있음)
- **전체 식단 재생성** `POST /api/meal-plans/{planId}/regenerate`는 이번 원본 `MealPlanController`에서 찾을 수 없었습니다. 앱은 해당 엔드포인트를 호출하지 않습니다. 필요 시 API 제공 여부를 결정 부탁드립니다.

## 3. 안드로이드 연동 시 기대하는 흐름

1. `GET /api/auth/csrf` -> `token`/`headerName`, XSRF-TOKEN 쿠키 수신
2. `POST /api/auth/signup`, `POST /api/auth/login` -> 201/200, access_token 쿠키
3. `GET /api/users/me` -> 200 (로그인 상태 확인)
4. `GET/PUT /api/users/me/profile` / `GET/PATCH /api/users/me/preferences` / `POST/PUT/GET /api/health-profiles`
5. `GET /api/meal-plans/today` -> 204 또는 Plan; 없다면 명시적 `POST /api/meal-plans/generate`
6. 재추천/레시피/식사 기록/채팅 연결

**지금 필요한 회신은 HTTPS 테스트 URL과 실제 실행 가능한 API 상태입니다.** 서버 원본 코드를 프론트에서 임의로 수정하거나 배포하지 않습니다.

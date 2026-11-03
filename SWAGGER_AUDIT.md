# HealthEat OpenAPI 3.1 대조 결과 — Android 전용 v4

비교 기준: 사용자 제공 `healtheat-openapi.json`, `healtheat-api-main.zip` 원본 Java 코드.
본 프로젝트 ZIP에는 **안드로이드 소스만 포함**하며 백엔드는 수정하지 않았음.

## 확인한 요청·응답 규격

- 인증: `GET /api/auth/csrf` → `headerName`, `token`; `POST /api/auth/signup` (201); `POST /api/auth/login` (200) → HttpOnly/Secure `access_token` 쿠키; `POST /api/auth/logout` (204). 로그인 전/후 쓰기 요청은 먼저 CSRF 값을 조회해 `X-XSRF-TOKEN`으로 전달.
- 기본 프로필: `GET/PUT /api/users/me/profile` → `age`, `gender`.
- 사용자 설정: `GET/PATCH /api/users/me/preferences` → `allowDiningOut`, `likedFoods`, `dislikedFoods`, `dietaryPreferences`, `allergyStatus`, `allergens`.
- 건강 분석: `POST /api/health-profiles/analyze` → `factors` 후보, 확인 후 `PUT /api/health-profiles` → `factors` 전체 목록 (기존 목록 중 누락된 요소는 비활성화됨). `factorId`는 마스터 ID임.
- 식단: `GET /api/meal-plans/today` → HTTP 204 또는 `Plan`; 사용자가 명시적으로 `POST /api/meal-plans/generate`; `Plan.meals`에 아침/점심/저녁과 음식 `Item.recipe.id` 포함.
- 재추천: `POST /api/meal-plans/{planId}/meals/{mealType}/regenerate` → `Plan` 전체 3끼. `additionalRequest`는 선택.
- 레시피: `GET /api/recipes/{recipeId}` → `RecipeDetail`, `ingredients[]` (`name`, `quantity`, `unit`), `steps[]` 문자열.
- 실제 식사 기록: `GET /api/meal-records` → 배열; `POST /api/meal-records` → `DirectRequest`; `POST /api/meal-records/from-recommendation` → `recommendationId`.
- 챗봇: `POST /api/chat/sessions` → `Session.id`, `POST /api/chat/messages` → `sessionId`, `reply`, `intent`, `updatedHealthProfile?`, `updatedMealPlan?`. 종료는 `DELETE /api/chat/sessions/{sessionId}` (204).

## 이번 Android 수정

1. 이메일, 비밀번호, 온보딩, 식사 기록, 챗봇의 `OutlinedTextField` 입력글자/placeholder/테두리/커서 색상을 헬싯 팔레트에 맞춰 공통 적용.
2. 이메일·비밀번호 키보드 유형 및 회원가입 비밀번호 길이 검증 보완.
3. 건강 프로필 `PUT`가 전체 교체 계약인 점을 고려해 기존 활성·비활성 요인 보존, **요인 이름과 타입 복합키**로 병합. 기존 요인의 `originalText`를 새로운 입력 내용으로 덮어쓰지 않도록 보완.
4. 기존 Retrofit 경로, CSRF, 쿠키 인증, 204/409/410 상태처리 유지.

## 백엔드 담당자에게 전달할 사항 (코드 수정은 안 함)

1. 인증/CSRF 쿠키가 Secure이므로 Android에서 접속 가능한 HTTPS 주소, 정상 신뢰된 인증서 필요. `/v3/api-docs` 안의 서버 URL `https://example.invalid`는 샘플이며 실제 주소가 아님.
2. Swagger 스키마 이름 충돌: `GET /api/health-profiles` 응답이 사용자 기본정보 `ProfileResponse(age,gender)`로 표시되고, `GET/PATCH /api/users/me/preferences` 응답이 챗봇 `Response(sessionId,reply...)`로 표시됨. **원본 Java 응답 DTO는 각각 `HealthDtos.ProfileResponse(factors)`와 `PreferenceDtos.Response(allowDiningOut,...,allergens)`이므로 Swagger 문서 스키마 이름 유일성 처리 필요.** Android는 원본 Java DTO 기준으로 작성.
3. Swagger의 `GET /api/auth/csrf`에 `csrf`라는 필수 query parameter가 나오는 것은 컨트롤러 실제 시그니처의 CSRF 주입 객체가 문서화된 것으로 추정. 실제 컨트롤러는 클라이언트 query parameter를 요구하지 않음. Swagger 표기 수정 권장.
4. 오늘 식단 없는 상태 204 반환은 `GET /api/meal-plans/today` 설명에 있으나 `responses`에 204가 빠져 있음. 문서 보완 권장.
5. 식단 생성/재추천/채팅의 실제 AI 응답과 로그인 쿠키 E2E 테스트는 HTTPS 테스트 서버에서 검증 필요.

## 검증 상태

- Swagger 19개 path의 경로·메서드와 Android Retrofit 선언 정적 비교 및 원본 Java 컨트롤러 확인.
- Android Gradle 전체 빌드/실기기/서버 통신 미실행: 현재 작업 환경에 Android SDK/Gradle 실행 환경이 없음. 사용자 PC에서 `Build > Make Project`, 에뮬레이터/실기기 테스트 필요.

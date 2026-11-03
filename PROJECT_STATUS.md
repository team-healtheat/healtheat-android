# Android-only v4 작업 현황 — Swagger 반영 / 입력창 디자인 정리

- 기준: `healtheat-openapi.json`, 원본 `healtheat-api-main.zip`, 기존 Android-only v3
- 변경 범위: **Android 프로젝트 파일 및 문서만**; 백엔드 원본 수정 없음
- Swagger API 19개 경로·24개 HTTP operation 모두 Android Retrofit 인터페이스와 정적 매칭 확인
- 로그인/온보딩/식사기록/챗봇의 `OutlinedTextField` 11곳 글자·안내·테두리·커서 색상 공통 적용
- 회원가입 비밀번호 8~72자 사전 검증 및 이메일/비밀번호 키보드 입력 유형 보완
- 건강정보 PUT 전체 교체 정책 반영: 기존 건강 요인/비활성 상태 보존, 이름+타입 기준 병합, 기존 원문 덮어쓰기 방지
- CSRF/쿠키/204·409·410 처리 및 기존 크림/라벤더/코랄 화면 구조 유지
- `api_contract/healtheat-openapi.json`: 전달받은 OpenAPI 3.1 사본(스펙 대비용)
- `SWAGGER_AUDIT.md`: API 점검·백엔드 개발자 전달 사항
- `FONT_GUIDE.md`: Pretendard 추천 및 직접 적용법. 폰트 바이너리 미포함, 시스템 Sans Serif 사용
- Android SDK/Gradle을 실행할 수 없는 환경이므로 **v4 실제 Gradle 빌드·에뮬레이터·HTTPS API E2E 테스트 미검증**
- 작동 검증에 필요한 것: 정상 TLS의 HTTPS API URL, 테스트 계정, 실제 AI API 설정, 사용자 PC Android Studio Build + 테스트

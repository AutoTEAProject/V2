# frontend

React + TypeScript + Vite SPA. `../backend`(Spring Boot REST API)와 통신합니다.

## 개발 환경 준비

```bash
cp .env.example .env
# .env에 VITE_API_BASE_URL, VITE_GOOGLE_CLIENT_ID 채우기
npm install
npm run dev
```

`VITE_GOOGLE_CLIENT_ID`는 백엔드(`infra/.env`)의 `GOOGLE_CLIENT_ID`와 같은 Google OAuth 클라이언트 ID를 써야 합니다.

## 화면 구성

- `/login` — Google 로그인
- `/` — 프로젝트(케이스) 목록/생성
- `/projects/:caseId` — 프로젝트 상세
  - **실행** 탭: input.xlsx/input.rep 업로드 → 장치 파싱 → (설정 탭에서 조정) → 계산 실행 → 결과 xlsx 다운로드
  - **장치비 · Utility 설정** 탭: 장치 타입별 기본값 + 개별 장치 오버라이드로 원가 수식 선택, 계산할 utility 선택, 장치비 계산 제외 지정
- `/formulas` — 전역 수식 라이브러리(K1/K2/K3 계수) 추가/수정/삭제

## 백엔드가 제공하는 API

- `POST /api/auth/google` — Google ID 토큰으로 로그인, 자체 JWT 발급
- `POST /api/cases` / `GET /api/cases` / `GET /api/cases/{id}` — 케이스(프로젝트) CRUD
- `POST /api/cases/{caseId}/runs/draft` (multipart: `xlsxFile`, `repFile`) — 업로드 + 장치 파싱
- `POST /api/cases/{caseId}/runs/{runId}/execute` — 설정 반영해 실제 계산 실행
- `GET /api/cases/{caseId}/runs` / `GET /api/runs/{id}` — 실행 이력/상태 조회
- `GET /api/runs/{id}/result` — 결과 파일(xlsx) 다운로드
- `GET/PUT /api/cases/{caseId}/equipment-settings` — 장치비/utility 설정
- `GET/POST/PUT/DELETE /api/formulas` — 전역 수식 라이브러리

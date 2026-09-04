# Railway 배포

`autotea-v2` 프로젝트(워크스페이스: Duckgii's Projects)에 이미 배포되어 있습니다.

- 프론트: https://frontend-production-1cf6c.up.railway.app
- 백엔드: https://backend-production-1070c.up.railway.app (`/actuator/health`)
- python-engine: private networking으로만 열려있음(`python-engine.railway.internal:8000`), 공개 URL 없음
- Postgres: Railway 플러그인

backend와 python-engine은 디스크를 공유하지 않습니다(파일을 HTTP 요청/응답으로 직접 주고받고, 업로드된 input과 계산 결과는 Postgres에 저장). Railway처럼 서비스끼리 볼륨을 공유할 수 없는 환경 전제로 만든 구조입니다.

## 구성은 코드로 관리됨

`.railway/railway.ts`가 이 프로젝트의 서비스 토폴로지(소스 저장소, root directory, Dockerfile 빌드, healthcheck, DB/서비스 간 참조 변수)를 정의합니다. 비밀값(JWT_SECRET, GOOGLE_CLIENT_ID, CORS_ALLOWED_ORIGINS, VITE_* 등)은 이 파일에 넣지 않고 `railway variable set`으로 따로 설정했습니다(파일에 평문으로 안 남게 하려고).

구조를 바꾸고 싶으면(서비스 추가, root directory 변경 등):
```bash
railway config plan    # .railway/railway.ts 기준으로 뭐가 바뀌는지 미리보기
railway config apply --yes
```

## 재현하려면(새 프로젝트로 처음부터)

```bash
railway login
railway init --name <프로젝트명>
railway config apply --yes        # .railway/railway.ts대로 Postgres+3개 서비스 생성

railway variable set JWT_SECRET="$(openssl rand -base64 48)" --service backend --skip-deploys
railway variable set GOOGLE_CLIENT_ID=<Google OAuth 클라이언트 ID> --service backend --skip-deploys

railway domain --service backend    # 백엔드 공개 URL 발급
railway domain --service frontend   # 프론트 공개 URL 발급

# 위 두 domain 명령 결과로 나온 URL을 아래에 채워넣기
railway variable set CORS_ALLOWED_ORIGINS=https://<frontend 도메인> --service backend --skip-deploys
railway variable set VITE_API_BASE_URL=https://<backend 도메인> --service frontend --skip-deploys
railway variable set VITE_GOOGLE_CLIENT_ID=<Google OAuth 클라이언트 ID> --service frontend --skip-deploys

railway redeploy --service backend --yes
railway redeploy --service frontend --yes   # Vite는 빌드 시점에 값을 굽기 때문에 재배포(재빌드) 필요
```

마지막으로 Google Cloud Console → OAuth 클라이언트 → **승인된 자바스크립트 원본**에 frontend 도메인을 추가해야 로그인이 됩니다.

## 확인 방법

```bash
railway status                              # 서비스별 상태/도메인
curl https://<backend 도메인>/actuator/health
```

실제 프로젝트 파일로 로그인 → 업로드/파싱 → 장치비·Utility 설정 → 계산 실행 → 결과 다운로드까지 curl로 end-to-end 확인 완료(2026-09-04).

## 참고: 로컬 docker-compose로 미리 검증하기

```bash
cp infra/.env.example infra/.env   # JWT_SECRET, GOOGLE_CLIENT_ID 채우기
make   # 루트 Makefile, docker compose up -d --build (db+python-engine+backend+frontend 전부)
```

`infra/docker-compose.yml`도 Railway와 동일하게 backend↔python-engine이 디스크 공유 없이 HTTP로만 통신하도록 되어 있어서, 여기서 정상 동작하면 Railway에서도 같은 방식으로 동작합니다.

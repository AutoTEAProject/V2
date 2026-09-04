# Railway 배포

`autotea-v2` 프로젝트(워크스페이스: Duckgii's Projects)에 이미 배포되어 있습니다.

- 프론트: https://frontend-production-1cf6c.up.railway.app
- 백엔드: https://backend-production-1070c.up.railway.app (`/actuator/health`)
- python-engine: private networking으로만 열려있음(`python-engine.railway.internal:8000`), 공개 URL 없음
- Postgres: Railway 플러그인

backend와 python-engine은 디스크를 공유하지 않습니다(파일을 HTTP 요청/응답으로 직접 주고받고, 업로드된 input과 계산 결과는 Postgres에 저장). Railway처럼 서비스끼리 볼륨을 공유할 수 없는 환경 전제로 만든 구조입니다.

## 구성은 코드로 관리됨

`.railway/railway.ts`가 이 프로젝트의 서비스 토폴로지(소스 저장소, root directory, Dockerfile 빌드, healthcheck, DB/서비스 간 참조 변수)를 정의합니다.

**중요: 이 도구는 Terraform처럼 파일 = 전체 desired state로 취급합니다.** 파일에 안 적힌 변수나 리소스는 `apply` 시 그냥 무시되는 게 아니라 **삭제 대상으로 잡힙니다.** 그래서 비밀값(JWT_SECRET, GOOGLE_CLIENT_ID, CORS_ALLOWED_ORIGINS, VITE_* 등)은 파일에 평문으로 안 넣는 대신, `railway variable set`으로 따로 설정한 뒤 `railway config pull --force`로 다시 당겨와서 `preserve()`(값은 안 적히고 "이미 설정된 값 유지"만 표시)로 반영해뒀습니다. 즉 지금 커밋된 파일에는 실제 비밀값이 하나도 없습니다.

구조를 바꾸고 싶으면:
```bash
npm install railway   # 저장소 루트에서, .railway/railway.ts 실행에만 필요(커밋 대상 아님)
railway config plan   # .railway/railway.ts 기준으로 뭐가 바뀌는지 미리보기 (안전, 아무것도 안 바꿈)
railway config apply --yes
```
`railway variable set`으로 뭔가 직접 바꿨다면, 다음에 `apply`하기 전에 반드시 `railway config pull --force`로 파일을 먼저 최신 상태와 동기화하세요 — 안 그러면 그 변경이 `apply` 때 날아갑니다.

새 commit을 배포하려면 push만으로는 자동 반영되지 않고, 서비스별로 다시 빌드를 트리거해야 합니다:
```bash
railway redeploy --service backend --from-source --yes
railway redeploy --service python-engine --from-source --yes
railway redeploy --service frontend --from-source --yes   # --from-source 빼면 예전 빌드를 재배포할 뿐 최신 커밋을 안 가져옴
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

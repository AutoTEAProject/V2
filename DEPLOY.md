# Railway 배포

이 저장소는 모노레포라서, Railway 프로젝트 하나 안에 서비스 4개(Postgres 플러그인 + backend + python-engine + frontend)를 만들고 같은 GitHub 저장소를 가리키되 서비스마다 **Root Directory**만 다르게 잡는 방식으로 배포합니다.

backend와 python-engine은 더 이상 디스크를 공유하지 않습니다(파일을 HTTP 요청/응답으로 직접 주고받고, 업로드된 input과 계산 결과는 Postgres에 저장). 그래서 Railway처럼 서비스끼리 볼륨을 공유할 수 없는 환경에서도 그대로 돌아갑니다.

## 0. 사전 준비

- Railway 계정, 이 GitHub 저장소에 대한 접근 권한
- Google Cloud Console에 만들어둔 OAuth 클라이언트 ID (로컬 개발 때 쓰던 것과 같은 걸 써도 되고, 배포용으로 새로 만들어도 됨)

## 1. Postgres 추가

Railway 프로젝트에서 **New → Database → PostgreSQL** 로 추가합니다. 이 플러그인이 `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` 환경변수를 자동으로 제공합니다(아래에서 다른 서비스가 이 값들을 참조합니다).

## 2. python-engine 서비스

**New → GitHub Repo** 로 이 저장소를 연결하고:
- Settings → Root Directory: `python-engine`
- Dockerfile이 있으니 자동으로 인식됩니다(`python-engine/railway.toml`에 healthcheck 경로도 설정해둠)
- Variables: `CALC_TIMEOUT_SECONDS` (선택, 기본 300)
- **Networking**에서 Private Networking을 켜서 다른 서비스가 `http://<service>.railway.internal:<PORT>` 로 접근할 수 있게 합니다. Public Domain은 필요 없습니다(백엔드만 이 서비스를 호출함).

## 3. backend 서비스

같은 저장소로 서비스 하나 더 추가:
- Root Directory: `backend`
- Variables (Railway의 `${{ServiceName.VAR}}` 참조 문법 사용):
  ```
  DB_HOST=${{Postgres.PGHOST}}
  DB_PORT=${{Postgres.PGPORT}}
  DB_NAME=${{Postgres.PGDATABASE}}
  DB_USERNAME=${{Postgres.PGUSER}}
  DB_PASSWORD=${{Postgres.PGPASSWORD}}
  PYTHON_ENGINE_URL=http://${{python-engine.RAILWAY_PRIVATE_DOMAIN}}:${{python-engine.PORT}}
  PYTHON_ENGINE_TIMEOUT=300
  JWT_SECRET=<openssl rand -base64 48 등으로 생성한 무작위 문자열>
  GOOGLE_CLIENT_ID=<Google OAuth 클라이언트 ID>
  CORS_ALLOWED_ORIGINS=<3단계에서 만들 프론트 URL, 일단 아무 값이나 넣고 나중에 갱신>
  ```
  (서비스 이름이 다르면 `${{...}}` 안의 이름도 실제 서비스 이름으로 바꿔주세요.)
- **Networking → Generate Domain**으로 공개 URL을 만듭니다(프론트가 이 URL로 API를 호출함).
- `spring.datasource`가 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`로 접속 URL을 조립하므로 Railway의 `DATABASE_URL` 통짜 값은 안 씁니다.

## 4. frontend 서비스

같은 저장소로 서비스 하나 더:
- Root Directory: `frontend`
- Dockerfile이 빌드 인자로 `VITE_API_BASE_URL`, `VITE_GOOGLE_CLIENT_ID`를 받습니다. Railway Variables에 아래처럼 넣고 **Build Arguments로도 동일하게** 설정해야 합니다(Vite는 빌드 시점에 값을 번들에 박아 넣기 때문에 런타임 env로는 안 됩니다):
  ```
  VITE_API_BASE_URL=https://<3단계 backend의 공개 URL>
  VITE_GOOGLE_CLIENT_ID=<Google OAuth 클라이언트 ID>
  ```
- **Networking → Generate Domain**으로 공개 URL을 만듭니다.

## 5. 마무리: CORS와 OAuth 되돌리기

1. 4단계에서 나온 프론트 URL을 backend 서비스의 `CORS_ALLOWED_ORIGINS`에 다시 설정하고 재배포합니다.
2. Google Cloud Console → OAuth 클라이언트 → **승인된 자바스크립트 원본**에 프론트 URL을 추가합니다.

## 확인

1. 프론트 URL 접속 → Google 로그인
2. 프로젝트 생성 → input.xlsx/input.rep 업로드 → 장치 파싱 확인
3. 장치비/Utility 설정 확인 → 계산 실행 → 성공 시 결과 다운로드까지 확인

## 참고: 로컬 docker-compose로 미리 검증하기

```bash
cp infra/.env.example infra/.env   # JWT_SECRET, GOOGLE_CLIENT_ID 채우기
make   # 루트 Makefile, docker compose up -d --build (db+python-engine+backend+frontend 전부)
```

`infra/docker-compose.yml`은 Railway와 동일하게 backend↔python-engine이 디스크 공유 없이 HTTP로만 통신하도록 되어 있어서, 여기서 정상 동작하면 Railway에서도 같은 방식으로 동작합니다.

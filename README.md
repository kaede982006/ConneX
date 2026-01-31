# ConneX

ConneX는 FastAPI 기반의 안전한 실시간 채팅 백엔드와 Android 네이티브 클라이언트로 구성된 프로젝트입니다.

## 주요 기능

- JWT 기반 인증 및 권한 관리
- 채널/룸 기반 실시간 메시징(WebSocket)
- PostgreSQL + SQLAlchemy(Async) 기반 데이터 계층
- Alembic 마이그레이션

## 아키텍처

- **Backend**: Python 3.11+, FastAPI, SQLAlchemy Async, PostgreSQL
- **Frontend**: Android (Kotlin/Jetpack Compose)
- **통신**: REST API + WebSocket

## 빠른 시작 (Backend)

### 1) 설치

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

### 2) DB 준비

```bash
sudo systemctl start postgresql
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE connex;"
```

### 3) 환경 변수

```bash
cp .env.example .env
```

### 4) 개발 서버 실행

```bash
./scripts/run_dev.sh
```

- API: `http://localhost:8000/api/v1`
- 문서: `http://localhost:8000/docs`

## Frontend

`frontend/` 디렉터리를 Android Studio에서 열고 Gradle 동기화 후 실행합니다.

## 개발 팁

- DB/세션 관련 설정은 `backend/src/connex/settings.py`에서 확인할 수 있습니다.
- API 라우팅은 `backend/src/connex/api/routes/`에 있습니다.

# PicSel Backend v2

> PicSel은 온라인 쇼핑 시 상품 페이지에서 **최저가**와 **카드 혜택**을 한 화면에서 분석·제공하는 Chrome 확장 프로그램 및 웹 대시보드 서비스입니다.
> 본 저장소는 PicSel의 백엔드 API 서버 v2입니다.

---

## Tech Stack

| 분류      | 기술                                  |
| --------- | ------------------------------------- |
| Language  | Java 21                               |
| Framework | Spring Boot 3.3.5                     |
| ORM       | Spring Data JPA + Hibernate           |
| Database  | MySQL 8                               |
| Cache     | Redis                                 |
| Security  | Spring Security + JWT (jjwt 0.12.6)   |
| Crawler   | Jsoup 1.17.2                          |
| API Docs  | Springdoc OpenAPI (Swagger UI)        |
| Build     | Gradle                                |

---

## 주요 기능

### Chrome 확장 연동

- **최저가 검색**: 상품명으로 다나와 최저가를 실시간 조회하고 현재 페이지의 가격과 자동 비교
- **카드 혜택 비교**: 가맹점과 결제 금액 기준으로 모든 활성 카드 혜택을 비교하고 TOP 3 추천

### 웹 대시보드

- 사용자별 최근 가격 검색 이력 조회
- 누적 절약 가능 금액 집계
- 전체 사용자 기반 인기 상품 트렌드
- 활성 카드 혜택 탐색

### 인증

- 이메일/비밀번호 회원가입 · 로그인
- JWT Access Token (1시간) + Refresh Token (7일) 발급
- 로그아웃 시 Redis 블랙리스트 기반 Access Token 즉시 무효화
- Google · Kakao · Naver OAuth 연동 (stub)

---

## 가격 검색 파이프라인

```text
요청 수신
    │
    ├─ Hard Skip 체크 (연속 3회 실패한 상품 → 즉시 반환)
    │
    ├─ Redis 캐시 조회 (TTL 6시간)
    │       └─ HIT → 즉시 반환
    │
    ├─ DB 영구 캐시 조회
    │       └─ HIT → 즉시 반환
    │
    ├─ Negative 캐시 체크 (이전에 결과 없음으로 기록된 상품)
    │
    ├─ FastPath — HTTP + Jsoup 크롤링 (최대 8초)
    │       └─ 성공 → Redis·DB 캐시 저장 후 반환
    │
    └─ 실패 기록 + Negative 캐시 등록 → 에러 반환
```

---

## API 명세

모든 API 경로에는 `/api` 접두사가 자동으로 붙습니다.

| 그룹      | 경로                                      | 인증   | 설명                       |
| --------- | ----------------------------------------- | ------ | -------------------------- |
| Auth      | `POST /api/auth/register`                 | 불필요 | 회원가입                   |
| Auth      | `POST /api/auth/login`                    | 불필요 | 로그인                     |
| Auth      | `POST /api/auth/refresh`                  | 불필요 | 토큰 재발급                |
| Auth      | `POST /api/auth/logout`                   | Bearer | 로그아웃                   |
| Price     | `POST /api/v1/price/search`               | 불필요 | 최저가 검색                |
| Price     | `GET /api/v1/price/statistics`            | 불필요 | 검색 통계                  |
| Price     | `GET /api/v1/price/popular`               | 불필요 | 인기 검색어                |
| Benefits  | `GET /api/benefits/compare`               | 불필요 | 전체 카드 혜택 비교        |
| Benefits  | `GET /api/benefits/top3`                  | 불필요 | 최적 카드 TOP 3 추천       |
| Benefits  | `POST /api/benefits/extract-and-compare`  | 불필요 | HTML에서 혜택 추출 후 비교 |
| Benefits  | `GET /api/benefits/offers`                | 불필요 | 활성 혜택 목록             |
| Dashboard | `GET /api/dashboard/recent-searches`      | Bearer | 내 검색 이력               |
| Dashboard | `GET /api/dashboard/savings`              | Bearer | 내 절약 금액               |
| Dashboard | `GET /api/dashboard/popular-products`     | 불필요 | 인기 상품 트렌드           |
| Dashboard | `GET /api/dashboard/benefit-offers`       | 불필요 | 혜택 탐색                  |
| User      | `GET /api/users/me`                       | Bearer | 내 정보 조회               |
| User      | `PUT /api/users/me`                       | Bearer | 내 정보 수정               |

> Swagger UI: `http://localhost:8082/swagger`

---

## 프로젝트 구조

```text
src/main/java/com/picsel/backend_v2/
├── config/          # Spring 설정 (Security, Redis, OpenAPI, API 접두사)
├── controller/      # REST 컨트롤러
├── domain/          # JPA 엔티티
│   └── ext/         # 확장 기능 엔티티 (PriceCache, SearchLog, SearchFailure)
├── dto/             # 요청/응답 DTO
├── exception/       # 전역 예외 처리
├── repository/      # Spring Data JPA 레포지토리
├── security/        # JWT 필터 및 UserDetails
└── service/         # 비즈니스 로직
    └── ext/         # 가격 검색 파이프라인 (Orchestrator, FastPathExecutor, CacheService)
```

---

## 시작하기

### 사전 요구사항

- Java 21
- MySQL 8
- Redis

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/picsel?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Encryption
ENCRYPTION_KEY=your_encryption_key

# Server
SERVER_PORT=8082

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Crawler (선택)
CACHE_TTL=21600
CRAWLER_TOTAL_BUDGET_MS=12000
CRAWLER_FASTPATH_TIMEOUT_MS=8000
```

### 실행

```bash
./gradlew bootRun
```

### 빌드

```bash
./gradlew build
java -jar build/libs/backend-v2-0.0.1-SNAPSHOT.jar
```

---

## 보안

- **Stateless 세션**: JWT 기반 인증, 서버 세션 미사용
- **토큰 블랙리스트**: 로그아웃 시 Redis에 Access Token 등록, 만료 전 재사용 차단
- **보안 헤더**: HSTS, X-Frame-Options (DENY), X-Content-Type-Options, XSS Protection 적용
- **CORS**: 허용 도메인 명시적 제한 (`picsel.kr`, `picsel.vercel.app`, `localhost`)
- **BCrypt**: 비밀번호 단방향 해시 저장

---

## 관련 저장소

| 저장소            | 설명                      |
| ----------------- | ------------------------- |
| FrontEnd-V2       | 웹 대시보드 (React)       |
| Chrome Extension  | Chrome 확장 프로그램      |

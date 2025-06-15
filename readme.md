# 실시간 채팅 애플리케이션 (Chat App)

## 프로젝트 소개
Spring Boot 3.4.3 기반의 실시간 채팅 애플리케이션으로, Domain-Driven Design(DDD)과 Clean Architecture 원칙을 적용하여 개발되었습니다. WebSocket을 활용한 실시간 메시지 전송과 RESTful API를 통한 채팅방 관리 기능을 제공합니다.

## 기술 스택

### Core
- **Java 21** - 최신 LTS 버전 사용
- **Spring Boot 3.4.3** - Web, WebSocket, Data JPA, Validation
- **Spring Kafka** - 이벤트 기반 메시징
- **MySQL** - 프로덕션 데이터베이스
- **H2 Database** - 테스트 환경
- **Redis** - 세션 관리 및 캐싱

### 보안 & 인증
- **JWT** - 토큰 기반 인증 (쿠키 방식)
- **Spring Validation** - 입력 검증

### 테스트 & 품질
- **JUnit 5** - 단위 및 통합 테스트
- **Mockito** - 모킹 프레임워크
- **TestContainers** - 통합 테스트 환경
- **p6spy** - SQL 쿼리 분석 및 성능 모니터링

### 문서화
- **SpringDoc OpenAPI 2.7.0** - Swagger UI 자동 생성
- **API 문서**: http://localhost:8080/swagger-ui.html

## 주요 기능
- 🔐 **인증/인가**: JWT 기반 사용자 인증
- 💬 **실시간 채팅**: WebSocket(STOMP)을 통한 실시간 메시지 전송
- 🏠 **채팅방 관리**: 생성, 참여, 나가기, 삭제
- 👥 **사용자 관리**: 회원가입, 로그인, 프로필 조회
- 📜 **메시지 히스토리**: 페이지네이션 지원
- 📊 **성능 최적화**: N+1 쿼리 해결, 배치 조회 최적화

## 아키텍처 특징
- **DDD(Domain-Driven Design)**: 도메인 중심 설계
- **Clean Architecture**: 의존성 역전 원칙 적용
- **Immutable Domain Objects**: 불변 도메인 객체 사용
- **Domain Services**: 복잡한 비즈니스 로직 캡슐화
- **Event-Driven**: Kafka를 활용한 이벤트 기반 아키텍처

## API 문서

### 인증 API
- `POST /api/auth/login` - 사용자 로그인
- `POST /api/auth/signup` - 사용자 회원가입
- `POST /api/auth/validate` - JWT 토큰 유효성 검증

### 채팅방 API
- `GET /api/rooms` - 전체 채팅방 목록 조회
- `GET /api/rooms/me?userId={userId}` - 사용자가 참여한 채팅방 목록 조회
- `GET /api/rooms/{id}` - 특정 채팅방 상세 정보 조회
- `POST /api/rooms` - 채팅방 생성
- `POST /api/rooms/{id}/join` - 채팅방 참여
- `DELETE /api/rooms/{id}` - 채팅방 삭제

### 사용자 API
- `GET /api/users` - 모든 사용자 조회
- `GET /api/users/{id}` - 특정 사용자 조회
- `GET /api/users/username/{username}` - 사용자명으로 사용자 조회
- `PATCH /api/users/{id}/status` - 사용자 상태 업데이트

### 메시지 API
- `GET /api/messages/room/{roomId}` - 채팅방 메시지 조회 (페이지네이션)
- `GET /api/messages/room/{roomId}/recent` - 채팅방 최근 메시지 조회
- `PATCH /api/messages/{id}/status` - 메시지 상태 업데이트

### 성능 테스트 API (개발용)
- `POST /api/performance/generate-test-data` - 테스트 데이터 생성
- `POST /api/performance/quick-performance-test` - 빠른 성능 테스트
- `POST /api/performance/compare-performance` - 최적화 전후 성능 비교
- `POST /api/performance/demonstrate-n1-queries` - N+1 쿼리 문제 시연
- `GET /api/performance/statistics` - 데이터 통계 조회

### WebSocket 엔드포인트
- `ws://host/ws` - 웹소켓 연결 엔드포인트
- `/app/message.send` - 메시지 전송
- `/app/room.enter` - 채팅방 입장
- `/app/room.leave` - 채팅방 퇴장
- `/topic/room/{roomId}` - 채팅방별 메시지 구독
- `/queue/errors` - 개인 오류 알림

## 프로젝트 구조
```
src
├── main
│   ├── java
│   │   └── com.example.chatapp
│   │       ├── config        - 애플리케이션 설정
│   │       ├── controller    - REST/WebSocket 컨트롤러
│   │       ├── domain        - 도메인 모델
│   │       ├── dto           - 데이터 전송 객체
│   │       ├── exception     - 예외 처리
│   │       ├── repository    - 데이터 액세스 계층
│   │       └── service       - 비즈니스 로직
│   └── resources
└── test                      - 단위 테스트
```

## 설치 및 실행
```bash
# 프로젝트 클론
git clone https://github.com/username/chat-app.git

# 디렉토리 이동
cd chat-app

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

## 테스트
```bash
# 모든 테스트 실행
./gradlew test
```

## 개발 환경 설정
- 프론트엔드 개발 서버: `http://localhost:3000`
- 백엔드 API 서버: `http://localhost:8080`
- CORS 설정 완료

## 성능 최적화

### 주요 최적화 사항
- **N+1 쿼리 문제 해결**: FETCH JOIN과 EntityGraph를 활용한 쿼리 최적화
- **HikariCP 연결 풀 최적화**: 데이터베이스 연결 관리 성능 향상
- **p6spy 쿼리 모니터링**: 실시간 SQL 쿼리 성능 모니터링
- **EntityFinderService 패턴**: 배치 조회로 여러 엔티티 효율적 로딩

자세한 내용은 [portfolio-docs/04-database-performance-optimization.md](./portfolio-docs/04-database-performance-optimization.md)를 참조하세요.

## 실행 방법

### 필수 요구사항
- Java 21
- Gradle 7.x
- MySQL 8.x (또는 H2 Database for test)

### 환경 설정
```bash
# JWT Secret 설정 (최소 32자)
export JWT_SECRET=this-is-a-very-long-secret-key-for-jwt-authentication

# 개발 환경 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트 환경 실행 (H2 Database)
./gradlew bootRun --args='--spring.profiles.active=test'
```

### API 문서 접속
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## 관련 문서
- **[CLAUDE.md](./CLAUDE.md)** - Claude Code AI 개발 가이드
- **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** - 상세 API 명세
- **[FRONTEND_DEVELOPER_GUIDE.md](./FRONTEND_DEVELOPER_GUIDE.md)** - 프론트엔드 통합 가이드
- **[portfolio-docs/](./portfolio-docs/)** - 프로젝트 개선 사항 및 기술적 결정

## 향후 개선 계획
- 🔒 Spring Security 통합
- 📎 파일 업로드/공유 기능
- ✅ 메시지 읽음 확인 기능
- 📊 모니터링 대시보드 (Grafana, Prometheus)
- 🐳 Docker 컨테이너화 및 K8s 배포
- 🔔 실시간 알림 시스템 (FCM/APNs)

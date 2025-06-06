# 실시간 채팅 애플리케이션 (Chat App)

## 프로젝트 소개
Spring Boot 기반의 실시간 채팅 애플리케이션입니다. WebSocket을 활용한 실시간 메시지 전송과 RESTful API를 통한 채팅방 관리 기능을 제공합니다.

## 기술 스택
- **백엔드**: Java 21, Spring Boot 3.x
- **웹소켓**: STOMP, spring websocket
- **데이터베이스**: JPA/Hibernate
- **빌드 도구**: Gradle
- **테스트**: JUnit 5, Mockito

## 주요 기능
- 실시간 채팅 메시지 전송 및 수신
- 사용자 입장/퇴장 알림
- 채팅방 생성 및 관리
- 채팅 내역 조회 및 페이징
- 사용자 상태 관리

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

이 프로젝트에는 데이터베이스 성능 최적화가 적용되어 있습니다. 자세한 내용은 [PERFORMANCE_OPTIMIZATION.md](./PERFORMANCE_OPTIMIZATION.md)를 참조하세요.

### 주요 최적화 사항
- **N+1 쿼리 문제 해결**: FETCH JOIN과 EntityGraph를 활용한 쿼리 최적화
- **HikariCP 연결 풀 최적화**: 데이터베이스 연결 관리 성능 향상
- **p6spy 쿼리 모니터링**: 실시간 SQL 쿼리 성능 모니터링
- **성능 측정 도구**: 최적화 전후 성능 비교 및 분석 도구 제공

## 향후 개선 계획
- 사용자 인증 및 권한 관리 (Spring Security + JWT)
- 파일 업로드/공유 기능
- 메시지 읽음 확인 기능
- 모니터링 및 로깅 시스템 구축
- Docker 컨테이너화 및 클라우드 배포

## 프로젝트 의의
이 프로젝트를 통해 실시간 통신의 구현 방법과 애플리케이션의 계층 구조 설계, 테스트 코드 작성의 중요성을 경험했습니다. 특히 WebSocket을 활용한 양방향 통신 구현과 예외 처리에 중점을 두었습니다.

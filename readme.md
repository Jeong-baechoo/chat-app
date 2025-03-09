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
### REST API
- `POST /api/chat-rooms` - 채팅방 생성
- `GET /api/chat-rooms` - 모든 채팅방 조회
- `GET /api/chat-rooms/{id}` - 특정 채팅방 조회
- `GET /api/chat-rooms/user/{userId}` - 사용자가 참여한 채팅방 목록
- `POST /api/chat-rooms/{chatRoomId}/join` - 채팅방 참여
- `DELETE /api/chat-rooms/{id}` - 채팅방 삭제

### WebSocket Endpoints
- `ws://host/ws` - 웹소켓 연결 엔드포인트
- `/app/chat.sendMessage` - 메시지 전송
- `/app/chat.addUser` - 사용자 입장
- `/app/chat.leaveUser` - 사용자 퇴장
- `/topic/chat/{chatRoomId}` - 채팅방별 메시지 구독

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

## 향후 개선 계획
- 사용자 인증 및 권한 관리 (Spring Security + JWT)
- 파일 업로드/공유 기능
- 메시지 읽음 확인 기능
- 모니터링 및 로깅 시스템 구축
- Docker 컨테이너화 및 클라우드 배포

## 프로젝트 의의
이 프로젝트를 통해 실시간 통신의 구현 방법과 애플리케이션의 계층 구조 설계, 테스트 코드 작성의 중요성을 경험했습니다. 특히 WebSocket을 활용한 양방향 통신 구현과 예외 처리에 중점을 두었습니다.

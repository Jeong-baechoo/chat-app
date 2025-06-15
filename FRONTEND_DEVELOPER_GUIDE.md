# Frontend Developer Guide

## 🚀 Quick Start

### 1. API 문서 확인
- **Swagger UI**: http://localhost:8080/swagger-ui.html (인터랙티브 테스트 가능)
- **API 명세서**: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **OpenAPI JSON**: http://localhost:8080/api-docs (자동 코드 생성용)

### 2. 개발 환경 설정
```bash
# 백엔드 서버 실행 (필수)
export JWT_SECRET=this-is-a-very-long-secret-key-for-jwt-authentication
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 3. CORS 설정
- 현재 `http://localhost:3000`에서만 허용
- 다른 포트 사용 시 백엔드 팀에 요청

## 📋 주요 개발 순서 가이드

### 1단계: 인증 시스템 구현
```javascript
// 1. 회원가입
POST /api/auth/signup
{
  "username": "testuser",
  "password": "password123"
}

// 2. 로그인
POST /api/auth/login
{
  "username": "testuser", 
  "password": "password123"
}
// 응답: JWT_TOKEN 쿠키가 자동 설정됨

// 3. 현재 사용자 정보
GET /api/auth/me
// 쿠키의 JWT_TOKEN 자동 전송됨
```

### 2단계: 채팅방 목록 및 관리
```javascript
// 1. 전체 채팅방 목록
GET /api/rooms

// 2. 내가 참여한 채팅방
GET /api/rooms/me

// 3. 채팅방 생성
POST /api/rooms
{
  "name": "개발팀 회의",
  "type": "PUBLIC",
  "description": "매주 화요일 정기 회의"
}

// 4. 채팅방 참여
POST /api/rooms/{roomId}/join
```

### 3단계: 실시간 메시징 (WebSocket)
```javascript
// 1. WebSocket 연결
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. 연결 및 인증
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 3. 채팅방 입장
    stompClient.send("/app/room.enter", {}, JSON.stringify({
        roomId: 1
    }));
    
    // 4. 메시지 구독
    stompClient.subscribe('/topic/room.1', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });
    
    // 5. 메시지 전송
    stompClient.send("/app/message.send", {}, JSON.stringify({
        chatRoomId: 1,
        content: "안녕하세요!"
    }));
});
```

### 4단계: 메시지 히스토리
```javascript
// 최근 메시지 조회
GET /api/messages/room/{roomId}/recent?limit=50

// 페이지네이션으로 이전 메시지 조회
GET /api/messages/room/{roomId}?page=0&size=20&sort=createdAt,desc
```

## 🔧 개발 시 주의사항

### 1. 인증 처리
- **JWT는 쿠키로 자동 전송됨** (JWT_TOKEN)
- 쿠키는 HTTP-only로 설정되어 있어 JavaScript로 접근 불가
- 30분 후 만료되므로 재로그인 처리 필요

### 2. 에러 처리
```javascript
// 모든 API 응답은 동일한 에러 형식
{
  "timestamp": "2024-12-27T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "인증이 필요합니다",
  "path": "/api/rooms"
}
```

### 3. WebSocket 재연결
- 네트워크 끊김 시 자동 재연결 로직 구현 필요
- 재연결 시 채팅방 재입장 필요

### 4. 메시지 상태 관리
- `SENT`: 전송됨
- `READ`: 읽음
- 읽음 표시는 PATCH `/api/messages/{id}/status` 사용

## 📱 화면별 필요 API

### 1. 로그인/회원가입 화면
- POST `/api/auth/login`
- POST `/api/auth/signup`

### 2. 채팅방 목록 화면
- GET `/api/rooms` (전체 목록)
- GET `/api/rooms/me` (내 채팅방)
- POST `/api/rooms` (새 채팅방 만들기)

### 3. 채팅방 화면
- WebSocket 연결 (`/ws`)
- GET `/api/messages/room/{roomId}/recent`
- POST `/api/rooms/{roomId}/leave` (나가기)
- WebSocket 메시지 전송/수신

### 4. 사용자 프로필
- GET `/api/auth/me`
- POST `/api/auth/logout`

## 🛠 유용한 도구

### 1. API 클라이언트 자동 생성
```bash
# OpenAPI Generator 사용
npx openapi-generator-cli generate \
  -i http://localhost:8080/api-docs \
  -g typescript-axios \
  -o ./src/api
```

### 2. WebSocket 라이브러리
```bash
npm install sockjs-client @stomp/stompjs
```

### 3. 개발 시 테스트 계정
```
username: testuser1, testuser2, testuser3
password: password123
```

## 📞 백엔드 팀과 협의 필요 사항

1. **추가 기능 요청**
   - 사용자 프로필 이미지
   - 파일 업로드
   - 읽지 않은 메시지 수
   - 사용자 온라인 상태

2. **성능 최적화**
   - 메시지 무한 스크롤
   - 실시간 타이핑 표시
   - 푸시 알림

3. **보안 강화**
   - Refresh Token
   - 메시지 암호화
   - Rate Limiting

## 💡 Tips

1. **Swagger UI 활용**
   - "Try it out"으로 API 직접 테스트
   - Request/Response 예제 확인
   - 스키마 정보로 TypeScript 인터페이스 생성

2. **개발 순서**
   - 인증 → 채팅방 목록 → 메시지 조회 → WebSocket 연동

3. **상태 관리**
   - 현재 사용자 정보 (전역 상태)
   - 채팅방 목록 (캐싱 고려)
   - 메시지 히스토리 (페이지네이션)
   - WebSocket 연결 상태

## 🐛 자주 발생하는 문제

### 1. CORS 에러
- 프론트엔드가 `http://localhost:3000`이 아닌 경우
- 해결: 백엔드 WebConfig.java 수정 요청

### 2. 401 Unauthorized
- JWT 토큰 만료 (30분)
- 해결: 재로그인 또는 토큰 갱신 로직 구현

### 3. WebSocket 연결 실패
- 쿠키가 전송되지 않는 경우
- 해결: `withCredentials: true` 설정

### 4. 메시지 순서 문제
- 비동기 처리로 인한 순서 역전
- 해결: 타임스탬프 기반 정렬

---

**문의사항**: 백엔드 팀에 Slack 또는 이슈 트래커로 연락주세요!
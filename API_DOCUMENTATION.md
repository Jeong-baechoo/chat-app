# Chat Application API Documentation

## Overview

이 문서는 Chat Application의 REST API 및 WebSocket API에 대한 명세를 제공합니다.

- **Base URL**: `http://localhost:8080`
- **API Prefix**: `/api`
- **WebSocket Endpoint**: `/ws`
- **Authentication**: JWT 기반 (쿠키 사용)

## Authentication

모든 API 요청(인증 관련 제외)은 JWT 토큰이 필요합니다. 토큰은 `JWT_TOKEN`이라는 이름의 HTTP-only 쿠키로 전달됩니다.

## REST API Endpoints

### 1. Authentication API (`/api/auth`)

#### 1.1 로그인
- **Endpoint**: `POST /api/auth/login`
- **Description**: 사용자 로그인 및 JWT 토큰 발급
- **Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **Response**: 
```json
{
  "token": "string",
  "user": {
    "id": 1,
    "username": "string"
  }
}
```
- **Cookie**: `JWT_TOKEN` (HTTP-only, 30분 유효)

#### 1.2 회원가입
- **Endpoint**: `POST /api/auth/signup`
- **Description**: 새 사용자 등록 및 자동 로그인
- **Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **Response**: 로그인과 동일
- **Cookie**: `JWT_TOKEN` (HTTP-only, 30분 유효)

#### 1.3 로그아웃
- **Endpoint**: `POST /api/auth/logout`
- **Description**: 로그아웃 (JWT 쿠키 삭제)
- **Authentication**: Required
- **Response**:
```json
{
  "message": "로그아웃되었습니다",
  "timestamp": "2024-12-27T10:00:00"
}
```

#### 1.4 현재 사용자 정보
- **Endpoint**: `GET /api/auth/me`
- **Description**: 인증된 사용자의 정보 조회
- **Authentication**: Required
- **Response**:
```json
{
  "id": 1,
  "username": "string"
}
```

### 2. User API (`/api/users`)

#### 2.1 전체 사용자 목록
- **Endpoint**: `GET /api/users`
- **Description**: 모든 사용자 목록 조회
- **Authentication**: Required
- **Response**:
```json
[
  {
    "id": 1,
    "username": "string"
  }
]
```

#### 2.2 로그인한 사용자 목록
- **Endpoint**: `GET /api/users/isLoggedIn`
- **Description**: 현재 로그인 중인 사용자 목록
- **Authentication**: Required
- **Response**: 사용자 배열

#### 2.3 사용자 ID로 조회
- **Endpoint**: `GET /api/users/{id}`
- **Description**: 특정 사용자 정보 조회
- **Authentication**: Required
- **Path Parameters**: 
  - `id` (Long): 사용자 ID
- **Response**:
```json
{
  "id": 1,
  "username": "string"
}
```

#### 2.4 사용자명으로 조회
- **Endpoint**: `GET /api/users/username/{username}`
- **Description**: 사용자명으로 사용자 조회
- **Authentication**: Required
- **Path Parameters**:
  - `username` (String): 사용자명
- **Response**: 사용자 정보 또는 404

### 3. Chat Room API (`/api/rooms`)

#### 3.1 전체 채팅방 목록
- **Endpoint**: `GET /api/rooms`
- **Description**: 모든 채팅방의 간단한 정보 조회
- **Authentication**: Required
- **Response**:
```json
[
  {
    "id": 1,
    "name": "채팅방 이름",
    "type": "PUBLIC",
    "participantCount": 5,
    "lastMessageTime": "2024-12-27T10:00:00"
  }
]
```

#### 3.2 내가 참여한 채팅방 목록
- **Endpoint**: `GET /api/rooms/me`
- **Description**: 현재 사용자가 참여한 채팅방 목록
- **Authentication**: Required
- **Response**:
```json
[
  {
    "id": 1,
    "name": "채팅방 이름",
    "type": "PRIVATE",
    "participants": [
      {
        "userId": 1,
        "username": "user1",
        "role": "ADMIN",
        "joinedAt": "2024-12-27T10:00:00"
      }
    ],
    "createdAt": "2024-12-27T10:00:00"
  }
]
```

#### 3.3 채팅방 상세 정보
- **Endpoint**: `GET /api/rooms/{id}`
- **Description**: 특정 채팅방의 상세 정보 조회
- **Authentication**: Required
- **Path Parameters**:
  - `id` (Long): 채팅방 ID
- **Response**: 채팅방 상세 정보 또는 404

#### 3.4 채팅방 생성
- **Endpoint**: `POST /api/rooms`
- **Description**: 새 채팅방 생성
- **Authentication**: Required
- **Request Body**:
```json
{
  "name": "채팅방 이름",
  "type": "PUBLIC"  // 또는 "PRIVATE"
}
```
- **Response**: 생성된 채팅방 정보
- **Response Headers**: 
  - `Location`: 생성된 리소스 URI

#### 3.5 채팅방 참여
- **Endpoint**: `POST /api/rooms/{id}/join`
- **Description**: 채팅방에 참여
- **Authentication**: Required
- **Path Parameters**:
  - `id` (Long): 채팅방 ID
- **Response**: 업데이트된 채팅방 정보

#### 3.6 채팅방 나가기
- **Endpoint**: `POST /api/rooms/{id}/leave`
- **Description**: 채팅방에서 완전히 나가기 (참여자 목록에서 제거)
- **Authentication**: Required
- **Path Parameters**:
  - `id` (Long): 채팅방 ID
- **Response**: 200 OK (빈 본문)

#### 3.7 채팅방 삭제
- **Endpoint**: `DELETE /api/rooms/{id}`
- **Description**: 채팅방 삭제 (관리자 권한 필요)
- **Authentication**: Required
- **Path Parameters**:
  - `id` (Long): 채팅방 ID
- **Response**: 204 No Content

### 4. Message API (`/api/messages`)

#### 4.1 채팅방 메시지 조회 (페이지네이션)
- **Endpoint**: `GET /api/messages/room/{roomId}`
- **Description**: 특정 채팅방의 메시지 목록 조회
- **Authentication**: Required
- **Path Parameters**:
  - `roomId` (Long): 채팅방 ID
- **Query Parameters**:
  - `page` (int): 페이지 번호 (0부터 시작, 기본값: 0)
  - `size` (int): 페이지 크기 (기본값: 20)
  - `sort` (string): 정렬 기준 (예: "createdAt,desc")
- **Response**:
```json
{
  "content": [
    {
      "id": 1,
      "content": "메시지 내용",
      "senderId": 1,
      "senderName": "user1",
      "chatRoomId": 1,
      "status": "SENT",
      "createdAt": "2024-12-27T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5
}
```

#### 4.2 최근 메시지 조회
- **Endpoint**: `GET /api/messages/room/{roomId}/recent`
- **Description**: 채팅방의 최근 메시지 조회
- **Authentication**: Required
- **Path Parameters**:
  - `roomId` (Long): 채팅방 ID
- **Query Parameters**:
  - `limit` (int): 조회할 메시지 수 (기본값: 50)
- **Response**: 메시지 배열

#### 4.3 메시지 상태 업데이트
- **Endpoint**: `PATCH /api/messages/{id}/status`
- **Description**: 메시지 상태 변경 (읽음 표시 등)
- **Authentication**: Required
- **Path Parameters**:
  - `id` (Long): 메시지 ID
- **Request Body**:
```json
{
  "userId": 1,
  "status": "READ"  // 또는 "SENT"
}
```
- **Response**: 업데이트된 메시지 정보

## WebSocket API

### WebSocket Connection
- **Endpoint**: `/ws`
- **Protocol**: STOMP over WebSocket
- **Authentication**: JWT 쿠키 필요

### STOMP Destinations

#### 1. 메시지 전송
- **Destination**: `/app/message.send`
- **Description**: 채팅방에 메시지 전송
- **Message Format**:
```json
{
  "chatRoomId": 1,
  "content": "메시지 내용"
}
```
- **Broadcast**: `/topic/room.{roomId}` 구독자에게 전달

#### 2. 채팅방 입장
- **Destination**: `/app/room.enter`
- **Description**: 채팅방 입장 및 참여자 추가
- **Message Format**:
```json
{
  "roomId": 1
}
```

#### 3. 채팅방 연결 해제
- **Destination**: `/app/room.disconnect`
- **Description**: WebSocket 세션에서만 제거 (참여자 목록은 유지)
- **Message Format**:
```json
{
  "roomId": 1
}
```

### STOMP Subscriptions

#### 1. 채팅방 메시지 구독
- **Destination**: `/topic/room.{roomId}`
- **Description**: 특정 채팅방의 메시지 수신
- **Message Format**:
```json
{
  "id": 1,
  "content": "메시지 내용",
  "senderId": 1,
  "senderName": "user1",
  "chatRoomId": 1,
  "status": "SENT",
  "createdAt": "2024-12-27T10:00:00"
}
```

#### 2. 개인 오류 메시지
- **Destination**: `/user/queue/errors`
- **Description**: 개인별 오류 메시지 수신
- **Message Format**:
```json
{
  "timestamp": "2024-12-27T10:00:00",
  "status": "USER_ERROR",
  "message": "오류 메시지"
}
```

## Error Responses

모든 API는 오류 발생 시 통일된 ErrorResponse 형식으로 응답합니다:

```json
{
  "errorCode": "에러_코드",
  "status": "HTTP_상태",
  "message": "구체적인 오류 메시지",
  "timestamp": "2024-12-27T10:00:00",
  "fieldErrors": [ // 유효성 검증 실패 시에만 포함
    {
      "field": "필드명",
      "rejectedValue": "입력값",
      "message": "필드별 오류 메시지"
    }
  ]
}
```

### 주요 에러 코드별 응답 예시

#### 400 Bad Request - 유효성 검증 실패
```json
{
  "errorCode": "VALIDATION_ERROR",
  "status": "BAD_REQUEST",
  "message": "요청 데이터가 유효하지 않습니다",
  "timestamp": "2024-12-27T10:00:00",
  "fieldErrors": [
    {
      "field": "username",
      "rejectedValue": "ab",
      "message": "사용자명은 3-20자 사이여야 합니다"
    }
  ]
}
```

#### 401 Unauthorized - 인증 실패
```json
{
  "errorCode": "AUTH_ERROR",
  "status": "UNAUTHORIZED",
  "message": "사용자명 또는 비밀번호가 올바르지 않습니다",
  "timestamp": "2024-12-27T10:00:00",
  "fieldErrors": null
}
```

#### 404 Not Found - 리소스 없음
```json
{
  "errorCode": "USER_NOT_FOUND",
  "status": "NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "timestamp": "2024-12-27T10:00:00",
  "fieldErrors": null
}
```

#### 409 Conflict - 중복
```json
{
  "errorCode": "USER_CONFLICT",
  "status": "CONFLICT",
  "message": "사용자명 'testuser'는 이미 사용 중입니다",
  "timestamp": "2024-12-27T10:00:00",  
  "fieldErrors": null
}
```

### HTTP Status Codes
- `200 OK`: 성공
- `201 Created`: 리소스 생성 성공
- `204 No Content`: 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 필요
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스를 찾을 수 없음
- `409 Conflict`: 충돌 (중복 등)
- `500 Internal Server Error`: 서버 오류

## Data Types

### ChatRoomType
- `PUBLIC`: 공개 채팅방
- `PRIVATE`: 비공개 채팅방

### ParticipantRole
- `ADMIN`: 관리자 (채팅방 생성자)
- `MEMBER`: 일반 참여자

### MessageStatus
- `SENT`: 전송됨
- `READ`: 읽음

## Notes

1. 모든 날짜/시간은 ISO 8601 형식 (`yyyy-MM-dd'T'HH:mm:ss`)
2. ID는 모두 Long 타입
3. 페이지네이션은 0부터 시작
4. WebSocket 연결 시 JWT 쿠키가 자동으로 전송됨
5. CORS는 `http://localhost:3000`에서만 허용됨
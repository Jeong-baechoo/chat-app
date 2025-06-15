# 이상적인 에러 처리 방식

## 1. 현재 상황의 문제점
- Swagger 문서화를 위해 만든 DTO들이 실제로는 사용되지 않음
- 실제 응답과 문서가 일치하지 않을 수 있음
- 불필요한 클래스들이 생성됨

## 2. 이상적인 에러 처리 방식

### 방식 1: 단일 ErrorResponse + 동적 예시 (권장)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode(errorCode.getCode())
            .status(errorCode.getStatus().name())
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return new ResponseEntity<>(error, errorCode.getHttpStatus());
    }
}
```

**장점:**
- 실제 코드와 문서가 일치
- 유지보수가 간단
- 일관된 에러 구조

**Swagger 문서화:**
```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "404", 
        description = "채팅방을 찾을 수 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {
                  "errorCode": "CHATROOM_NOT_FOUND",
                  "status": "NOT_FOUND",
                  "message": "채팅방을 찾을 수 없습니다",
                  "timestamp": "2024-12-27T10:00:00"
                }
                """)))
})
```

### 방식 2: Problem Details (RFC 7807) 표준 사용
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            e.getErrorCode().getHttpStatus(), 
            e.getMessage()
        );
        
        problemDetail.setTitle(e.getErrorCode().getCode());
        problemDetail.setProperty("errorCode", e.getErrorCode().getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
            .body(problemDetail);
    }
}
```

**장점:**
- 국제 표준 준수
- Spring 6.0+ 에서 기본 지원
- 확장 가능한 구조

### 방식 3: 타입별 특화 처리 (복잡하지만 명확)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserException e) {
        return createErrorResponse(e.getErrorCode(), e.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(UnauthorizedException e) {
        return createErrorResponse(e.getErrorCode(), e.getMessage());
    }
    
    // 각 예외 타입별로 명시적 처리
}
```

## 3. 권장 사항

### 3.1 에러 코드 체계
```java
public enum ErrorCode {
    // 도메인별 그룹화
    // USER_XXX: 사용자 관련
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),
    
    // CHAT_XXX: 채팅방 관련
    CHAT_NOT_FOUND("CHAT_NOT_FOUND", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    CHAT_ACCESS_DENIED("CHAT_ACCESS_DENIED", HttpStatus.FORBIDDEN, "채팅방 접근 권한이 없습니다"),
    
    // AUTH_XXX: 인증 관련
    AUTH_INVALID_TOKEN("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    AUTH_EXPIRED_TOKEN("AUTH_EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다");
}
```

### 3.2 예외 계층 구조
```java
// 기본 비즈니스 예외
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}

// 도메인별 예외
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId);
    }
}

// 인증 예외
public class AuthenticationException extends BusinessException {
    // 인증 관련 특화 처리
}
```

### 3.3 클라이언트 친화적 응답
```java
@Getter
@Builder
public class ErrorResponse {
    private String errorCode;      // 프론트엔드에서 처리할 코드
    private String message;        // 사용자에게 보여줄 메시지
    private String detail;         // 개발자를 위한 상세 정보
    private LocalDateTime timestamp;
    private String path;          // 에러가 발생한 경로
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldError> fieldErrors;  // 유효성 검증 실패시만
}
```

### 3.4 로깅 전략
```java
@ExceptionHandler(BaseException.class)
public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
    // 4xx 에러는 WARN, 5xx 에러는 ERROR
    if (e.getErrorCode().getHttpStatus().is4xxClientError()) {
        log.warn("Client error: {} - {}", e.getErrorCode(), e.getMessage());
    } else {
        log.error("Server error: {} - {}", e.getErrorCode(), e.getMessage(), e);
    }
    
    // 응답 생성...
}
```

## 4. 최종 권장안

1. **단일 ErrorResponse 사용** - 실제 코드와 문서의 일치
2. **ErrorCode enum으로 중앙 관리** - 일관성 보장
3. **@ExampleObject로 Swagger 문서화** - 상태별 예시 제공
4. **명확한 예외 계층 구조** - 도메인별 예외 분리
5. **적절한 로깅** - 디버깅과 모니터링 지원

## 5. 구현 예시

```java
// Controller
@PostMapping("/{id}/join")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "404", description = "채팅방 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {
                  "errorCode": "CHAT_NOT_FOUND",
                  "status": "NOT_FOUND",
                  "message": "채팅방을 찾을 수 없습니다",
                  "timestamp": "2024-12-27T10:00:00"
                }
                """)))
})
public ResponseEntity<ChatRoomResponse> joinRoom(@PathVariable Long id) {
    return ResponseEntity.ok(chatRoomService.join(id));
}

// Service
public ChatRoomResponse join(Long roomId) {
    ChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
    // ...
}

// Exception
public class ChatRoomNotFoundException extends BaseException {
    public ChatRoomNotFoundException(Long roomId) {
        super(ErrorCode.CHAT_NOT_FOUND, 
              String.format("채팅방을 찾을 수 없습니다. ID: %d", roomId));
    }
}
```

이 방식으로 구현하면:
- 실제 동작과 문서가 일치
- 유지보수가 쉬움
- 클라이언트가 에러를 쉽게 처리 가능
- 불필요한 클래스 생성 없음
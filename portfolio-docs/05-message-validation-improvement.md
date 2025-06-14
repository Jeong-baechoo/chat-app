# 메시지 검증 로직 개선: DDD 원칙 적용과 Spring Bean Validation 활용

## 📋 개요

메시지 생성 요청 검증 로직이 응용 계층의 별도 Validator 클래스와 도메인 엔티티에 중복으로 구현되어 있던 문제를 해결하고, Spring의 표준 Bean Validation을 활용하여 DDD 원칙에 맞게 개선했습니다.

## 🎯 개선 목표

1. **중복 검증 로직 제거**: 동일한 비즈니스 규칙이 여러 곳에 산재하는 문제 해결
2. **DDD 원칙 준수**: 비즈니스 규칙을 도메인 계층에 집중
3. **Spring 표준 활용**: Bean Validation을 통한 일관된 검증 방식 적용
4. **코드 단순화**: 불필요한 Validator 클래스 제거

## 🔍 문제점 분석

### 기존 구조의 문제점

#### 1. 검증 로직의 중복
```java
// MessageValidator (응용 계층)
public class MessageValidator {
    private static final int MAX_MESSAGE_LENGTH = 1000;
    
    public void validateMessageRequest(MessageCreateRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new MessageException("메시지 내용은 필수입니다");
        }
        if (request.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageException("메시지 길이는 최대 1000자를 초과할 수 없습니다");
        }
    }
}

// Message 엔티티 (도메인 계층)
public class Message {
    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("메시지는 1000자를 초과할 수 없습니다");
        }
    }
}
```

#### 2. DDD 원칙 위반
- 비즈니스 규칙(메시지 길이 제한)이 응용 계층에 노출
- 도메인 불변성이 외부 검증에 의존

#### 3. Spring 표준 미활용
- 수동 검증 대신 Bean Validation 사용 가능
- 중복 코드와 일관성 없는 에러 처리

## ✨ 개선 내용

### 1. Bean Validation 적용

#### MessageCreateRequest DTO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지 길이는 1000자를 초과할 수 없습니다.")
    private String content;
}
```

### 2. Controller에서 @Valid 활용

#### WebSocketController
```java
@MessageMapping("/message.send")
public void sendMessage(@Payload @Valid MessageCreateRequest request, 
                       SimpMessageHeaderAccessor headerAccessor) {
    Long senderId = getUserIdFromSession(headerAccessor);
    messageService.sendMessage(request, senderId);
}
```

### 3. 응용 서비스 단순화

#### Before
```java
@Override
@Transactional
public void sendMessage(MessageCreateRequest request, Long senderId) {
    // 중복 검증
    validator.validateMessageRequest(request);
    
    ChatRoomParticipant participant = validateAndGetParticipant(senderId, request.getChatRoomId());
    // ...
}
```

#### After
```java
@Override
@Transactional
public void sendMessage(MessageCreateRequest request, Long senderId) {
    // 검증 로직 제거 - Controller에서 이미 검증됨
    ChatRoomParticipant participant = validateAndGetParticipant(senderId, request.getChatRoomId());
    // ...
}
```

### 4. 불필요한 클래스 제거
- `MessageValidator` 클래스 완전 삭제
- 테스트 코드에서 관련 의존성 제거

## 📊 개선 효과

### 1. 코드 품질 향상
- **LOC 감소**: MessageValidator 클래스 33줄 제거
- **중복 제거**: 동일한 검증 로직이 한 곳(도메인)에만 존재
- **일관성**: Spring 표준 검증 방식 사용

### 2. 아키텍처 개선
```
Before:
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Controller  │ --> │ MessageValid │ --> │   Service   │
└─────────────┘     └──────────────┘     └─────────────┘
                            │                     │
                            v                     v
                    ┌──────────────┐     ┌─────────────┐
                    │   Exception  │     │   Domain    │
                    └──────────────┘     └─────────────┘

After:
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Controller  │ --> │   Service   │ --> │   Domain    │
│  (@Valid)   │     │             │     │ (검증 포함) │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 3. DDD 원칙 준수
- **단일 책임**: 도메인 검증은 도메인 계층에서만
- **캡슐화**: 비즈니스 규칙이 도메인 내부에 숨겨짐
- **표현 계층 분리**: DTO 검증과 도메인 검증의 명확한 구분

## 🔄 마이그레이션 가이드

### 기존 코드를 개선하는 단계

1. **DTO에 Bean Validation 어노테이션 추가**
   ```java
   @NotNull, @NotBlank, @Size 등 적용
   ```

2. **Controller에 @Valid 추가**
   ```java
   public void method(@Valid @RequestBody Request request)
   ```

3. **Service에서 Validator 의존성 제거**
   - Validator 주입 제거
   - validateXXX() 메서드 호출 제거

4. **Validator 클래스 삭제**

5. **테스트 코드 수정**
   - Mock Validator 제거
   - 관련 verify() 구문 제거

## 💡 핵심 교훈

1. **표준 활용**: Spring이 제공하는 표준 기능을 적극 활용
2. **중복 제거**: 같은 검증을 여러 곳에서 하지 않기
3. **계층 책임**: 각 계층이 담당해야 할 검증의 명확한 구분
   - **표현 계층**: 형식 검증 (null, blank, size)
   - **도메인 계층**: 비즈니스 규칙 검증

4. **DDD 실천**: 비즈니스 규칙은 도메인 객체가 스스로 지키도록

## 🎯 결론

이번 개선을 통해 중복된 검증 로직을 제거하고, Spring의 표준 Bean Validation을 활용하여 더 깔끔하고 유지보수하기 쉬운 코드를 만들었습니다. 특히 DDD 원칙에 따라 비즈니스 규칙을 도메인 계층에 집중시킴으로써 응집도 높은 도메인 모델을 구현할 수 있었습니다.

**개선 전**: 응용 계층과 도메인 계층에 중복된 검증 로직
**개선 후**: 표현 계층은 Bean Validation, 도메인 계층은 비즈니스 규칙만 담당

이러한 접근 방식은 코드의 가독성과 유지보수성을 크게 향상시키며, 팀 전체가 일관된 방식으로 검증 로직을 구현할 수 있게 합니다.
# 아키텍처 개선 기록

## 개요
실시간 채팅 애플리케이션의 도메인 계층을 DDD(Domain-Driven Design)와 Clean Architecture 원칙에 따라 개선한 과정을 기록합니다.

## 개선 전 문제점

### 1. 불변성 부족
```java
// 문제: 누구나 자유롭게 상태 변경 가능
@Entity
@Setter  // 위험한 Setter 노출
public class User {
    private String password;
}

// 사용 예시
user.setPassword("new_password"); // 암호화 없이 직접 변경
```

### 2. 도메인 로직 누락
- 비밀번호 암호화/검증 로직이 엔티티 외부에 존재
- 사용자명 검증 규칙이 서비스 계층에 분산
- 비즈니스 규칙이 무시될 수 있는 구조

### 3. 의존성 방향 문제
```java
// 문제: 도메인이 인프라에 의존
Domain → Infrastructure (잘못된 방향)
```

## 개선 과정

### 1단계: 불변성 확보

**Setter 제거 및 도메인 로직 추가**
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용
public class User {
    private String username;
    private String password;
    
    // 정적 팩토리 메서드
    public static User create(String username, String rawPassword, PasswordEncoder encoder) {
        String encodedPassword = encoder.encode(rawPassword);
        return new User(username, encodedPassword);
    }
    
    // 비즈니스 규칙을 통한 상태 변경
    public void changePassword(String current, String newPassword, PasswordEncoder encoder) {
        if (!encoder.matches(current, this.password)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }
        validateRawPassword(newPassword);
        this.password = encoder.encode(newPassword);
    }
    
    // 검증 로직 내장
    private static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("사용자명은 3-50자 사이여야 합니다");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("사용자명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다");
        }
    }
}
```

### 2단계: 의존성 역전 적용

**PasswordEncoder를 도메인 패키지로 이동**
```java
// Before: infrastructure/auth/PasswordEncoder
// After: domain/auth/PasswordEncoder

// 의존성 방향
Application Service → Domain ← Infrastructure
```

### 3단계: 도메인 서비스 도입 (실험)

**UserDomainService 생성 (후에 제거됨)**
```java
@Service
public class UserDomainService {
    private final PasswordEncoder passwordEncoder;
    
    public User createUser(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return User.create(username, encodedPassword);
    }
}
```

### 4단계: 최종 단순화 (현재 상태)

**암호화를 애플리케이션 서비스로 이동**

**핵심 깨달음:** 암호화는 기술적 관심사이므로 도메인이 알 필요가 없다.

```java
// User 엔티티: 순수 비즈니스 로직만
public class User {
    public static User create(String username, String encodedPassword) {
        return new User(username, encodedPassword);
    }
    
    public boolean isPasswordMatch(String encodedPassword) {
        return this.password.equals(encodedPassword);
    }
    
    public void changePassword(String newEncodedPassword) {
        validatePassword(newEncodedPassword);
        this.password = newEncodedPassword;
    }
}

// AuthService: 기술적 관심사 처리
@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder; // 인프라 의존성
    
    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username);
        
        // 암호화는 애플리케이션 서비스에서
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("비밀번호가 일치하지 않습니다");
        }
        
        return createAuthResponse(user);
    }
    
    public Map<String, Object> signup(String username, String password) {
        // 암호화 후 도메인에 전달
        String encodedPassword = passwordEncoder.encode(password);
        User user = User.create(username, encodedPassword);
        
        return createAuthResponse(userRepository.save(user));
    }
}
```

## 최종 아키텍처

### 의존성 구조
```
┌─────────────────┐
│   AuthService   │ ← 암호화 등 기술적 관심사 처리
│ (Application)   │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐
│      User       │    │ PasswordEncoder │
│   (Domain)      │    │(Infrastructure) │
└─────────────────┘    └─────────────────┘
      순수 비즈니스           기술적 구현
```

### 계층별 책임

**Domain (User)**
- 사용자명 형식 검증
- 비밀번호 변경 권한 확인
- 엔티티 생성 규칙
- equals/hashCode 구현

**Application Service (AuthService)**
- 암호화/복호화 처리
- 세션 관리
- 트랜잭션 경계
- 도메인 로직 조율

**Infrastructure (PasswordEncoder, Repository)**
- 암호화 알고리즘 구현
- 데이터베이스 연동
- 외부 시스템 통신

## 주요 설계 원칙 적용

### 1. 단일 책임 원칙 (SRP)
- User: 사용자 관련 비즈니스 규칙만
- AuthService: 인증/인가 로직만
- PasswordEncoder: 암호화 로직만

### 2. 의존성 역전 원칙 (DIP)
- 도메인이 인프라에 의존하지 않음
- 인터페이스를 통한 추상화

### 3. 캡슐화
- private 생성자 + 정적 팩토리 메서드
- 비즈니스 규칙을 통해서만 상태 변경

### 4. 불변성
- Setter 제거
- 의미 있는 메서드명으로 상태 변경

## 개선 효과

### 장점
1. **안전성**: 잘못된 상태 변경 방지
2. **명확성**: 비즈니스 의도가 코드에 드러남
3. **테스트 용이성**: 각 계층을 독립적으로 테스트 가능
4. **유지보수성**: 변경 영향도 최소화
5. **확장성**: 새로운 비즈니스 규칙 추가 용이

### 학습 포인트
1. **과도한 추상화 피하기**: UserDomainService는 불필요했음
2. **기술적 관심사 분리**: 암호화는 인프라 영역
3. **적절한 복잡성**: 단순함과 유연성의 균형
4. **의존성 방향**: Clean Architecture의 핵심

## 5단계: 비즈니스 로직의 도메인 이동 (추가 개선)

### 문제 발견
초기 DDD 적용 후에도 여전히 핵심 비즈니스 로직이 응용 서비스 레이어에 남아있는 문제를 발견했습니다.

**응용 서비스에 있던 비즈니스 로직 예시:**
```java
// ChatRoomServiceImpl - 권한 검증이 서비스에 있음
private void validateUserIsRoomAdmin(Long userId, Long chatRoomId) {
    boolean isAdmin = participantRepo.findByUserIdAndChatRoomId(userId, chatRoomId)
            .map(participant -> participant.getRole() == ParticipantRole.ADMIN)
            .orElse(false);
    
    if (!isAdmin) {
        throw new ChatRoomException("채팅방을 삭제할 권한이 없습니다");
    }
}

// MessageServiceImpl - 참여자 검증이 서비스에 있음
private ChatRoomParticipant validateAndGetParticipant(Long userId, Long chatRoomId) {
    return participantRepository
            .findByUserIdAndChatRoomIdWithUserAndChatRoom(userId, chatRoomId)
            .orElseThrow(() -> new MessageException("채팅방 참여자만 메시지를 보낼 수 있습니다"));
}
```

### 도메인 엔티티 강화

**ChatRoom 엔티티에 비즈니스 메서드 추가:**
```java
@Entity
public class ChatRoom {
    // 기존 메서드들...
    
    /**
     * 사용자의 ID로 참여자 확인
     */
    public boolean isParticipantById(Long userId) {
        return participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
    }

    /**
     * 사용자의 ID로 관리자 권한 확인
     */
    public boolean isAdminById(Long userId) {
        return participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(userId) && p.getRole() == ParticipantRole.ADMIN);
    }

    /**
     * 사용자가 채팅방을 삭제할 수 있는지 확인
     */
    public void validateCanDelete(Long userId) {
        if (!isAdminById(userId)) {
            throw new IllegalStateException("채팅방을 삭제할 권한이 없습니다. 관리자만 삭제할 수 있습니다.");
        }
    }

    /**
     * 사용자가 메시지를 보낼 수 있는지 확인
     */
    public void validateCanSendMessage(Long userId) {
        if (!isParticipantById(userId)) {
            throw new IllegalStateException("채팅방 참여자만 메시지를 보낼 수 있습니다.");
        }
    }
}
```

**Message 엔티티의 생성 메서드 개선:**
```java
@Entity
public class Message {
    /**
     * 새로운 메시지 생성 (참여자 검증 포함)
     */
    public static Message create(String content, User sender, ChatRoom chatRoom) {
        // 채팅방 참여자인지 검증
        if (!chatRoom.isParticipantById(sender.getId())) {
            throw new IllegalStateException("채팅방 참여자만 메시지를 보낼 수 있습니다");
        }
        return new Message(content, sender, chatRoom);
    }
}
```

### 응용 서비스 단순화

**Before (ChatRoomServiceImpl):**
```java
@Override
@Transactional
public void deleteChatRoom(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);
    validateUserIsRoomAdmin(userId, chatRoomId); // 서비스에서 검증
    
    chatRoomRepository.delete(chatRoom);
}
```

**After (ChatRoomServiceImpl):**
```java
@Override
@Transactional
public void deleteChatRoom(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);
    
    // 도메인에서 권한 검증
    chatRoom.validateCanDelete(userId);
    
    chatRoomRepository.delete(chatRoom);
}
```

**Before (MessageServiceImpl):**
```java
@Override
@Transactional
public void sendMessage(MessageCreateRequest request, Long senderId) {
    // 메시지 전송 권한 확인 (채팅방 참여자인지)
    ChatRoomParticipant participant = validateAndGetParticipant(senderId, request.getChatRoomId());
    User sender = participant.getUser();
    ChatRoom chatRoom = participant.getChatRoom();
    
    Message message = Message.create(request.getContent(), sender, chatRoom);
}
```

**After (MessageServiceImpl):**
```java
@Override
@Transactional
public void sendMessage(MessageCreateRequest request, Long senderId) {
    // 엔티티 조회
    User sender = entityFinder.findUserById(senderId);
    ChatRoom chatRoom = entityFinder.findChatRoomById(request.getChatRoomId());
    
    // 메시지 생성 (도메인에서 참여자 검증 수행)
    Message message = Message.create(request.getContent(), sender, chatRoom);
}
```

### 개선 효과

1. **응집도 향상**: 비즈니스 규칙이 해당 도메인 엔티티에 모임
2. **결합도 감소**: 서비스 레이어의 Repository 의존성 감소
3. **테스트 용이성**: 도메인 로직을 독립적으로 테스트 가능
4. **일관성**: 동일한 비즈니스 규칙이 여러 곳에서 중복되지 않음

### 제거된 중복 코드
- ChatRoomServiceImpl: `validateUserIsRoomAdmin()` 메서드 제거
- MessageServiceImpl: `validateAndGetParticipant()` 메서드 및 ChatRoomParticipantRepository 의존성 제거
- AuthService: `getUserById()` 중복 메서드 제거
- UserServiceImpl: 직접적인 Repository 조회를 EntityFinderService로 통일

## 결론

DDD와 Clean Architecture 원칙을 적용하여 도메인의 순수성을 확보했습니다. 
특히 "복잡성을 관리하기 위한 도구"로서 DDD를 활용하되, 과도한 추상화는 피했습니다.

초기 적용 후에도 지속적인 리팩토링을 통해 응용 서비스 레이어에 남아있던 비즈니스 로직을 도메인으로 이동시켜 **진정한 풍부한 도메인 모델(Rich Domain Model)**을 구현했습니다.

**핵심 교훈:** 
1. 아키텍처는 비즈니스 문제를 해결하기 위한 도구이며, 문제의 복잡성에 맞는 적절한 수준의 설계를 적용하는 것이 중요합니다.
2. DDD 적용은 한 번에 완성되는 것이 아니라, 지속적인 개선을 통해 더 나은 도메인 모델로 발전시켜 나가는 과정입니다.
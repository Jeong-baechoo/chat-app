
# 데이터베이스 성능 최적화 문서

## 개요

이 문서는 채팅 애플리케이션의 데이터베이스 성능 최적화 작업에 대한 상세한 기록입니다. 주요 목표는 N+1 쿼리 문제를 해결하고, 데이터베이스 연결 풀 설정을 최적화하여 전반적인 애플리케이션 성능을 향상시키는 것입니다.

## 최적화 항목

### 1. 데이터베이스 연결 설정 최적화

#### HikariCP 연결 풀 설정
`application-dev.yml`에서 HikariCP 연결 풀 설정을 최적화했습니다:

```yaml
spring:
  datasource:
    # MySQL 성능 최적화 파라미터 추가
    url: jdbc:p6spy:mysql://127.0.0.1:3306/chatapp?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048
    
    # HikariCP 연결 풀 최적화 설정
    hikari:
      maximum-pool-size: 20           # 최대 연결 수
      minimum-idle: 5                 # 최소 유휴 연결 수
      idle-timeout: 300000           # 유휴 연결 타임아웃 (5분)
      max-lifetime: 1800000          # 연결 최대 수명 (30분)
      connection-timeout: 20000      # 연결 타임아웃 (20초)
      validation-timeout: 5000       # 연결 검증 타임아웃 (5초)
      leak-detection-threshold: 60000 # 연결 누수 감지 임계값 (1분)
      connection-test-query: SELECT 1 # 연결 테스트 쿼리
```

#### JPA/Hibernate 성능 설정
```yaml
jpa:
  properties:
    hibernate:
      # 배치 처리 최적화
      jdbc:
        batch_size: 20
        batch_versioned_data: true
      order_inserts: true
      order_updates: true
      
      # 쿼리 캐시 설정
      cache:
        use_second_level_cache: true
        use_query_cache: true
        region:
          factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
      
      # 통계 및 모니터링
      generate_statistics: true
```

### 2. 쿼리 모니터링 설정 (p6spy)

p6spy를 활용하여 실행되는 모든 SQL 쿼리를 모니터링하도록 설정했습니다:

```properties
# p6spy 설정 파일 - 성능 최적화를 위한 쿼리 모니터링
appender=com.p6spy.engine.spy.appender.StdoutLogger
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=[P6SPY] 실행시간: %(executionTime)ms | Connection: %(connection) | 쿼리: %(sql)

# N+1 쿼리 감지를 위한 낮은 임계값 설정 (50ms)
executionThreshold=50

# 스택 추적 표시 (쿼리 발생 위치 추적)
stacktraceclass=com.example.chatapp
```

### 3. N+1 쿼리 문제 해결

#### 3.1 메시지 조회 최적화

**기존 문제점:**
```java
// N+1 문제 발생 - 메시지 조회 후 각 메시지의 발신자, 채팅방 정보를 개별 조회
List<Message> messages = messageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId, limit);
for (Message message : messages) {
    String senderName = message.getSender().getUsername(); // 추가 쿼리 발생
    String chatRoomName = message.getChatRoom().getName(); // 추가 쿼리 발생
}
```

**최적화 해결책:**
```java
// FETCH JOIN을 사용하여 관련 엔티티를 한번에 조회
@Query("SELECT m FROM Message m " +
       "LEFT JOIN FETCH m.sender " +
       "LEFT JOIN FETCH m.chatRoom " +
       "WHERE m.chatRoom.id = :chatRoomId " +
       "ORDER BY m.timestamp DESC")
List<Message> findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
```

#### 3.2 채팅방 참여자 조회 최적화

**기존 문제점:**
```java
// N+1 문제 발생 - 채팅방 조회 후 각 채팅방의 참여자 정보를 개별 조회
List<ChatRoom> chatRooms = chatRoomRepository.findAll();
for (ChatRoom chatRoom : chatRooms) {
    int participantCount = chatRoom.getParticipants().size(); // 추가 쿼리 발생
}
```

**최적화 해결책:**
```java
// 이미 구현된 FETCH JOIN 쿼리 활용
@Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants")
List<ChatRoom> findAllWithParticipants();
```

#### 3.3 채팅방 참여자 관계 최적화

**최적화된 쿼리 추가:**
```java
// 사용자와 채팅방 정보를 함께 조회 (FETCH JOIN)
@Query("SELECT crp FROM ChatRoomParticipant crp " +
       "LEFT JOIN FETCH crp.user " +
       "LEFT JOIN FETCH crp.chatRoom " +
       "WHERE crp.user.id = :userId AND crp.chatRoom.id = :chatRoomId")
Optional<ChatRoomParticipant> findByUserIdAndChatRoomIdWithUserAndChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);
```

### 4. 대안적 최적화 방법

#### EntityGraph 활용
FETCH JOIN 외에 `@EntityGraph` 애노테이션을 활용한 최적화 방법도 제공:

```java
@EntityGraph(attributePaths = {"sender", "chatRoom"})
@Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.timestamp DESC")
List<Message> findByChatRoomIdWithEntityGraph(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
```

## 성능 측정 및 테스트

### 1. 테스트 데이터 생성 서비스

대량의 테스트 데이터를 생성하여 성능 측정을 위한 `TestDataGeneratorService` 구현:

- 사용자, 채팅방, 메시지 대량 생성
- 실제 운영 환경과 유사한 데이터 관계 구성
- 배치 단위 저장으로 메모리 효율성 고려

### 2. N+1 쿼리 문제 시연

`N1QueryDemonstrator`를 통해 최적화 전후 비교:

```java
// N+1 문제 시연
public void demonstrateN1ForChatRoomsAndParticipants() {
    // 1. 모든 채팅방 조회 (1개의 쿼리)
    List<ChatRoom> chatRooms = chatRoomRepository.findAll();
    
    // 2. 각 채팅방의 참여자 정보 접근 (N개의 추가 쿼리 발생)
    for (ChatRoom chatRoom : chatRooms) {
        int participantCount = chatRoom.getParticipants().size();
    }
}

// 최적화된 쿼리 시연
public void demonstrateOptimizedQuery() {
    // FETCH JOIN으로 한번에 조회
    List<ChatRoom> chatRooms = chatRoomRepository.findAllWithParticipants();
    
    // 추가 쿼리 없이 참여자 정보 접근
    for (ChatRoom chatRoom : chatRooms) {
        int participantCount = chatRoom.getParticipants().size();
    }
}
```

### 3. 성능 비교 측정

`PerformanceComparisonService`를 통한 정량적 성능 측정:

- 기존 쿼리 vs FETCH JOIN 최적화된 쿼리 실행 시간 비교
- EntityGraph를 활용한 쿼리와의 성능 비교
- 개선 효과 백분율 계산 및 로깅

## 성능 테스트 API

개발 환경에서 성능 테스트를 위한 REST API 제공:

```
POST /api/performance/generate-test-data        # 테스트 데이터 생성
POST /api/performance/quick-performance-test    # 빠른 성능 테스트
POST /api/performance/compare-performance       # 최적화 전후 성능 비교
POST /api/performance/demonstrate-n1-queries    # N+1 쿼리 문제 시연
GET  /api/performance/statistics                # 데이터 통계 조회
```

## 기대 효과

### 1. 쿼리 수 감소
- **기존**: 1 + N개의 쿼리 (N+1 문제)
- **최적화 후**: 1개의 FETCH JOIN 쿼리

### 2. 응답 시간 개선
- 관련 엔티티 조회를 위한 추가 데이터베이스 왕복 제거
- 네트워크 대기 시간 감소

### 3. 데이터베이스 부하 감소
- 동시 연결 수 감소
- CPU 및 메모리 사용량 최적화

### 4. 애플리케이션 확장성 향상
- 더 많은 동시 사용자 지원 가능
- 안정적인 성능 유지

## 모니터링 및 지속적 개선

### 1. p6spy를 통한 지속적 모니터링
- 실행 시간이 50ms 이상인 쿼리 자동 감지
- 쿼리 발생 위치 스택 추적으로 문제 지점 파악

### 2. JPA 통계 활성화
- `hibernate.generate_statistics=true`로 Hibernate 통계 수집
- 세션별 쿼리 수, 캐시 히트율 등 모니터링

### 3. 연결 풀 모니터링
- HikariCP 메트릭스를 통한 연결 풀 상태 모니터링
- 연결 누수 감지 및 예방

## 결론

이번 최적화 작업을 통해 다음과 같은 성과를 달성했습니다:

1. **N+1 쿼리 문제 해결**: FETCH JOIN과 EntityGraph를 활용한 쿼리 최적화
2. **데이터베이스 연결 최적화**: HikariCP 설정 튜닝으로 연결 관리 개선
3. **성능 모니터링 체계 구축**: p6spy를 활용한 쿼리 성능 모니터링
4. **테스트 인프라 구축**: 성능 측정 및 비교를 위한 테스트 도구 개발

이러한 최적화를 통해 애플리케이션의 성능과 확장성이 크게 향상되었으며, 향후 유사한 성능 이슈를 조기에 발견하고 해결할 수 있는 기반을 마련했습니다.
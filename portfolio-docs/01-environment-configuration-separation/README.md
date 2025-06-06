# 환경별 설정 파일 분리 개선 작업

## 📋 프로젝트 개요
Spring Boot 채팅 애플리케이션의 환경별 설정 파일을 분리하여 개발/테스트/운영 환경에 맞는 최적화된 설정을 적용하는 개선 작업입니다.

## 🔍 문제 상황

### 기존 문제점
- **하나의 설정 파일**: 모든 환경 설정이 `application.yml` 한 파일에 혼재
- **환경별 구분 없음**: 개발과 운영 환경의 요구사항 차이 무시
- **보안 취약점**: 운영 환경 민감 정보가 하드코딩됨
- **유지보수 어려움**: 환경별 설정 변경 시 전체 파일 수정 필요

### 구체적 문제 사항
```yaml
# 기존 application.yml - 모든 설정이 혼재
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatapp  # 운영 DB 정보
    username: chatapp                         # 하드코딩된 계정
    password: chatapp123                      # 평문 패스워드
  jpa:
    show-sql: true                           # 운영에 부적합한 설정
    hibernate.ddl-auto: update               # 운영에 위험한 설정
```

## 🎯 해결 목표
1. **환경별 설정 분리**: 개발/테스트/운영 환경에 맞는 독립적 설정
2. **보안 강화**: 운영 환경 민감 정보의 환경 변수 처리
3. **성능 최적화**: 각 환경 특성에 맞는 리소스 설정
4. **유지보수성 향상**: 환경별 독립적 설정 관리

## 🔧 해결 방법

### 1. Spring Profile 기반 설정 분리
Spring Boot의 Profile 기능을 활용하여 환경별 설정 파일을 분리했습니다.

### 2. 생성된 설정 파일 구조
```
src/main/resources/
├── application.yml          # 공통 설정
├── application-dev.yml      # 개발 환경
├── application-test.yml     # 테스트 환경
└── application-prod.yml     # 운영 환경
```

### 3. 각 환경별 주요 설정

#### 공통 설정 (`application.yml`)
```yaml
# 모든 환경에서 공통으로 사용하는 기본 설정
spring:
  application:
    name: chat-app
  profiles:
    active: dev  # 기본값을 개발 환경으로 설정
  jpa:
    properties:
      hibernate:
        use_sql_comments: true
        format_sql: false

server:
  port: 8080

chat:
  websocket:
    endpoint: /ws
```

#### 개발 환경 (`application-dev.yml`)
```yaml
# 개발자 친화적 설정
spring:
  datasource:
    url: jdbc:h2:mem:chatapp-dev  # 메모리 기반 H2 DB
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true  # H2 콘솔 활성화
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # 개발용 스키마 자동 생성
    show-sql: true           # SQL 로그 출력
    properties:
      hibernate:
        format_sql: true     # SQL 포맷팅

# 상세한 디버그 로깅
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    com.example.chatapp: DEBUG
```

#### 테스트 환경 (`application-test.yml`)
```yaml
# 테스트 최적화 설정
spring:
  datasource:
    url: jdbc:h2:mem:chatapp-test
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false  # 테스트 시 SQL 로그 비활성화
  
  kafka:
    enabled: false  # 테스트에서 Kafka 비활성화

# 최소한의 로깅
logging:
  level:
    org.hibernate: WARN
    org.springframework: WARN
    com.example.chatapp: INFO
```

#### 운영 환경 (`application-prod.yml`)
```yaml
# 운영 최적화 및 보안 강화 설정
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatapp?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=true
    username: ${DB_USERNAME:chatapp}      # 환경 변수 사용
    password: ${DB_PASSWORD:chatapp123}   # 환경 변수 사용
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20               # 커넥션 풀 최적화
      minimum-idle: 5
      connection-timeout: 20000
  
  jpa:
    hibernate:
      ddl-auto: validate  # 운영에서는 스키마 검증만
    show-sql: false       # 성능을 위해 SQL 로그 비활성화
    properties:
      hibernate:
        jdbc.batch_size: 20  # 배치 처리 최적화
        order_inserts: true
        order_updates: true

# 성능 최적화된 서버 설정
server:
  tomcat:
    threads:
      max: 200           # 높은 동시 처리량
      min-spare: 20
    max-connections: 8192
    accept-count: 100
  compression:
    enabled: true        # 응답 압축 활성화
    mime-types: application/json,text/html,text/css,application/javascript
    min-response-size: 1024

# 구조화된 로깅
logging:
  level:
    org.hibernate: WARN
    org.springframework: INFO
    com.example.chatapp: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/chat-app.log
    max-size: 100MB
    max-history: 30
```

## 🚀 적용 방법

### 1. Profile 활성화 방법

#### IDE에서 실행 시
```bash
# 개발 환경으로 실행
java -jar -Dspring.profiles.active=dev chat-app.jar

# 테스트 환경으로 실행
java -jar -Dspring.profiles.active=test chat-app.jar

# 운영 환경으로 실행
java -jar -Dspring.profiles.active=prod chat-app.jar
```

#### 환경 변수로 설정
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar chat-app.jar
```

### 2. 운영 환경 보안 설정
```bash
# 환경 변수로 민감 정보 관리
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password_123
export FRONTEND_URL=https://chatapp.company.com
export KAFKA_SERVERS=kafka1:9092,kafka2:9092
```

## 📊 개선 효과

### 1. 보안 강화
- ✅ 운영 환경 민감 정보의 환경 변수 처리
- ✅ 개발/운영 환경 설정 완전 분리
- ✅ SSL 연결 및 보안 설정 강화

### 2. 성능 최적화
- ✅ 환경별 맞춤 리소스 설정
- ✅ 운영 환경 커넥션 풀 최적화 (20개 풀)
- ✅ 배치 처리 및 압축 기능 활성화
- ✅ 불필요한 로깅 제거로 성능 향상

### 3. 개발 생산성 향상
- ✅ 개발자 친화적 H2 콘솔 제공
- ✅ 상세한 디버그 로깅으로 문제 해결 용이
- ✅ 자동 스키마 생성으로 빠른 개발 환경 구축

### 4. 운영 안정성 확보
- ✅ DDL 자동 실행 방지 (validate 모드)
- ✅ 구조화된 로그 파일 관리
- ✅ 환경별 독립적 설정 관리

## 🎓 학습 내용

### 1. Spring Profile 시스템 이해
- **Profile 우선순위**: 명령행 > 환경변수 > application.yml
- **Profile 조합**: 여러 Profile 동시 활성화 가능
- **설정 오버라이드**: 환경별 설정이 공통 설정을 덮어씀

### 2. 설정 외부화 패턴
- **환경 변수 활용**: `${변수명:기본값}` 문법
- **민감 정보 보호**: 하드코딩 대신 런타임 주입
- **설정 검증**: 잘못된 설정으로 인한 오류 방지

### 3. 성능 튜닝 기법
- **커넥션 풀 설정**: HikariCP 최적화 매개변수
- **JPA 성능 최적화**: 배치 처리, 쿼리 최적화
- **서버 튜닝**: Tomcat 스레드 풀, 압축 설정

### 4. 운영 환경 고려사항
- **로깅 전략**: 환경별 로그 레벨 차등 적용
- **보안 설정**: SSL, 인증, 권한 관리
- **모니터링**: 성능 지표 수집을 위한 설정

## 🔄 다음 개선 과제
1. **설정 검증**: @ConfigurationProperties + @Valid 적용
2. **외부 설정 서버**: Spring Cloud Config 도입 검토
3. **암호화**: 민감 정보 암호화 저장 방안 검토
4. **컨테이너 환경**: Docker, Kubernetes 환경 최적화

## 📚 참고 자료
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
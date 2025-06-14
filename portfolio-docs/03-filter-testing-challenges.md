# Spring Boot 필터 적용 시 Controller 테스트 문제 해결

## 📋 문제 상황

### 프로젝트 배경
Spring Boot 채팅 애플리케이션에서 세션 기반 인증을 위한 `SessionAuthenticationFilter`를 구현했으나, Controller 테스트 실행 시 필터에 의해 모든 요청이 차단되는 문제가 발생했습니다.

### 구체적 문제 사례

#### 문제가 된 필터 구현
```java
@Component  // ❌ 이 애노테이션으로 인해 모든 테스트에서 필터 활성화
@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 인증이 필요없는 경로는 패스
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 세션 토큰 추출 및 검증
        String sessionToken = extractSessionToken(request);
        if (sessionToken == null) {
            handleAuthenticationFailure(response, "인증이 필요합니다");  // ❌ 401 응답
            return;
        }
        
        // ... 인증 로직
    }
}
```

#### 실패한 Controller 테스트
```java
@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ChatRoomService chatRoomService;
    
    @Test
    void createChatRoom() throws Exception {
        ChatRoomCreateRequest request = ChatRoomCreateRequest.builder()
            .name("테스트방")
            .type(ChatRoomType.PUBLIC)
            .build();
        
        // ❌ 실패: 401 Unauthorized
        // 필터에서 세션 토큰이 없다고 판단하여 요청 차단
        mockMvc.perform(post("/api/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());  // 예상: 201, 실제: 401
    }
}
```

#### 에러 로그
```
MockHttpServletResponse:
           Status = 401
    Error message = null
          Headers = [Content-Type:"application/json"]
             Body = {"error":"인증이 필요합니다","status":401}

Expected status:<201> but was:<401>
```

## 🔍 문제 원인 분석

### 1. @WebMvcTest의 필터 로딩 방식
```java
@WebMvcTest(ChatRoomController.class)
```
- **스프링 웹 컨텍스트** 로딩 시 `@Component`로 등록된 모든 필터를 자동 로딩
- **테스트 환경**에서도 프로덕션과 동일한 필터 체인 적용
- **인증이 필요한 엔드포인트**에 대해 실제 인증 로직 실행

### 2. 필터 체인 실행 순서
```
HTTP 요청 → SessionAuthenticationFilter → Controller
              ↑
            401 응답 반환 (테스트 실패)
```

### 3. Mock 객체와 필터의 차이점
```java
@MockitoBean
private AuthContext authContext;  // ✅ Controller에서 사용하는 의존성은 Mock 처리 가능

// ❌ 하지만 필터는 Controller 이전에 실행되므로 Mock으로 처리 불가
```

## 🛠️ 해결 방법들

### 해결책 1: 필터 비활성화 (addFilters = false) ⭐ 권장

```java
@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)  // ✅ 모든 필터 비활성화
class ChatRoomControllerTest {
    
    @MockitoBean
    private AuthContext authContext;  // Mock으로 인증 컨텍스트 대체
    
    @BeforeEach
    void setUp() {
        // 인증된 사용자로 가정
        when(authContext.getCurrentUserId()).thenReturn(1L);
        when(authContext.isAuthenticated()).thenReturn(true);
    }
    
    @Test
    void createChatRoom_성공() throws Exception {
        // Given
        ChatRoomCreateRequest request = ChatRoomCreateRequest.builder()
            .name("테스트방")
            .type(ChatRoomType.PUBLIC)
            .build();
            
        ChatRoomResponse response = ChatRoomResponse.builder()
            .id(1L)
            .name("테스트방")
            .type(ChatRoomType.PUBLIC)
            .build();
            
        when(chatRoomService.createChatRoom(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())  // ✅ 성공
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("테스트방"));
            
        // 인증 컨텍스트 호출 확인
        verify(authContext).getCurrentUserId();
    }
}
```

**장점**:
- 가장 간단하고 직관적인 해결책
- Controller 로직에만 집중 가능
- AuthContext Mock으로 인증 상태 제어 가능

**단점**:
- 필터와 Controller 간의 통합 동작 테스트 불가

### 해결책 2: 필터를 조건부로 활성화

```java
// 환경에 따라 필터 활성화 제어
@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    // ... 필터 구현
}
```

```yaml
# application-test.yml
app:
  security:
    enabled: false  # 테스트 환경에서 필터 비활성화
```

**장점**:
- 환경별로 필터 동작 제어 가능
- 프로덕션과 테스트 환경 분리

**단점**:
- 설정 관리 복잡성 증가

### 해결책 3: 프로파일 기반 필터 등록

```java
@Component
@Profile("!test")  // 테스트 프로파일이 아닐 때만 활성화
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    // ... 필터 구현
}
```

```java
@ActiveProfiles("test")  // 테스트 프로파일 활성화
@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {
    // 필터가 자동으로 비활성화됨
}
```

**장점**:
- 프로파일 기반 명확한 환경 구분
- 테스트 환경에서 자동으로 필터 제외

**단점**:
- 모든 테스트 클래스에 @ActiveProfiles 추가 필요

### 해결책 4: Mock 인증 토큰 제공

```java
@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {
    
    @MockitoBean
    private SessionStore sessionStore;
    
    @Test
    void createChatRoom_인증토큰포함() throws Exception {
        // Given - Mock 세션 설정
        String mockToken = "mock-session-token";
        LoginSession mockSession = new LoginSession(1L, System.currentTimeMillis() + 1800000);
        when(sessionStore.getSession(mockToken)).thenReturn(mockSession);
        
        // When & Then - 쿠키에 Mock 토큰 포함
        mockMvc.perform(post("/api/rooms")
            .cookie(new Cookie("SESSION_TOKEN", mockToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
```

**장점**:
- 실제 필터 동작 테스트 가능
- 인증 플로우 전체 검증 가능

**단점**:
- 테스트 설정 복잡성 증가
- 필터의 모든 의존성 Mock 처리 필요

### 해결책 5: 테스트용 Security Configuration

```java
@TestConfiguration
public class TestSecurityConfig {
    
    @Bean
    @Primary
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionAuthFilter() {
        FilterRegistrationBean<SessionAuthenticationFilter> registration = 
            new FilterRegistrationBean<>();
        registration.setFilter(new SessionAuthenticationFilter(
            mock(SessionStore.class), 
            mock(ObjectMapper.class)));
        registration.addUrlPatterns("/api/secure/*");  // 특정 패턴만 적용
        registration.setOrder(1);
        return registration;
    }
}
```

**장점**:
- 테스트용 필터 설정 세밀한 제어
- 필요한 경우에만 필터 적용

**단점**:
- 복잡한 테스트 설정 구성 필요

## 🎯 권장 해결 방안

### 상황별 최적 해결책

#### 1. **Controller 단위 테스트** (가장 일반적)
```java
@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)  // ✅ 권장
```
- **목적**: Controller 로직만 격리하여 테스트
- **장점**: 빠르고 안정적인 테스트
- **용도**: 대부분의 Controller 테스트

#### 2. **보안 통합 테스트** (필요시에만)
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    
    @Test
    void 인증없이_보호된_API_접근시_401_반환() throws Exception {
        mockMvc.perform(post("/api/rooms"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void 유효한_토큰으로_API_접근시_성공() throws Exception {
        // 실제 로그인 후 토큰 획득
        String token = performLogin();
        
        mockMvc.perform(post("/api/rooms")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated());
    }
}
```
- **목적**: 인증 필터와 Controller 간의 통합 동작 검증
- **용도**: 보안 요구사항 검증, 인증 플로우 테스트

#### 3. **개발 환경 설정**
```java
//@Component  // 개발 시에는 주석 처리
@Profile("prod")  // 운영 환경에서만 활성화
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    // ... 필터 구현
}
```

## 🎓 학습 내용

### 1. Spring Boot 테스트의 필터 동작 원리
- **@WebMvcTest**는 웹 계층의 모든 컴포넌트를 로딩
- **@Component**로 등록된 필터는 자동으로 필터 체인에 포함
- **addFilters = false**로 필터 체인 전체를 비활성화 가능

### 2. 테스트 격리의 중요성
- **단위 테스트**는 테스트 대상의 로직만 검증해야 함
- **의존성은 Mock**으로 처리하여 격리된 환경 구성
- **통합 테스트**에서 컴포넌트 간 상호작용 검증

### 3. 필터 테스트 전략
- **Controller 로직 테스트**: 필터 비활성화
- **필터 로직 테스트**: 별도의 단위 테스트 작성
- **통합 동작 테스트**: @SpringBootTest 활용

### 4. 프로덕션 vs 테스트 환경 분리
- **@Profile** 활용한 환경별 빈 등록
- **@ConditionalOnProperty** 활용한 조건부 활성화
- **application-test.yml** 활용한 테스트 설정 분리

## 🔄 베스트 프랙티스

### 1. 필터 설계 시 고려사항
```java
@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    
    // 테스트하기 쉬운 구조로 설계
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 로직을 별도 메서드로 분리하여 단위 테스트 가능하게 구성
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        AuthenticationResult result = authenticateRequest(request);
        if (result.isSuccess()) {
            processAuthenticatedRequest(request, response, filterChain, result);
        } else {
            handleAuthenticationFailure(response, result.getErrorMessage());
        }
    }
    
    // 별도 메서드들로 분리하여 각각 단위 테스트 가능
    protected boolean shouldSkipFilter(HttpServletRequest request) { ... }
    protected AuthenticationResult authenticateRequest(HttpServletRequest request) { ... }
}
```

### 2. 테스트 구조 설계
```java
// 1. Controller 단위 테스트 (필터 비활성화)
@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest { ... }

// 2. 필터 단위 테스트 (순수 Java 객체 테스트)
@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest { ... }

// 3. 보안 통합 테스트 (필터 + Controller)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest { ... }
```

### 3. 개발 환경 설정
```yaml
# application-dev.yml
app:
  security:
    enabled: false  # 개발 시 편의를 위해 비활성화

# application-prod.yml  
app:
  security:
    enabled: true   # 운영에서는 반드시 활성화
```

## 📈 문제 해결 결과

### Before (문제 상황)
- ❌ Controller 테스트 실행 시 모든 요청이 401 에러
- ❌ 테스트 작성 및 실행 불가
- ❌ 개발 생산성 저하

### After (해결 후)
- ✅ `@AutoConfigureMockMvc(addFilters = false)` 적용
- ✅ AuthContext Mock 처리로 인증 상태 제어
- ✅ 안정적인 Controller 테스트 실행
- ✅ 개발 생산성 향상

## 📚 참고 자료
- [Spring Boot Testing Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [@WebMvcTest Documentation](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html)
- [Spring Security Testing](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
- [MockMvc Filter Configuration](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/setup/MockMvcConfigurer.html)
package com.example.chatapp.infrastructure.session;

import com.example.chatapp.domain.LoginSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemorySessionStore 테스트
 */
class InMemorySessionStoreTest {

    // 테스트 대상 객체
    private InMemorySessionStore sessionStore;

    // 테스트에 사용할 상수 정의
    private static final String TEST_TOKEN = "test-session-token";
    private static final Long TEST_USER_ID = 123L;
    private static final long FUTURE_TIME = System.currentTimeMillis() + 10000; // 10초 후
    private static final long PAST_TIME = System.currentTimeMillis() - 1000; // 1초 전

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 세션스토어 객체 초기화
        sessionStore = new InMemorySessionStore();
    }

    @Test
    @DisplayName("세션 저장 후 동일한 토큰으로 조회하면 저장된 세션이 반환되어야 함")
    void givenSavedSession_whenGetSession_thenReturnSavedSession() {
        // Given
        LoginSession session = new LoginSession(TEST_USER_ID, FUTURE_TIME);
        sessionStore.saveSession(TEST_TOKEN, session);

        // When
        LoginSession retrievedSession = sessionStore.getSession(TEST_TOKEN);

        // Then
        assertNotNull(retrievedSession, "저장된 세션이 조회되어야 합니다");
        assertEquals(TEST_USER_ID, retrievedSession.getUserId(), "조회된 세션의 사용자 ID가 일치해야 합니다");
        assertEquals(FUTURE_TIME, retrievedSession.getExpiryTime(), "조회된 세션의 만료 시간이 일치해야 합니다");
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 시 null이 반환되어야 함")
    void givenNonExistentToken_whenGetSession_thenReturnNull() {
        // Given
        String nonExistentToken = "non-existent-token";

        // When
        LoginSession result = sessionStore.getSession(nonExistentToken);

        // Then
        assertNull(result, "존재하지 않는 토큰으로 조회 시 null이 반환되어야 합니다");
    }

    @Test
    @DisplayName("세션 삭제 후 조회 시 null이 반환되어야 함")
    void givenRemovedSession_whenGetSession_thenReturnNull() {
        // Given
        LoginSession session = new LoginSession(TEST_USER_ID, FUTURE_TIME);
        sessionStore.saveSession(TEST_TOKEN, session);

        // When
        sessionStore.removeSession(TEST_TOKEN);
        LoginSession result = sessionStore.getSession(TEST_TOKEN);

        // Then
        assertNull(result, "삭제된 세션은 null을 반환해야 합니다");
    }

    @Test
    @DisplayName("만료된 세션 정리 후 만료된 세션은 조회되지 않아야 함")
    void givenExpiredSession_whenCleanExpiredSessions_thenExpiredSessionNotRetrievable() {
        // Given
        String expiredToken = "expired-token";
        String validToken = "valid-token";

        // 만료된 세션 (현재 시간보다 이전 시간으로 설정)
        LoginSession expiredSession = new LoginSession(TEST_USER_ID, PAST_TIME);
        sessionStore.saveSession(expiredToken, expiredSession);

        // 유효한 세션
        LoginSession validSession = new LoginSession(TEST_USER_ID + 1, FUTURE_TIME);
        sessionStore.saveSession(validToken, validSession);

        // When
        sessionStore.cleanExpiredSessions();

        // Then
        assertNull(sessionStore.getSession(expiredToken), "만료된 세션은 정리 후 조회되지 않아야 합니다");
        assertNotNull(sessionStore.getSession(validToken), "유효한 세션은 정리 후에도 조회되어야 합니다");
    }

    @Test
    @DisplayName("새 세션으로 기존 세션 덮어쓰기 시 새 세션이 조회되어야 함")
    void givenExistingSession_whenSaveNewSession_thenNewSessionReturned() {
        // Given
        LoginSession oldSession = new LoginSession(TEST_USER_ID, FUTURE_TIME - 5000);
        sessionStore.saveSession(TEST_TOKEN, oldSession);

        // 새 세션 생성 (다른 사용자 ID와 만료 시간)
        Long newUserId = 456L;
        LoginSession newSession = new LoginSession(newUserId, FUTURE_TIME);

        // When
        sessionStore.saveSession(TEST_TOKEN, newSession);
        LoginSession retrievedSession = sessionStore.getSession(TEST_TOKEN);

        // Then
        assertNotNull(retrievedSession, "세션이 조회되어야 합니다");
        assertEquals(newUserId, retrievedSession.getUserId(), "새 세션의 사용자 ID가 조회되어야 합니다");
        assertEquals(FUTURE_TIME, retrievedSession.getExpiryTime(), "새 세션의 만료 시간이 조회되어야 합니다");
        assertNotEquals(TEST_USER_ID, retrievedSession.getUserId(), "이전 세션의 사용자 ID가 아니어야 합니다");
    }

    @Test
    @DisplayName("세션 저장소에 여러 세션을 저장하고 각각 올바르게 조회되어야 함")
    void givenMultipleSessions_whenGetEachSession_thenReturnCorrectSessions() {
        // Given
        String token1 = "token-1";
        String token2 = "token-2";
        String token3 = "token-3";

        LoginSession session1 = new LoginSession(101L, FUTURE_TIME);
        LoginSession session2 = new LoginSession(102L, FUTURE_TIME);
        LoginSession session3 = new LoginSession(103L, FUTURE_TIME);

        sessionStore.saveSession(token1, session1);
        sessionStore.saveSession(token2, session2);
        sessionStore.saveSession(token3, session3);

        // When & Then
        assertEquals(101L, sessionStore.getSession(token1).getUserId(), "첫 번째 세션은 올바른 사용자 ID를 가져야 합니다");
        assertEquals(102L, sessionStore.getSession(token2).getUserId(), "두 번째 세션은 올바른 사용자 ID를 가져야 합니다");
        assertEquals(103L, sessionStore.getSession(token3).getUserId(), "세 번째 세션은 올바른 사용자 ID를 가져야 합니다");
    }
}

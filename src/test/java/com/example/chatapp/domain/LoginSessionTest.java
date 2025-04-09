package com.example.chatapp.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginSession 클래스 단위 테스트
 */
class LoginSessionTest {

    private LoginSession session;

    private static final Long TEST_USER_ID = 123L;
    private static final long TEN_SECONDS_IN_MILLIS = 10000L;
    private static final long ONE_SECOND_IN_MILLIS = 1000L;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 세션 객체 초기화 (기본 만료 시간은 10초 후로 설정)
        long expiryTime = System.currentTimeMillis() + TEN_SECONDS_IN_MILLIS;
        session = new LoginSession(TEST_USER_ID, expiryTime);
    }

    @Test
    @DisplayName("세션이 만료되지 않았을 때 isExpired는 false를 반환해야 함")
    void givenFutureExpirationTime_whenCheckIsExpired_thenReturnFalse() {
        // Given
        long futureTime = System.currentTimeMillis() + TEN_SECONDS_IN_MILLIS;
        session.setExpirationTime(futureTime);

        // When
        boolean isExpired = session.isExpired();

        // Then
        assertFalse(isExpired, "만료 시간이 미래인 경우, 세션은 만료되지 않아야 합니다");
    }

    @Test
    @DisplayName("세션이 만료되었을 때 isExpired는 true를 반환해야 함")
    void givenPastExpirationTime_whenCheckIsExpired_thenReturnTrue() {
        // Given
        long pastTime = System.currentTimeMillis() - ONE_SECOND_IN_MILLIS;
        session.setExpirationTime(pastTime);

        // When
        boolean isExpired = session.isExpired();

        // Then
        assertTrue(isExpired, "만료 시간이 과거인 경우, 세션은 만료되어야 합니다");
    }

    @Test
    @DisplayName("사용자 ID가 올바르게 반환되어야 함")
    void givenSession_whenGetUserId_thenReturnCorrectUserId() {
        // Given
        // setUp에서 이미 초기화됨

        // When
        Long userId = session.getUserId();

        // Then
        assertEquals(TEST_USER_ID, userId, "사용자 ID가 정확하게 반환되어야 합니다");
    }

    @Test
    @DisplayName("getId 메서드는 사용자 ID와 동일한 값을 반환해야 함")
    void givenSession_whenGetId_thenReturnSameAsUserId() {
        // Given
        // setUp에서 이미 초기화됨

        // When
        Long id = session.getId();
        Long userId = session.getUserId();

        // Then
        assertEquals(userId, id, "getId는 사용자 ID와 동일한 값을 반환해야 합니다");
    }

    @Test
    @DisplayName("만료 시간 설정 후 getExpiryTime은 설정된 시간을 반환해야 함")
    void givenExpiryTime_whenSetAndGet_thenReturnSameValue() {
        // Given
        long newExpiryTime = System.currentTimeMillis() + 60000; // 60초 후

        // When
        session.setExpirationTime(newExpiryTime);
        long returnedExpiryTime = session.getExpiryTime();

        // Then
        assertEquals(newExpiryTime, returnedExpiryTime, "설정한 만료 시간과 반환된 만료 시간이 일치해야 합니다");
    }

    @Test
    @DisplayName("생성자에서 설정한 만료 시간이 올바르게 설정되어야 함")
    void givenSessionCreatedWithExpiryTime_whenGetExpiryTime_thenReturnCorrectValue() {
        // Given
        long expectedExpiryTime = System.currentTimeMillis() + 30000; // 30초 후
        LoginSession newSession = new LoginSession(TEST_USER_ID, expectedExpiryTime);

        // When
        long actualExpiryTime = newSession.getExpiryTime();

        // Then
        assertEquals(expectedExpiryTime, actualExpiryTime, "생성자에서 설정한 만료 시간이 정확히 설정되어야 합니다");
    }
}

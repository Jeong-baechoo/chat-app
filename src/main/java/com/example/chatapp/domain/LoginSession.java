package com.example.chatapp.domain;

import lombok.Getter;

/**
 * 사용자 로그인 세션 정보를 담는 클래스
 * 인증된 사용자의 상태를 유지하기 위한 객체
 */


@Getter
public class LoginSession {
    /**
     * -- GETTER --
     *  사용자 ID 조회
     *
     * @return 사용자 ID
     */
    private final Long userId;
    /**
     * -- GETTER --
     *  세션 만료 시간 조회
     *
     * @return 만료 시간 (밀리초 단위 timestamp)
     */
    private long expiryTime;

    public LoginSession(Long userId, long expiryTime) {
        this.userId = userId;
        this.expiryTime = expiryTime;
    }


    /**
     * 세션 만료 여부 확인
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public void setExpirationTime(long futureTime) {
        this.expiryTime = futureTime;
    }

    public Long getId() {
        return userId;
    }
}

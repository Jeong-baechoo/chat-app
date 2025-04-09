package com.example.chatapp.infrastructure.session;

import com.example.chatapp.domain.LoginSession;

/**
 * 로그인 세션 저장소에 대한 인터페이스
 * 다양한 세션 저장 메커니즘(로컬 메모리, Redis 등)에 대한 추상화 제공
 */
public interface SessionStore {
    /**
     * 세션을 저장
     * @param token 세션 식별자 토큰
     * @param session 저장할 로그인 세션 객체
     */
    void saveSession(String token, LoginSession session);
    
    /**
     * 세션을 조회
     * @param token 세션 식별자 토큰
     * @return 로그인 세션 객체 또는 null(세션이 없는 경우)
     */
    LoginSession getSession(String token);
    
    /**
     * 세션을 삭제
     * @param token 삭제할 세션의 토큰
     */
    void removeSession(String token);
    
    /**
     * 만료된 세션 정리
     * 주기적으로 실행되어 만료된 세션을 제거
     */
    void cleanExpiredSessions();
}
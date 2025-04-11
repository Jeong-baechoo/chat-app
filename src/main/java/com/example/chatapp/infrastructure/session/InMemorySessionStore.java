package com.example.chatapp.infrastructure.session;

import com.example.chatapp.domain.LoginSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬 메모리 맵을 사용한 세션 저장소 구현체
 * 단일 서버 환경에 적합
 */
@Component
public class InMemorySessionStore implements SessionStore {
    private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void saveSession(String token, LoginSession session) {
        sessions.put(token, session);
    }

    @Override
    public LoginSession getSession(String token) {
        return sessions.get(token);
    }

    @Override
    public List<LoginSession> getAllSessions() {
        return List.copyOf(sessions.values());
    }

    @Override
    public void removeSession(String token) {
        sessions.remove(token);
    }

    @Override
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15분마다 실행
    public void cleanExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry ->
                entry.getValue().getExpiryTime() < currentTime);
    }
}

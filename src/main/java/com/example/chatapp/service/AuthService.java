package com.example.chatapp.service;

import com.example.chatapp.domain.LoginSession;
import com.example.chatapp.domain.User;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.infrastructure.auth.PasswordEncoder;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionStore sessionStore;
    private final PasswordEncoder passwordEncoder;


    // 세션 만료 시간 (기본값: 30분)
    private static final long SESSION_EXPIRY_TIME = 30 * 60 * 1000;

    /**
     * 사용자 로그인 처리
     * @param username 사용자명
     * @param password 비밀번호
     * @return 인증 응답 정보 (토큰, 사용자 ID, 사용자명 등)
     * @throws UnauthorizedException 인증 실패 시 발생
     */
    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("비밀번호가 일치하지 않습니다");
        }

        return createAuthResponse(user);
    }

    /**
     * 회원가입 처리
     * @param username 사용자명
     * @param password 비밀번호
     * @return 인증 응답 정보 (토큰, 사용자 ID, 사용자명 등)
     * @throws IllegalArgumentException 이미 사용 중인 사용자명인 경우 발생
     */
    public Map<String, Object> signup(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();

        return createAuthResponse(userRepository.save(user));
    }

    /**
     * 세션 토큰 유효성 검증
     * @param token 세션 토큰
     * @return 사용자 정보 (ID, 사용자명)
     * @throws UnauthorizedException 토큰이 유효하지 않거나 세션이 만료된 경우 발생
     */
    public Map<String, Object> validateToken(String token) {
        LoginSession session = sessionStore.getSession(token);

        if (session == null || session.isExpired()) {
            // 만료된 세션은 삭제
            if (session != null) {
                sessionStore.removeSession(token);
            }
            throw new UnauthorizedException("유효하지 않은 세션입니다");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("valid", true);
        return response;
    }

    /**
     * 로그아웃 처리
     * @param token 세션 토큰
     */
    public void logout(String token) {
        sessionStore.removeSession(token);
    }

    /**
     * 인증 응답 생성
     * 세션을 생성하고 토큰과 함께 사용자 정보를 반환
     *
     * @param user 인증된 사용자
     * @return 인증 응답 정보
     */
    private Map<String, Object> createAuthResponse(User user) {
        // 세션 생성
        String sessionToken = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + SESSION_EXPIRY_TIME;

        // 세션 객체 생성 및 저장
        LoginSession session = new LoginSession(user.getId(), expiryTime);
        sessionStore.saveSession(sessionToken, session);

        Map<String, Object> response = new HashMap<>();
        response.put("token", sessionToken);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("expiresAt", expiryTime);
        return response;
    }

    /**
     * 세션 토큰으로 사용자 정보 조회
     * @param token 세션 토큰
     * @return 사용자 객체 또는 null (세션이 없거나 만료된 경우)
     */
    public User getUserBySessionToken(String token) {
        LoginSession session = sessionStore.getSession(token);

        if (session == null || session.isExpired()) {
            // 세션이 없거나 만료됨
            if (session != null) {
                // 만료된 세션은 제거
                sessionStore.removeSession(token);
            }
            return null;
        }

        // 세션에서 사용자 ID를 가져와 사용자 정보 조회
        return userRepository.findById(session.getUserId()).orElse(null);
    }

    /**
     * 세션이 유효한지 확인
     * @param token 세션 토큰
     * @return 유효 여부
     */
    public boolean isValidSession(String token) {
        LoginSession session = sessionStore.getSession(token);
        return session != null && !session.isExpired();
    }
}

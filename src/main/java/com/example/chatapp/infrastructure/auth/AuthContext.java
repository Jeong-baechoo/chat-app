package com.example.chatapp.infrastructure.auth;

import com.example.chatapp.domain.User;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 현재 인증된 사용자 정보에 접근하기 위한 컴포넌트
 * 요청 스코프로 지정하여 각 요청마다 새로운 인스턴스가 생성됨
 * JwtAuthenticationFilter에서 설정한 userId 속성을 활용
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class AuthContext {

    private final HttpServletRequest request;
    private final UserRepository userRepository;

    private static final String USER_ID_ATTRIBUTE = "userId";

    /**
     * 현재 인증된 사용자의 ID를 반환
     * @return 사용자 ID
     * @throws UnauthorizedException 인증되지 않은 경우 예외 발생
     */
    public Long getCurrentUserId() {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다");
        }
        return (Long) userId;
    }

    /**
     * 사용자가 인증되었는지 확인
     * @return 인증 여부
     */
    public boolean isAuthenticated() {
        return request.getAttribute(USER_ID_ATTRIBUTE) != null;
    }

    /**
     * 현재 인증된 사용자 정보를 반환
     * @return User 객체
     * @throws UnauthorizedException 인증되지 않은 경우 예외 발생
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자 정보를 찾을 수 없습니다"));
    }

    /**
     * 현재 인증된 사용자 정보를 Optional로 반환 (예외 없음)
     * @return Optional<User> 객체
     */
    public java.util.Optional<User> getCurrentUserOptional() {
        if (!isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return userRepository.findById(getCurrentUserId());
    }
}

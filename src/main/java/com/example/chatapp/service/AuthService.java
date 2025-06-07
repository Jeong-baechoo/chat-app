package com.example.chatapp.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.infrastructure.auth.PasswordEncoder;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 로그인 처리
     * @param username 사용자명
     * @param password 비밀번호
     * @return 인증 응답 정보 (JWT 토큰, 사용자 ID, 사용자명 등)
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
     * @return 인증 응답 정보 (JWT 토큰, 사용자 ID, 사용자명 등)
     * @throws IllegalArgumentException 이미 사용 중인 사용자명인 경우 발생
     */
    public Map<String, Object> signup(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = User.create(username, encodedPassword);

        return createAuthResponse(userRepository.save(user));
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 사용자 정보 (ID, 사용자명)
     * @throws UnauthorizedException 토큰이 유효하지 않거나 만료된 경우 발생
     */
    public Map<String, Object> validateToken(String token) {
        String cleanToken = jwtTokenProvider.extractToken(token);
        
        if (!jwtTokenProvider.validateToken(cleanToken)) {
            throw new UnauthorizedException("유효하지 않은 JWT 토큰입니다");
        }

        Long userId = jwtTokenProvider.getUserId(cleanToken);
        String username = jwtTokenProvider.getUsername(cleanToken);

        // 사용자가 여전히 존재하는지 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("valid", true);
        response.put("expiresAt", jwtTokenProvider.getExpirationDate(cleanToken));
        return response;
    }

    /**
     * 로그아웃 처리 (JWT는 Stateless이므로 서버에서 할 일이 없음)
     * 클라이언트에서 토큰을 삭제해야 함
     * @param token JWT 토큰
     */
    public void logout(String token) {
        // JWT는 서버에서 상태를 관리하지 않으므로 로그아웃은 클라이언트 측에서 처리
        // 필요하다면 블랙리스트 기능을 추가할 수 있음
    }

    /**
     * 인증 응답 생성
     * JWT 토큰을 생성하고 사용자 정보와 함께 반환
     *
     * @param user 인증된 사용자
     * @return 인증 응답 정보
     */
    private Map<String, Object> createAuthResponse(User user) {
        String jwtToken = jwtTokenProvider.createToken(user.getId(), user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwtToken);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("expiresAt", jwtTokenProvider.getExpirationDate(jwtToken));
        return response;
    }

    /**
     * JWT 토큰으로 사용자 정보 조회
     * @param token JWT 토큰
     * @return 사용자 객체 또는 null (토큰이 유효하지 않은 경우)
     */
    public User getUserByToken(String token) {
        try {
            String cleanToken = jwtTokenProvider.extractToken(token);
            
            if (!jwtTokenProvider.validateToken(cleanToken)) {
                return null;
            }

            Long userId = jwtTokenProvider.getUserId(cleanToken);
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JWT 토큰이 유효한지 확인
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean isValidToken(String token) {
        try {
            String cleanToken = jwtTokenProvider.extractToken(token);
            return jwtTokenProvider.validateToken(cleanToken);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID 또는 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            String cleanToken = jwtTokenProvider.extractToken(token);
            if (jwtTokenProvider.validateToken(cleanToken)) {
                return jwtTokenProvider.getUserId(cleanToken);
            }
        } catch (Exception e) {
            // 토큰 파싱 실패
        }
        return null;
    }
}

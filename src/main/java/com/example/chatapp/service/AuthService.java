package com.example.chatapp.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.response.AuthResponse;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.infrastructure.auth.PasswordEncoder;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 로그인 처리
     * @param username 사용자명
     * @param password 비밀번호
     * @return 인증 응답 DTO
     * @throws UnauthorizedException 인증 실패 시 발생
     */
    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> UnauthorizedException.invalidCredentials());

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw UnauthorizedException.invalidCredentials();
        }

        return createAuthResponse(user);
    }

    /**
     * 회원가입 처리
     * @param username 사용자명
     * @param password 비밀번호
     * @return 인증 응답 DTO
     * @throws IllegalArgumentException 이미 사용 중인 사용자명인 경우 발생
     */
    @Transactional
    public AuthResponse signup(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw UserException.alreadyExists(username);
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = User.create(username, encodedPassword);

        return createAuthResponse(userRepository.save(user));
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
     * @return 인증 응답 DTO
     */
    private AuthResponse createAuthResponse(User user) {
        String jwtToken = jwtTokenProvider.createToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .username(user.getUsername())
                .expiresAt(jwtTokenProvider.getExpirationDate(jwtToken))
                .valid(true)
                .build();
    }

    /**
     * JWT 토큰으로 사용자 정보 조회
     * @param token JWT 토큰
     * @return 사용자 객체 또는 null (토큰이 유효하지 않은 경우)
     */
}

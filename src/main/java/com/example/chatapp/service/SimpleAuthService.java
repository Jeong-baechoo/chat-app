package com.example.chatapp.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.security.SimpleJwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimpleAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpleJwtProvider jwtProvider;

    // 로그인 처리
    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        user.setStatus(UserStatus.ONLINE); // 로그인 시 상태 변경

        String token = jwtProvider.createToken(user.getId(), user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());

        return response;
    }

    // 회원가입 처리
    public Map<String, Object> signup(String username, String password) {
        // 사용자명 중복 검사
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다.");
        }

        // 신규 사용자 생성
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();

        // 저장
        User savedUser = userRepository.save(user);

        // 토큰 생성 및 응답
        String token = jwtProvider.createToken(savedUser.getId(), savedUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", savedUser.getId());
        response.put("username", savedUser.getUsername());

        return response;
    }

    // 토큰 검증 및 사용자 정보 조회
    public Map<String, Object> validateToken(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        String username = jwtProvider.extractUsername(token);
        Long userId = jwtProvider.extractUserId(token);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", username);

        return response;
    }
}

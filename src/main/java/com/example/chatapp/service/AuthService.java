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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpleJwtProvider jwtProvider;

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        user.setStatus(UserStatus.ONLINE);
        return createAuthResponse(user);
    }

    public Map<String, Object> signup(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();

        return createAuthResponse(userRepository.save(user));
    }

    public Map<String, Object> validateToken(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", jwtProvider.extractUserId(token));
        response.put("username", jwtProvider.extractUsername(token));
        return response;
    }

    private Map<String, Object> createAuthResponse(User user) {
        String token = jwtProvider.createToken(user.getId(), user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        return response;
    }
}

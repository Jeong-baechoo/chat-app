package com.example.chatapp.infrastructure.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 비밀번호 인코딩을 SHA-256 해시 알고리즘으로 처리하는 구현체
 */
@Component
public class SHA256PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("비밀번호 암호화 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        String newEncodedPassword = encode(rawPassword);
        return newEncodedPassword.equals(encodedPassword);
    }
}

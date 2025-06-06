package com.example.chatapp.infrastructure.auth;

/**
 * 비밀번호 인코딩을 처리하는 인터페이스
 */
public interface PasswordEncoder {

    /**
     * 비밀번호를 암호화합니다.
     * @param rawPassword 평문 비밀번호
     * @return 암호화된 비밀번호
     */
    String encode(String rawPassword);

    /**
     * 입력된 평문 비밀번호가 암호화된 비밀번호와 일치하는지 확인합니다.
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 암호화된 비밀번호
     * @return 일치 여부
     */
    boolean matches(String rawPassword, String encodedPassword);
}
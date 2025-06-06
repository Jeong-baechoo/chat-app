package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private User(String username, String encodedPassword) {
        validateUsername(username);
        validatePassword(encodedPassword);
        this.username = username;
        this.password = encodedPassword;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 새로운 사용자 생성 (도메인 서비스에서 호출)
     * 이미 암호화된 비밀번호를 받아 사용자를 생성합니다.
     */
    public static User create(String username, String encodedPassword) {
        return new User(username, encodedPassword);
    }

    /**
     * 비밀번호 변경 (이미 검증되고 암호화된 비밀번호로)
     */
    public void changePassword(String newEncodedPassword) {
        validatePassword(newEncodedPassword);
        this.password = newEncodedPassword;
    }

    /**
     * 암호화된 비밀번호 직접 비교 (애플리케이션 서비스에서 암호화 후 호출)
     */
    public boolean isPasswordMatch(String encodedPassword) {
        return this.password.equals(encodedPassword);
    }

    /**
     * 사용자명 변경
     */
    public void changeUsername(String newUsername) {
        validateUsername(newUsername);
        this.username = newUsername;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    private static void validateUsername(String username) {
        if (username ==null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("사용자명은 3-50자 사이여야 합니다");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("사용자명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }
    }

    private static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("비밀번호는 최소 6자 이상이어야 합니다");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        
        // username은 unique이므로 비즈니스 키로 사용
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        // 불변인 username 기반 해시코드
        return Objects.hash(username);
    }
}

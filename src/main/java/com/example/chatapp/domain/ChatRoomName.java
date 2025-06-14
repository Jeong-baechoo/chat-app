package com.example.chatapp.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 채팅방명을 표현하는 값 객체
 * 채팅방명에 대한 비즈니스 규칙을 캡슐화합니다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomName {
    
    private String value;
    
    private ChatRoomName(String value) {
        this.value = value;
    }
    
    /**
     * 채팅방명 생성
     */
    public static ChatRoomName of(String name) {
        validateName(name);
        return new ChatRoomName(name);
    }
    
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방명은 필수입니다");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("채팅방명은 최소 2자 이상이어야 합니다");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("채팅방명은 100자를 초과할 수 없습니다");
        }
        if (containsInvalidCharacters(name)) {
            throw new IllegalArgumentException("채팅방명에 사용할 수 없는 문자가 포함되어 있습니다");
        }
    }
    
    private static boolean containsInvalidCharacters(String name) {
        // 기본적인 특수문자 제한
        return name.matches(".*[<>\"'&].*");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomName that = (ChatRoomName) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}

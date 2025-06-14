package com.example.chatapp.domain.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.exception.DomainException;
import org.springframework.stereotype.Component;

/**
 * 사용자 도메인 서비스
 * 사용자와 관련된 순수한 도메인 규칙을 처리합니다.
 */
@Component
public class UserDomainService {
    
    /**
     * 비밀번호 변경
     * 도메인 서비스를 통해서만 비밀번호 변경이 가능하도록 제어
     * 
     * @param user 사용자
     * @param currentEncodedPassword 현재 암호화된 비밀번호
     * @param newEncodedPassword 새로운 암호화된 비밀번호
     */
    public void changePassword(User user, String currentEncodedPassword, String newEncodedPassword) {
        if (!user.isPasswordMatch(currentEncodedPassword)) {
            throw new DomainException("현재 비밀번호가 일치하지 않습니다");
        }
        
        if (currentEncodedPassword.equals(newEncodedPassword)) {
            throw new DomainException("새 비밀번호는 현재 비밀번호와 달라야 합니다");
        }
        
        user.changePassword(newEncodedPassword);
    }
    
    /**
     * 사용자명 변경
     * 도메인 서비스를 통해서만 사용자명 변경이 가능하도록 제어
     * 
     * @param user 사용자
     * @param newUsername 새로운 사용자명
     */
    public void changeUsername(User user, String newUsername) {
        if (user.getUsername().equals(newUsername)) {
            throw new DomainException("새 사용자명은 현재 사용자명과 달라야 합니다");
        }
        
        user.changeUsername(newUsername);
    }
}
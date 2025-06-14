package com.example.chatapp.service.impl;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.service.UserDomainService;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.auth.PasswordEncoder;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("사용자(ID: " + id + ")를 찾을 수 없습니다."));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserException("사용자(ID: " + id + ")를 찾을 수 없습니다.");
        }

        userRepository.deleteById(id);
        log.debug("사용자 삭제 완료: id={}", id);
    }

    @Override
    public List<UserResponse> findLoggedInUsers() {
        // JWT Stateless 환경에서는 로그인된 사용자 추적이 불가능
        // 필요시 Redis 등을 이용한 별도 구현 필요
        log.warn("findLoggedInUsers는 JWT Stateless 환경에서 지원되지 않습니다");
        return List.of();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자(ID: " + userId + ")를 찾을 수 없습니다."));
        
        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserException("현재 비밀번호가 일치하지 않습니다");
        }
        
        // 새 비밀번호 인코딩
        String newEncodedPassword = passwordEncoder.encode(newPassword);
        
        // 도메인 서비스를 통한 비밀번호 변경 (이미 검증됨)
        userDomainService.changePassword(user, user.getPassword(), newEncodedPassword);
        
        log.info("사용자 비밀번호 변경 완료: userId={}", userId);
    }

    @Override
    @Transactional
    public void changeUsername(Long userId, String newUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자(ID: " + userId + ")를 찾을 수 없습니다."));
        
        // 새로운 사용자명이 이미 존재하는지 확인
        if (userRepository.existsByUsername(newUsername)) {
            throw new UserException("이미 사용 중인 사용자명입니다: " + newUsername);
        }
        
        userDomainService.changeUsername(user, newUsername);
        
        log.info("사용자명 변경 완료: userId={}, newUsername={}", userId, newUsername);
    }
}

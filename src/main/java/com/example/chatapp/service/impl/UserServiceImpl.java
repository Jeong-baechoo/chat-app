// src/main/java/com/example/chatapp/service/impl/UserServiceImpl.java
package com.example.chatapp.service.impl;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.dto.request.UserCreateRequest;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 중복 사용자명 체크
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserException("이미 존재하는 사용자명입니다.");
        }

        // 사용자 엔티티 생성
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.OFFLINE)
                .build();

        // 저장 및 응답
        User savedUser = userRepository.save(user);
        log.debug("사용자 생성 완료: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        List<User> users = userRepository.findAll();
        log.debug("전체 사용자 조회: {}명 조회됨", users.size());

        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
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
    public UserResponse updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("사용자(ID: " + id + ")를 찾을 수 없습니다."));

        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        log.debug("사용자 상태 업데이트: id={}, status={}", id, status);

        return userMapper.toResponse(updatedUser);
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
}

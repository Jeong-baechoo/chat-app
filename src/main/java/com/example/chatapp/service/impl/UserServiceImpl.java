package com.example.chatapp.service.impl;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.UserException;
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
}

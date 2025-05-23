package com.example.chatapp.service.impl;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SessionStore sessionStore;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        List<User> users = userRepository.findAll();

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
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserException("사용자(ID: " + id + ")를 찾을 수 없습니다.");
        }

        userRepository.deleteById(id);
        log.debug("사용자 삭제 완료: id={}", id);
    }

    @Override
    public List<UserResponse> findLoggedInUsers() {
        List<UserResponse> list = sessionStore.getAllSessions().stream()
                .map(session -> userRepository.findById(session.getUserId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(userMapper::toResponse)
                .toList();
        log.debug("전체 사용자 조회: {}명 조회됨", list.size());

        return list;
    }
}

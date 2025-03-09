package com.example.chatapp.repository;

import com.example.chatapp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 사용자명으로 사용자 찾기
    Optional<User> findByUsername(String username);
}

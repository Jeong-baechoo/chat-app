package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {
    boolean existsByUserIdAndChatRoomId(Long userId, Long chatRoomId);
    List<ChatRoomParticipant> findByUserId(Long userId);

    Optional<ChatRoomParticipant> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);
}

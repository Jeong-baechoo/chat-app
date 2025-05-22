package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomParticipant;
import com.example.chatapp.domain.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {
    boolean existsByUserIdAndChatRoomId(Long userId, Long chatRoomId);
    List<ChatRoomParticipant> findByUserId(Long userId);


    boolean existsByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Query("SELECT crp FROM ChatRoomParticipant crp " +
            "WHERE crp.user.id = :userId AND crp.chatRoom.id = :chatRoomId")
    Optional<ChatRoomParticipant> findByUserIdAndChatRoomId(
            @Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);
}

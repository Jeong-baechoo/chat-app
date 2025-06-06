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
    
    // 기본 조회 (N+1 문제 발생 가능)
    List<ChatRoomParticipant> findByUserId(Long userId);

    // 사용자와 채팅방 정보를 함께 조회 (FETCH JOIN) - 최적화됨
    @Query("SELECT crp FROM ChatRoomParticipant crp " +
           "LEFT JOIN FETCH crp.user " +
           "LEFT JOIN FETCH crp.chatRoom " +
           "WHERE crp.user.id = :userId")
    List<ChatRoomParticipant> findByUserIdWithUserAndChatRoom(@Param("userId") Long userId);

    boolean existsByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Query("SELECT crp FROM ChatRoomParticipant crp " +
            "WHERE crp.user.id = :userId AND crp.chatRoom.id = :chatRoomId")
    Optional<ChatRoomParticipant> findByUserIdAndChatRoomId(
            @Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);

    // 사용자와 채팅방 정보를 함께 조회 (FETCH JOIN) - 최적화됨
    @Query("SELECT crp FROM ChatRoomParticipant crp " +
           "LEFT JOIN FETCH crp.user " +
           "LEFT JOIN FETCH crp.chatRoom " +
           "WHERE crp.user.id = :userId AND crp.chatRoom.id = :chatRoomId")
    Optional<ChatRoomParticipant> findByUserIdAndChatRoomIdWithUserAndChatRoom(
            @Param("userId") Long userId,
            @Param("chatRoomId") Long chatRoomId);
}

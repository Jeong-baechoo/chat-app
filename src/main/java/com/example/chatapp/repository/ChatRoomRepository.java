package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

    List<ChatRoom> findByType(ChatRoomType type);
}

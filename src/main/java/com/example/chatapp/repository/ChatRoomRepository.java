package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants WHERE cr.id IN :ids")
    List<ChatRoom> findAllByIdsWithParticipants(List<Long> ids);
}

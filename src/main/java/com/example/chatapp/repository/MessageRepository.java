package com.example.chatapp.repository;

import com.example.chatapp.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // 채팅방 ID로 메시지 조회 (페이지네이션)
    Page<Message> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);

    // 채팅방 ID로 최근 메시지 조회 (제한된 개수)
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.timestamp DESC")
    List<Message> findTopByChatRoomIdOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 위 메서드의 편의 메서드
    default List<Message> findTopByChatRoomIdOrderByTimestampDesc(Long chatRoomId, int limit) {
        return findTopByChatRoomIdOrderByTimestampDesc(chatRoomId,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // 발신자 ID로 메시지 조회 (페이지네이션)
    Page<Message> findBySenderIdOrderByTimestampDesc(Long senderId, Pageable pageable);
}

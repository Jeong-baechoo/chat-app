package com.example.chatapp.repository;

import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // 채팅방 ID로 메시지 조회 (페이지네이션) - N+1 문제 발생 가능
    Page<Message> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);

    // 채팅방 ID로 메시지와 발신자, 채팅방 정보를 함께 조회 (FETCH JOIN) - 최적화됨
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "LEFT JOIN FETCH m.chatRoom " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "ORDER BY m.timestamp DESC")
    List<Message> findByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 채팅방 ID로 최근 메시지 조회 (제한된 개수) - N+1 문제 발생 가능
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.timestamp DESC")
    List<Message> findTopByChatRoomIdOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 채팅방 ID로 최근 메시지 조회 (FETCH JOIN) - 최적화됨
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "LEFT JOIN FETCH m.chatRoom " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "ORDER BY m.timestamp DESC")
    List<Message> findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 최적화된 편의 메서드 (FETCH JOIN 사용) - 권장
    default List<Message> findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(Long chatRoomId, int limit) {
        return findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(chatRoomId,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // 기존 메서드 (N+1 문제 발생 가능) - 하위 호환성을 위해 유지하되 deprecated 처리
    @Deprecated(since = "1.0", forRemoval = true)
    default List<Message> findTopByChatRoomIdOrderByTimestampDesc(Long chatRoomId, int limit) {
        return findTopByChatRoomIdOrderByTimestampDesc(chatRoomId,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // 발신자 ID로 메시지 조회 (페이지네이션) - N+1 문제 발생 가능
    Page<Message> findBySenderIdOrderByTimestampDesc(Long senderId, Pageable pageable);

    // 발신자 ID로 메시지와 채팅방 정보를 함께 조회 (FETCH JOIN) - 최적화됨
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "LEFT JOIN FETCH m.chatRoom " +
           "WHERE m.sender.id = :senderId " +
           "ORDER BY m.timestamp DESC")
    Page<Message> findBySenderIdWithSenderAndRoomOrderByTimestampDesc(@Param("senderId") Long senderId, Pageable pageable);

    List<Message> findByStatus(MessageStatus status);

    // 참고: EntityGraph 방식은 FETCH JOIN으로 통일하기 위해 주석 처리
    // 필요시 팀 컨벤션에 따라 활성화할 수 있음
    
    // @EntityGraph(attributePaths = {"sender", "chatRoom"})
    // @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.timestamp DESC")
    // List<Message> findByChatRoomIdWithEntityGraph(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // @EntityGraph(attributePaths = {"sender", "chatRoom"})
    // Page<Message> findByIdIsNotNull(Pageable pageable);
}

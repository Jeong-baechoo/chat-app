package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    /**
     * 모든 채팅방의 기본 정보만 조회 (참가자 정보 없이)
     */
    @Query("SELECT cr FROM ChatRoom cr")
    List<ChatRoom> findAllRoomsSimple();

    /**
     * 채팅방 기본 정보만 DTO로 직접 조회
     */
    @Query("SELECT new com.example.chatapp.dto.response.ChatRoomSimpleResponse(cr.id, cr.name, cr.type) FROM ChatRoom cr")
    List<ChatRoomSimpleResponse> findAllRoomsAsSimpleDto();

    /**
     * 특정 ID 목록에 해당하는 채팅방과 참여자를 함께 조회 (FETCH JOIN)
     */
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants WHERE cr.id IN :ids")
    List<ChatRoom> findAllByIdsWithParticipants(List<Long> ids);

    /**
     * 모든 채팅방과 참여자 정보를 함께 조회 (FETCH JOIN)
     */
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants")
    List<ChatRoom> findAllWithParticipants();

    /**
     * 단일 채팅방과 참여자 정보를 함께 조회 (FETCH JOIN)
     */
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants WHERE cr.id = :id")
    Optional<ChatRoom> findByIdWithParticipants(Long id);

    /**
     * EntityGraph를 사용하여 참여자와 메시지까지 함께 로딩
     */
    @EntityGraph(attributePaths = {"participants", "participants.user", "messages"})
    Optional<ChatRoom> findWithParticipantsAndMessagesById(Long id);

    /**
     * 특정 사용자가 참여한 모든 채팅방 조회 (FETCH JOIN)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN FETCH cr.participants p " +
           "WHERE p.user.id = :userId")
    List<ChatRoom> findAllByParticipantUserId(Long userId);
}

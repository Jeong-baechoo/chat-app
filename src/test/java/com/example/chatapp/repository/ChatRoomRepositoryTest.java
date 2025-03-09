package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomType;
import com.example.chatapp.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("채팅방 생성 및 조회 테스트")
    public void testCreateAndFindChatRoom() {
        // Given
        ChatRoom chatRoom = ChatRoom.builder()
                .name("Test Chat Room")
                .type(ChatRoomType.GROUP)
                .build();

        // When
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Then
        assertThat(savedChatRoom).isNotNull();
        assertThat(savedChatRoom.getName()).isEqualTo("Test Chat Room");
        assertThat(savedChatRoom.getId()).isNotNull();
    }
}

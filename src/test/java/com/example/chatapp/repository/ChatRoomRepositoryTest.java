package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private ChatRoom savedRoom;

    @BeforeEach
    void setUp() {
        ChatRoom room = ChatRoom.builder()
                .name("테스트 방")
                .type(ChatRoomType.GROUP)
                .build();

        savedRoom = chatRoomRepository.save(room); // 공통 테스트용 데이터 저장
    }

    @Test
    public void testFindById() {
        // given
        Long roomId = savedRoom.getId();

        // when
        Optional<ChatRoom> foundRoom = chatRoomRepository.findById(roomId);

        // then
        assertThat(foundRoom).isPresent();
        assertThat(foundRoom.get().getName()).isEqualTo(savedRoom.getName());
    }
}

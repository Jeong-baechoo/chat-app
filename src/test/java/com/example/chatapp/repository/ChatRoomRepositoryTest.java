package com.example.chatapp.repository;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomType;
import com.example.chatapp.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private TestEntityManager entityManager;

    private ChatRoom savedRoom;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 및 저장
        User creator = User.create("testuser", "encoded_password");
        entityManager.persistAndFlush(creator);
        
        // 채팅방 생성 및 저장
        ChatRoom room = ChatRoom.create(
                "Test Room",
                ChatRoomType.GROUP,
                creator
        );

        savedRoom = chatRoomRepository.save(room);
        entityManager.flush(); // 데이터베이스에 즉시 반영
    }

    @Test
    public void testFindById() {
        // given
        Long roomId = savedRoom.getId();

        // when
        Optional<ChatRoom> foundRoom = chatRoomRepository.findById(roomId);

        // then
        assertThat(foundRoom).isPresent();
        assertThat(foundRoom.get().getName().getValue()).isEqualTo("Test Room");
    }
}

package com.example.chatapp.controller;

import com.example.chatapp.domain.ChatRoomType;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.infrastructure.filter.SessionAuthenticationFilter;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.example.chatapp.service.ChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private AuthContext authContext; // 이 부분을 추가

    @MockitoBean
    private SessionStore sessionStore;

    @Test
    @DisplayName("전체 채팅방 조회 성공")
    void testGetAllChatRooms() throws Exception {
        // Given
        List<ChatRoomSimpleResponse> chatRoomSimpleResponses = List.of(
                new ChatRoomSimpleResponse(1L, "testRoom1", ChatRoomType.GROUP),
                new ChatRoomSimpleResponse(2L, "testRoom2", ChatRoomType.PRIVATE)
        );
        when(chatRoomService.findAllChatRoomsSimple()).thenReturn(chatRoomSimpleResponses);

        // When
        ResultActions perform = mockMvc.perform(get("/api/rooms"));

        // Then
        perform.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("testRoom1"))
                .andExpect(jsonPath("$[0].type").value(ChatRoomType.GROUP.toString()))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("testRoom2"))
                .andExpect(jsonPath("$[1].type").value(ChatRoomType.PRIVATE.toString()));
    }
}

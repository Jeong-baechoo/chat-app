package com.example.chatapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomEnterRequest {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    Long roomId;

}

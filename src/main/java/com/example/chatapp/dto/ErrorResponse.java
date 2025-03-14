package com.example.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
}

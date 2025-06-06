package com.example.chatapp.infrastructure.message;

public enum ChatEventType {
    MESSAGE_SENT,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    USER_JOINED,
    USER_LEFT,
    ROOM_CREATED,
    ROOM_UPDATED,
    ROOM_DELETED,
    TYPING_START,
    TYPING_STOP,
    USER_STATUS_CHANGED
}

# Frontend Integration Example

## React + TypeScript 예제 코드

### 1. API 클라이언트 설정
```typescript
// src/api/client.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // 쿠키 자동 전송
  headers: {
    'Content-Type': 'application/json',
  },
});

// 에러 인터셉터
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 로그인 페이지로 리다이렉트
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### 2. 인증 서비스
```typescript
// src/services/authService.ts
import { apiClient } from '../api/client';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  expiresAt: string;
}

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', data);
    return response.data;
  },

  async signup(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/signup', data);
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout');
  },

  async getCurrentUser() {
    const response = await apiClient.get('/auth/me');
    return response.data;
  }
};
```

### 3. WebSocket 연결
```typescript
// src/services/websocketService.ts
import SockJS from 'sockjs-client';
import { Client, Message } from '@stomp/stompjs';

export class WebSocketService {
  private client: Client;
  private roomSubscriptions: Map<number, any> = new Map();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('WebSocket Connected');
    };

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client.onConnect = () => {
        console.log('WebSocket Connected');
        resolve();
      };
      this.client.onStompError = (error) => {
        reject(error);
      };
      this.client.activate();
    });
  }

  disconnect(): void {
    this.client.deactivate();
  }

  enterRoom(roomId: number): void {
    this.client.publish({
      destination: '/app/room.enter',
      body: JSON.stringify({ roomId }),
    });
  }

  subscribeToRoom(roomId: number, callback: (message: any) => void): void {
    const subscription = this.client.subscribe(
      `/topic/room.${roomId}`,
      (message: Message) => {
        callback(JSON.parse(message.body));
      }
    );
    this.roomSubscriptions.set(roomId, subscription);
  }

  unsubscribeFromRoom(roomId: number): void {
    const subscription = this.roomSubscriptions.get(roomId);
    if (subscription) {
      subscription.unsubscribe();
      this.roomSubscriptions.delete(roomId);
    }
  }

  sendMessage(chatRoomId: number, content: string): void {
    this.client.publish({
      destination: '/app/message.send',
      body: JSON.stringify({ chatRoomId, content }),
    });
  }

  subscribeToErrors(callback: (error: any) => void): void {
    this.client.subscribe('/user/queue/errors', (message: Message) => {
      callback(JSON.parse(message.body));
    });
  }
}
```

### 4. React Hook 예제
```typescript
// src/hooks/useChat.ts
import { useState, useEffect, useCallback } from 'react';
import { WebSocketService } from '../services/websocketService';
import { messageService } from '../services/messageService';

export const useChat = (roomId: number) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [wsService] = useState(() => new WebSocketService());

  useEffect(() => {
    // 이전 메시지 로드
    const loadMessages = async () => {
      const recentMessages = await messageService.getRecentMessages(roomId);
      setMessages(recentMessages);
    };

    // WebSocket 연결
    const connectWebSocket = async () => {
      try {
        await wsService.connect();
        setIsConnected(true);
        
        // 채팅방 입장
        wsService.enterRoom(roomId);
        
        // 메시지 구독
        wsService.subscribeToRoom(roomId, (message) => {
          setMessages((prev) => [...prev, message]);
        });
        
        // 에러 구독
        wsService.subscribeToErrors((error) => {
          console.error('WebSocket error:', error);
        });
      } catch (error) {
        console.error('Failed to connect WebSocket:', error);
      }
    };

    loadMessages();
    connectWebSocket();

    // Cleanup
    return () => {
      wsService.unsubscribeFromRoom(roomId);
      wsService.disconnect();
    };
  }, [roomId]);

  const sendMessage = useCallback((content: string) => {
    if (isConnected) {
      wsService.sendMessage(roomId, content);
    }
  }, [isConnected, roomId, wsService]);

  return {
    messages,
    sendMessage,
    isConnected,
  };
};
```

### 5. React 컴포넌트 예제
```tsx
// src/components/ChatRoom.tsx
import React, { useState } from 'react';
import { useChat } from '../hooks/useChat';

interface ChatRoomProps {
  roomId: number;
}

export const ChatRoom: React.FC<ChatRoomProps> = ({ roomId }) => {
  const { messages, sendMessage, isConnected } = useChat(roomId);
  const [inputMessage, setInputMessage] = useState('');

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputMessage.trim()) {
      sendMessage(inputMessage);
      setInputMessage('');
    }
  };

  return (
    <div className="chat-room">
      <div className="connection-status">
        {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
      </div>
      
      <div className="messages">
        {messages.map((msg) => (
          <div key={msg.id} className="message">
            <strong>{msg.sender.username}:</strong> {msg.content}
            <span className="timestamp">
              {new Date(msg.timestamp).toLocaleTimeString()}
            </span>
          </div>
        ))}
      </div>
      
      <form onSubmit={handleSendMessage} className="message-input">
        <input
          type="text"
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          placeholder="Type a message..."
          disabled={!isConnected}
        />
        <button type="submit" disabled={!isConnected}>
          Send
        </button>
      </form>
    </div>
  );
};
```

### 6. 타입 정의
```typescript
// src/types/index.ts
export interface User {
  id: number;
  username: string;
}

export interface Message {
  id: number;
  content: string;
  sender: User;
  chatRoomId: number;
  status: 'SENT' | 'DELIVERED' | 'READ' | 'DELETED';
  timestamp: string;
}

export interface ChatRoom {
  id: number;
  name: string;
  type: 'PRIVATE' | 'GROUP';
  participants: Participant[];
  createdAt: string;
}

export interface Participant {
  userId: number;
  username: string;
  role: 'ADMIN' | 'MEMBER';
  joinedAt: string;
}
```

## 설치 방법
```bash
# 필요한 패키지 설치
npm install axios sockjs-client @stomp/stompjs
npm install -D @types/sockjs-client

# 또는 yarn 사용
yarn add axios sockjs-client @stomp/stompjs
yarn add -D @types/sockjs-client
```

## 주의사항
1. CORS 설정으로 인해 프론트엔드는 `http://localhost:3000`에서 실행해야 함
2. WebSocket 연결 시 쿠키 인증이 자동으로 처리됨
3. 메시지 전송 전 반드시 채팅방에 입장해야 함 (`room.enter`)
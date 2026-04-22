package com.gamingbar.service;

import com.gamingbar.vo.message.MessageVo;
import org.springframework.web.socket.WebSocketSession;

public interface RoomRealtimeService {

    void register(WebSocketSession session, Long roomId, Long userId);

    void unregister(WebSocketSession session);

    void touch(WebSocketSession session);

    void sendError(WebSocketSession session, Long roomId, String text);

    void broadcastChatMessage(MessageVo message);

    void closeRoomSessions(Long roomId, String text);

    void disconnectUserSessions(Long roomId, Long userId, String text);

    void disconnectAllUserSessions(Long userId, String text);
}

package com.gamingbar.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamingbar.dto.message.SendMessageRequest;
import com.gamingbar.service.MessageService;
import com.gamingbar.service.RoomRealtimeService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private final RoomRealtimeService roomRealtimeService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public RoomWebSocketHandler(RoomRealtimeService roomRealtimeService,
                                MessageService messageService,
                                ObjectMapper objectMapper) {
        this.roomRealtimeService = roomRealtimeService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        roomRealtimeService.register(
            session,
            (Long) session.getAttributes().get("roomId"),
            (Long) session.getAttributes().get("userId")
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        RoomSocketInboundMessage inbound = objectMapper.readValue(message.getPayload(), RoomSocketInboundMessage.class);
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if ("ping".equalsIgnoreCase(inbound.getType())) {
            roomRealtimeService.touch(session);
            return;
        }
        if (!"chat".equalsIgnoreCase(inbound.getType())) {
            roomRealtimeService.sendError(session, roomId, "Unsupported message type");
            return;
        }

        try {
            SendMessageRequest request = new SendMessageRequest();
            request.setContent(inbound.getContent());
            messageService.sendMessage(userId, roomId, request);
            roomRealtimeService.touch(session);
        } catch (Exception exception) {
            roomRealtimeService.sendError(session, roomId, exception.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        roomRealtimeService.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomRealtimeService.unregister(session);
    }
}

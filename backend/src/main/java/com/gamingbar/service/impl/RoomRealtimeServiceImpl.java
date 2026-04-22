package com.gamingbar.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.RoomCacheService;
import com.gamingbar.service.RoomRealtimeService;
import com.gamingbar.vo.message.MessageVo;
import com.gamingbar.websocket.RoomSocketEvent;
import com.gamingbar.websocket.RoomSocketUserVo;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class RoomRealtimeServiceImpl implements RoomRealtimeService {

    private final Map<Long, Map<String, SessionContext>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, SessionContext> sessionIndex = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final RoomCacheService roomCacheService;
    private final long heartbeatTimeoutMs;

    public RoomRealtimeServiceImpl(ObjectMapper objectMapper,
                                   UserMapper userMapper,
                                   RoomCacheService roomCacheService,
                                   @Value("${app.websocket.heartbeat-timeout-ms}") long heartbeatTimeoutMs) {
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.roomCacheService = roomCacheService;
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
    }

    @Override
    public void register(WebSocketSession session, Long roomId, Long userId) {
        User user = userMapper.selectById(userId);
        RoomSocketUserVo socketUser = user == null
            ? new RoomSocketUserVo(userId, "", "")
            : new RoomSocketUserVo(user.getId(), user.getNickname(), user.getAvatar());

        Map<String, SessionContext> sessions = roomSessions.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>());
        boolean userAlreadyOnline = isUserOnline(sessions, userId);
        SessionContext context = new SessionContext(session, roomId, userId, socketUser);
        sessions.put(session.getId(), context);
        sessionIndex.put(session.getId(), context);

        int onlineCount = uniqueUserCount(sessions);
        roomCacheService.setOnlineCount(roomId, onlineCount);
        send(session, new RoomSocketEvent("connected", roomId, onlineCount, null, socketUser, null));
        if (!userAlreadyOnline) {
            broadcast(roomId, new RoomSocketEvent("member_online", roomId, onlineCount, null, socketUser, null));
        }
    }

    @Override
    public void unregister(WebSocketSession session) {
        SessionContext context = sessionIndex.remove(session.getId());
        if (context == null) {
            return;
        }

        Map<String, SessionContext> sessions = roomSessions.get(context.roomId());
        if (sessions == null) {
            return;
        }
        sessions.remove(session.getId());
        boolean stillOnline = isUserOnline(sessions, context.userId());
        int onlineCount = uniqueUserCount(sessions);
        roomCacheService.setOnlineCount(context.roomId(), onlineCount);
        if (!stillOnline) {
            broadcast(context.roomId(), new RoomSocketEvent("member_offline", context.roomId(), onlineCount, null, context.user(), null));
        }
        if (sessions.isEmpty()) {
            roomSessions.remove(context.roomId());
        }
    }

    @Override
    public void touch(WebSocketSession session) {
        SessionContext context = sessionIndex.get(session.getId());
        if (context == null) {
            return;
        }
        context.touch();
        send(session, new RoomSocketEvent("pong", context.roomId(), roomCacheService.getOnlineCount(context.roomId()), null, null, null));
    }

    @Override
    public void sendError(WebSocketSession session, Long roomId, String text) {
        send(session, new RoomSocketEvent("error", roomId, roomCacheService.getOnlineCount(roomId), null, null, text));
    }

    @Override
    public void broadcastChatMessage(MessageVo message) {
        broadcast(message.getRoomId(), new RoomSocketEvent("chat_message", message.getRoomId(), roomCacheService.getOnlineCount(message.getRoomId()), message, null, null));
    }

    @Override
    public void closeRoomSessions(Long roomId, String text) {
        Map<String, SessionContext> sessions = roomSessions.remove(roomId);
        if (sessions == null) {
            roomCacheService.setOnlineCount(roomId, 0);
            return;
        }
        for (SessionContext context : sessions.values()) {
            sessionIndex.remove(context.session().getId());
            send(context.session(), new RoomSocketEvent("room_closed", roomId, 0, null, null, text));
            close(context.session(), CloseStatus.NORMAL);
        }
        roomCacheService.setOnlineCount(roomId, 0);
    }

    @Override
    public void disconnectUserSessions(Long roomId, Long userId, String text) {
        Map<String, SessionContext> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }

        Set<String> targetSessionIds = sessions.entrySet().stream()
            .filter(entry -> entry.getValue().userId().equals(userId))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());

        for (String sessionId : targetSessionIds) {
            SessionContext context = sessions.get(sessionId);
            if (context == null) {
                continue;
            }
            send(context.session(), new RoomSocketEvent(text, roomId, roomCacheService.getOnlineCount(roomId), null, null, text));
            close(context.session(), CloseStatus.NORMAL);
            unregister(context.session());
        }
    }

    @Override
    public void disconnectAllUserSessions(Long userId, String text) {
        Set<String> targetSessionIds = sessionIndex.values().stream()
            .filter(context -> context.userId().equals(userId))
            .map(context -> context.session().getId())
            .collect(java.util.stream.Collectors.toSet());

        for (String sessionId : targetSessionIds) {
            SessionContext context = sessionIndex.get(sessionId);
            if (context == null) {
                continue;
            }
            send(context.session(), new RoomSocketEvent(text, context.roomId(), roomCacheService.getOnlineCount(context.roomId()), null, null, text));
            close(context.session(), CloseStatus.NORMAL);
            unregister(context.session());
        }
    }

    @Scheduled(fixedDelayString = "${app.websocket.heartbeat-check-ms}")
    public void closeExpiredSessions() {
        Instant deadline = Instant.now().minusMillis(heartbeatTimeoutMs);
        for (SessionContext context : sessionIndex.values()) {
            if (context.lastHeartbeat().isBefore(deadline)) {
                log.info("Closing stale websocket session={}, roomId={}", context.session().getId(), context.roomId());
                close(context.session(), CloseStatus.SESSION_NOT_RELIABLE);
                unregister(context.session());
            }
        }
    }

    private void broadcast(Long roomId, RoomSocketEvent event) {
        Map<String, SessionContext> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        for (SessionContext context : sessions.values()) {
            send(context.session(), event);
        }
    }

    private void send(WebSocketSession session, RoomSocketEvent event) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(toJson(event)));
            }
        } catch (IOException exception) {
            close(session, CloseStatus.SERVER_ERROR);
            unregister(session);
        }
    }

    private String toJson(RoomSocketEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize websocket event", exception);
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isUserOnline(Map<String, SessionContext> sessions, Long userId) {
        return sessions.values().stream().anyMatch(context -> context.userId().equals(userId));
    }

    private int uniqueUserCount(Map<String, SessionContext> sessions) {
        return (int) sessions.values().stream().map(SessionContext::userId).distinct().count();
    }

    private static final class SessionContext {

        private final WebSocketSession session;
        private final Long roomId;
        private final Long userId;
        private final RoomSocketUserVo user;
        private volatile Instant lastHeartbeat;

        private SessionContext(WebSocketSession session, Long roomId, Long userId, RoomSocketUserVo user) {
            this.session = session;
            this.roomId = roomId;
            this.userId = userId;
            this.user = user;
            this.lastHeartbeat = Instant.now();
        }

        private void touch() {
            this.lastHeartbeat = Instant.now();
        }

        private WebSocketSession session() {
            return session;
        }

        private Long roomId() {
            return roomId;
        }

        private Long userId() {
            return userId;
        }

        private RoomSocketUserVo user() {
            return user;
        }

        private Instant lastHeartbeat() {
            return lastHeartbeat;
        }
    }
}

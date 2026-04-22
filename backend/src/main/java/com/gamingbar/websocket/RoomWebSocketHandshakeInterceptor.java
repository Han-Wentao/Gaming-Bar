package com.gamingbar.websocket;

import com.gamingbar.cache.CacheService;
import com.gamingbar.common.constant.AppConstants;
import com.gamingbar.entity.Room;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.service.RoomCleanupService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RoomWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final CacheService cacheService;
    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final RoomCleanupService roomCleanupService;

    public RoomWebSocketHandshakeInterceptor(CacheService cacheService,
                                             RoomMapper roomMapper,
                                             RoomUserMapper roomUserMapper,
                                             RoomCleanupService roomCleanupService) {
        this.cacheService = cacheService;
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.roomCleanupService = roomCleanupService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            Long roomId = parseRoomId(request.getURI().getPath());
            String ticket = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
            String ticketValue = ticket == null ? null : cacheService.getAndDelete(AppConstants.WS_TICKET_PREFIX + ticket);
            if (ticket == null || ticket.isBlank() || ticketValue == null || ticketValue.isBlank()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String[] parts = ticketValue.split(":");
            if (parts.length != 3) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            Long userId = Long.parseLong(parts[0]);
            Long ticketRoomId = Long.parseLong(parts[1]);
            String sessionVersion = parts[2];
            if (!roomId.equals(ticketRoomId)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            String activeSessionVersion = cacheService.get(AppConstants.SESSION_VERSION_PREFIX + userId);
            if (activeSessionVersion == null || !activeSessionVersion.equals(sessionVersion)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Room room = roomMapper.selectById(roomId);
            if (room == null || "closed".equals(room.getStatus()) || roomCleanupService.isExpired(room)) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }
            if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) == null) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put("roomId", roomId);
            attributes.put("userId", userId);
            return true;
        } catch (Exception exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private Long parseRoomId(String path) {
        String[] parts = path.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}

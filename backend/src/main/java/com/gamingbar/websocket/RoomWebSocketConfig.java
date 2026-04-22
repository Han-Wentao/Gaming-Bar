package com.gamingbar.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
public class RoomWebSocketConfig implements WebSocketConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;
    private final RoomWebSocketHandshakeInterceptor handshakeInterceptor;
    private final String[] allowedOrigins;

    public RoomWebSocketConfig(RoomWebSocketHandler roomWebSocketHandler,
                               RoomWebSocketHandshakeInterceptor handshakeInterceptor,
                               @Value("${app.websocket.allowed-origin-patterns:*}") String allowedOriginPatterns) {
        this.roomWebSocketHandler = roomWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.allowedOrigins = allowedOriginPatterns.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomWebSocketHandler, "/ws/rooms/*")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOriginPatterns(allowedOrigins);
    }
}

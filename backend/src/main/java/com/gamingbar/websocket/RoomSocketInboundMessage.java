package com.gamingbar.websocket;

import lombok.Data;

@Data
public class RoomSocketInboundMessage {

    private String type;
    private String content;
}

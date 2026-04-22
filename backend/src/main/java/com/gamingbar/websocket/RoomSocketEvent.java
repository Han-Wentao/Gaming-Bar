package com.gamingbar.websocket;

import com.gamingbar.vo.message.MessageVo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomSocketEvent {

    private String type;
    private Long roomId;
    private Integer onlineCount;
    private MessageVo message;
    private RoomSocketUserVo user;
    private String text;
}

package com.gamingbar.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomSocketUserVo {

    private Long userId;
    private String nickname;
    private String avatar;
}

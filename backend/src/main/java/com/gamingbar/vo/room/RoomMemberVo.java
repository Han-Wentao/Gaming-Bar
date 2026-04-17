package com.gamingbar.vo.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomMemberVo {

    private Long userId;
    private String nickname;
    private String avatar;
    private String joinTime;
}

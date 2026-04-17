package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RoomUser {

    private Long id;
    private Long roomId;
    private Long userId;
    private LocalDateTime joinTime;
}

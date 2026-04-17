package com.gamingbar.dto.room;

import lombok.Data;

@Data
public class CreateRoomRequest {

    private Integer gameId;
    private Integer maxPlayer;
    private String type;
    private String startTime;
}

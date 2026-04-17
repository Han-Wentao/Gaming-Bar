package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Room {

    private Long id;
    private Integer gameId;
    private Long ownerId;
    private Integer maxPlayer;
    private Integer currentPlayer;
    private String type;
    private LocalDateTime startTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Message {

    private Long id;
    private Long roomId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
}

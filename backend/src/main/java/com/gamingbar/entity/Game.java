package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Game {

    private Integer id;
    private String gameName;
    private String status;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

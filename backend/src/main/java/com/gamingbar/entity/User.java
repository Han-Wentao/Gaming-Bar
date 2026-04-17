package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer creditScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

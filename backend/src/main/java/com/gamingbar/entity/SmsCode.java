package com.gamingbar.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SmsCode {

    private Long id;
    private String phone;
    private String smsCode;
    private LocalDateTime expiredAt;
    private Integer usedStatus;
    private LocalDateTime usedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

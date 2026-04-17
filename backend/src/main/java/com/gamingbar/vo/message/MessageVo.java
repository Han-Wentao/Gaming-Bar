package com.gamingbar.vo.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageVo {

    private Long id;
    private Long roomId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private String createTime;
}

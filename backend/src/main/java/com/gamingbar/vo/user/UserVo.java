package com.gamingbar.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserVo {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer creditScore;
}

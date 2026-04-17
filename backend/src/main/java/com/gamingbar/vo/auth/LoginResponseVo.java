package com.gamingbar.vo.auth;

import com.gamingbar.vo.user.UserVo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseVo {

    private String token;
    private Long expiresIn;
    private UserVo user;
}

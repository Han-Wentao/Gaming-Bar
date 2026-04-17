package com.gamingbar.service;

import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.user.UserVo;

public interface AuthService {

    SendSmsResponseVo sendSms(SendSmsRequest request);

    LoginResponseVo login(LoginRequest request);

    UserVo getCurrentUser(Long userId);
}

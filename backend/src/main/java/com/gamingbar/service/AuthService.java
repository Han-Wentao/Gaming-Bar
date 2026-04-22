package com.gamingbar.service;

import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.LogoutRequest;
import com.gamingbar.dto.auth.RefreshTokenRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.auth.WsTicketResponseVo;
import com.gamingbar.vo.user.UserVo;

public interface AuthService {

    SendSmsResponseVo sendSms(SendSmsRequest request, String clientKey);

    LoginResponseVo login(LoginRequest request, String clientKey);

    UserVo getCurrentUser(Long userId);

    void logout(String token, LogoutRequest request);

    LoginResponseVo refreshToken(RefreshTokenRequest request);

    WsTicketResponseVo createWsTicket(Long userId, Long roomId);
}

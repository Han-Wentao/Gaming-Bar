package com.gamingbar.controller.auth;

import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.service.AuthService;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.user.UserVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sms/send")
    public ApiResponse<SendSmsResponseVo> sendSms(@RequestBody SendSmsRequest request) {
        return ApiResponse.success(authService.sendSms(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseVo> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserVo> me() {
        return ApiResponse.success(authService.getCurrentUser(UserContext.getUserId()));
    }
}

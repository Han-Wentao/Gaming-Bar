package com.gamingbar.controller.auth;

import com.gamingbar.common.constant.AppConstants;
import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.LogoutRequest;
import com.gamingbar.dto.auth.RefreshTokenRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.service.AuthService;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.auth.WsTicketResponseVo;
import com.gamingbar.vo.user.UserVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean trustForwardedClientIp;

    public AuthController(AuthService authService,
                          @Value("${app.security.trust-forwarded-client-ip:false}") boolean trustForwardedClientIp) {
        this.authService = authService;
        this.trustForwardedClientIp = trustForwardedClientIp;
    }

    @PostMapping("/sms/send")
    public ApiResponse<SendSmsResponseVo> sendSms(@Valid @RequestBody SendSmsRequest request,
                                                  HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.sendSms(request, resolveClientKey(servletRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseVo> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, resolveClientKey(servletRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                    HttpServletRequest servletRequest) {
        authService.logout(resolveToken(servletRequest), request);
        return ApiResponse.success();
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponseVo> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }

    @PostMapping("/ws-ticket")
    public ApiResponse<WsTicketResponseVo> createWsTicket(@RequestParam Long roomId) {
        return ApiResponse.success(authService.createWsTicket(UserContext.getUserId(), roomId));
    }

    @GetMapping("/me")
    public ApiResponse<UserVo> me() {
        return ApiResponse.success(authService.getCurrentUser(UserContext.getUserId()));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTH_HEADER);
        if (header == null || !header.startsWith(AppConstants.AUTH_PREFIX)) {
            return "";
        }
        return header.substring(AppConstants.AUTH_PREFIX.length());
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (!trustForwardedClientIp) {
            return request.getRemoteAddr();
        }
        String forwardedFor = request.getHeader(AppConstants.CLIENT_IP_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

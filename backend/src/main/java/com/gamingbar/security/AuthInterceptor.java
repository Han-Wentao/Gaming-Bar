package com.gamingbar.security;

import com.gamingbar.common.constant.AppConstants;
import com.gamingbar.cache.CacheService;
import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.enums.ErrorCode;
import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.util.JwtUtils;
import com.gamingbar.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final CacheService cacheService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthInterceptor(JwtUtils jwtUtils,
                           CacheService cacheService,
                           TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.cacheService = cacheService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AppConstants.AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(AppConstants.AUTH_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(AppConstants.AUTH_PREFIX.length());
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态已失效，请重新登录");
        }

        try {
            Long userId = jwtUtils.parseUserId(token);
            String sessionVersion = jwtUtils.parseSessionVersion(token);
            String activeSessionVersion = cacheService.get(AppConstants.SESSION_VERSION_PREFIX + userId);
            if (userId == null || sessionVersion == null || !sessionVersion.equals(activeSessionVersion)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态已失效，请重新登录");
            }
            UserContext.setUserId(userId);
            return true;
        } catch (Exception exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

package com.gamingbar.security;

import com.gamingbar.common.constant.AppConstants;
import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public AuthInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AppConstants.AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(AppConstants.AUTH_PREFIX)) {
            throw new BusinessException(401, "未登录或 token 无效");
        }

        String token = authHeader.substring(AppConstants.AUTH_PREFIX.length());
        try {
            UserContext.setUserId(jwtUtils.parseUserId(token));
            return true;
        } catch (Exception exception) {
            throw new BusinessException(401, "未登录或 token 无效");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

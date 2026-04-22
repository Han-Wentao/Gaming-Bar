package com.gamingbar.service.impl;

import com.gamingbar.cache.CacheService;
import com.gamingbar.common.constant.AppConstants;
import com.gamingbar.common.enums.ErrorCode;
import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.util.JwtUtils;
import com.gamingbar.common.util.TimeUtils;
import com.gamingbar.common.util.ValidationUtils;
import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.LogoutRequest;
import com.gamingbar.dto.auth.RefreshTokenRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.entity.Room;
import com.gamingbar.entity.SmsCode;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.mapper.SmsCodeMapper;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.AuthService;
import com.gamingbar.service.RoomRealtimeService;
import com.gamingbar.service.TokenBlacklistService;
import com.gamingbar.service.sms.SmsService;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.auth.WsTicketResponseVo;
import com.gamingbar.vo.user.UserVo;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String SMS_CODE_PREFIX = "auth:sms:code:";
    private static final String SMS_COOLDOWN_PREFIX = "auth:sms:cooldown:";
    private static final String SMS_RATE_PREFIX = "auth:sms:rate:";
    private static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_PREFIX = "auth:refresh:user:";

    private final SmsCodeMapper smsCodeMapper;
    private final UserMapper userMapper;
    private final RoomMapper roomMapper;
    private final RoomUserMapper roomUserMapper;
    private final JwtUtils jwtUtils;
    private final CacheService cacheService;
    private final SmsService smsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RoomRealtimeService roomRealtimeService;
    private final int smsExpireSeconds;
    private final int resendIntervalSeconds;
    private final int smsSendLimitWindowSeconds;
    private final int smsSendLimitMaxTimes;
    private final int loginFailWindowSeconds;
    private final int loginFailMaxTimes;
    private final long refreshExpireSeconds;
    private final long wsTicketExpireSeconds;

    public AuthServiceImpl(SmsCodeMapper smsCodeMapper,
                           UserMapper userMapper,
                           RoomMapper roomMapper,
                           RoomUserMapper roomUserMapper,
                           JwtUtils jwtUtils,
                           CacheService cacheService,
                           SmsService smsService,
                           TokenBlacklistService tokenBlacklistService,
                           RoomRealtimeService roomRealtimeService,
                           @Value("${app.sms.expire-seconds}") int smsExpireSeconds,
                           @Value("${app.sms.resend-interval-seconds}") int resendIntervalSeconds,
                           @Value("${app.sms.send-limit-window-seconds}") int smsSendLimitWindowSeconds,
                           @Value("${app.sms.send-limit-max-times}") int smsSendLimitMaxTimes,
                           @Value("${app.security.login-fail-window-seconds}") int loginFailWindowSeconds,
                           @Value("${app.security.login-fail-max-times}") int loginFailMaxTimes,
                           @Value("${app.jwt.refresh-expire-seconds}") long refreshExpireSeconds,
                           @Value("${app.websocket.ticket-expire-seconds:30}") long wsTicketExpireSeconds) {
        this.smsCodeMapper = smsCodeMapper;
        this.userMapper = userMapper;
        this.roomMapper = roomMapper;
        this.roomUserMapper = roomUserMapper;
        this.jwtUtils = jwtUtils;
        this.cacheService = cacheService;
        this.smsService = smsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.roomRealtimeService = roomRealtimeService;
        this.smsExpireSeconds = smsExpireSeconds;
        this.resendIntervalSeconds = resendIntervalSeconds;
        this.smsSendLimitWindowSeconds = smsSendLimitWindowSeconds;
        this.smsSendLimitMaxTimes = smsSendLimitMaxTimes;
        this.loginFailWindowSeconds = loginFailWindowSeconds;
        this.loginFailMaxTimes = loginFailMaxTimes;
        this.refreshExpireSeconds = refreshExpireSeconds;
        this.wsTicketExpireSeconds = wsTicketExpireSeconds;
    }

    @Override
    public SendSmsResponseVo sendSms(SendSmsRequest request, String clientKey) {
        ValidationUtils.phone(request.getPhone());

        String phoneKey = SMS_CODE_PREFIX + request.getPhone();
        String cooldownKey = SMS_COOLDOWN_PREFIX + request.getPhone();
        if (cacheService.get(cooldownKey) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Verification code requests are too frequent");
        }

        String rateKey = SMS_RATE_PREFIX + request.getPhone() + ":" + sanitizeClientKey(clientKey);
        long count = cacheService.increment(rateKey, Duration.ofSeconds(smsSendLimitWindowSeconds));
        if (count > smsSendLimitMaxTimes) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "SMS send limit exceeded");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        LocalDateTime now = TimeUtils.now();
        cacheService.set(phoneKey, code, Duration.ofSeconds(smsExpireSeconds));
        cacheService.set(cooldownKey, "1", Duration.ofSeconds(resendIntervalSeconds));
        smsCodeMapper.markUnusedAsUsed(request.getPhone(), now);

        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(request.getPhone());
        smsCode.setSmsCode(code);
        smsCode.setExpiredAt(now.plusSeconds(smsExpireSeconds));
        smsCode.setUsedStatus(0);
        smsCodeMapper.insert(smsCode);
        smsService.sendCode(request.getPhone(), code);
        log.info("SMS code issued for phone={}", request.getPhone());
        return new SendSmsResponseVo(smsExpireSeconds);
    }

    @Override
    @Transactional
    public LoginResponseVo login(LoginRequest request, String clientKey) {
        ValidationUtils.phone(request.getPhone());
        ValidationUtils.smsCode(request.getCode());

        String failKey = LOGIN_FAIL_PREFIX + request.getPhone() + ":" + sanitizeClientKey(clientKey);
        String failCount = cacheService.get(failKey);
        if (failCount != null && Long.parseLong(failCount) >= loginFailMaxTimes) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "Too many failed login attempts");
        }

        LocalDateTime now = TimeUtils.now();
        String cachedCode = cacheService.get(SMS_CODE_PREFIX + request.getPhone());
        SmsCode latestSmsCode = smsCodeMapper.selectLatestUnusedByPhone(request.getPhone());
        boolean cacheMatched = cachedCode != null && cachedCode.equals(request.getCode());
        boolean databaseMatched = latestSmsCode != null
            && request.getCode().equals(latestSmsCode.getSmsCode())
            && latestSmsCode.getExpiredAt() != null
            && latestSmsCode.getExpiredAt().isAfter(now);
        if (!cacheMatched && !databaseMatched) {
            cacheService.increment(failKey, Duration.ofSeconds(loginFailWindowSeconds));
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification code is invalid");
        }

        if (latestSmsCode == null || smsCodeMapper.markUsed(latestSmsCode.getId(), now) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Verification code has already been used");
        }

        cacheService.delete(SMS_CODE_PREFIX + request.getPhone());
        cacheService.delete(SMS_COOLDOWN_PREFIX + request.getPhone());
        cacheService.delete(failKey);

        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            user = new User();
            user.setPhone(request.getPhone());
            user.setNickname("Player0000");
            user.setAvatar("");
            user.setCreditScore(100);
            userMapper.insert(user);

            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setNickname(defaultNickname(user.getId()));
            userMapper.updateProfile(updateUser);
            user = userMapper.selectById(user.getId());
        }

        log.info("User login succeeded, userId={}", user.getId());
        return buildLoginResponse(user);
    }

    @Override
    public UserVo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return toUserVo(user);
    }

    @Override
    public void logout(String token, LogoutRequest request) {
        if (token == null || token.isBlank()) {
            return;
        }

        tokenBlacklistService.blacklist(token);
        try {
            Long userId = jwtUtils.parseUserId(token);
            rotateSessionVersion(userId);
            deleteCurrentRefreshToken(userId);
            roomRealtimeService.disconnectAllUserSessions(userId, "logged_out");
        } catch (Exception ignored) {
        }

        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            deleteRefreshToken(request.getRefreshToken());
        }
    }

    @Override
    public LoginResponseVo refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String userIdValue = cacheService.get(REFRESH_TOKEN_PREFIX + refreshToken);
        if (userIdValue == null || userIdValue.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired");
        }

        Long userId = Long.parseLong(userIdValue);
        User user = userMapper.selectById(userId);
        if (user == null) {
            deleteRefreshToken(refreshToken);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        deleteRefreshToken(refreshToken);
        return buildLoginResponse(user);
    }

    @Override
    public WsTicketResponseVo createWsTicket(Long userId, Long roomId) {
        ValidationUtils.positive(roomId);
        Room room = roomMapper.selectById(roomId);
        if (room == null || "closed".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (roomUserMapper.selectByRoomIdAndUserId(roomId, userId) == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You are not in this room");
        }

        String sessionVersion = getActiveSessionVersion(userId);
        if (sessionVersion == null || sessionVersion.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Session is invalid");
        }

        String ticket = UUID.randomUUID().toString().replace("-", "");
        cacheService.set(
            AppConstants.WS_TICKET_PREFIX + ticket,
            userId + ":" + roomId + ":" + sessionVersion,
            Duration.ofSeconds(wsTicketExpireSeconds)
        );
        return new WsTicketResponseVo(ticket, wsTicketExpireSeconds);
    }

    private LoginResponseVo buildLoginResponse(User user) {
        String sessionVersion = rotateSessionVersion(user.getId());
        String accessToken = jwtUtils.generateToken(user.getId(), sessionVersion);
        String refreshToken = issueRefreshToken(user.getId());
        return new LoginResponseVo(
            accessToken,
            refreshToken,
            jwtUtils.getExpireSeconds(),
            toUserVo(user)
        );
    }

    private String issueRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String userRefreshKey = USER_REFRESH_PREFIX + userId;
        String previousRefreshToken = cacheService.get(userRefreshKey);
        if (previousRefreshToken != null && !previousRefreshToken.isBlank()) {
            cacheService.delete(REFRESH_TOKEN_PREFIX + previousRefreshToken);
        }
        Duration ttl = Duration.ofSeconds(refreshExpireSeconds);
        cacheService.set(REFRESH_TOKEN_PREFIX + refreshToken, String.valueOf(userId), ttl);
        cacheService.set(userRefreshKey, refreshToken, ttl);
        return refreshToken;
    }

    private void deleteRefreshToken(String refreshToken) {
        String userIdValue = cacheService.get(REFRESH_TOKEN_PREFIX + refreshToken);
        cacheService.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        if (userIdValue != null && !userIdValue.isBlank()) {
            String userRefreshKey = USER_REFRESH_PREFIX + userIdValue;
            String currentToken = cacheService.get(userRefreshKey);
            if (Objects.equals(currentToken, refreshToken)) {
                cacheService.delete(userRefreshKey);
            }
        }
    }

    private void deleteCurrentRefreshToken(Long userId) {
        String userRefreshKey = USER_REFRESH_PREFIX + userId;
        String refreshToken = cacheService.get(userRefreshKey);
        if (refreshToken != null && !refreshToken.isBlank()) {
            cacheService.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        }
        cacheService.delete(userRefreshKey);
    }

    private String rotateSessionVersion(Long userId) {
        String sessionVersion = UUID.randomUUID().toString().replace("-", "");
        cacheService.set(
            AppConstants.SESSION_VERSION_PREFIX + userId,
            sessionVersion,
            Duration.ofSeconds(refreshExpireSeconds)
        );
        return sessionVersion;
    }

    private String getActiveSessionVersion(Long userId) {
        return cacheService.get(AppConstants.SESSION_VERSION_PREFIX + userId);
    }

    private String sanitizeClientKey(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return "unknown";
        }
        return clientKey.trim();
    }

    private String defaultNickname(Long userId) {
        String raw = String.valueOf(userId % 10000);
        return "Player" + String.format("%04d", Integer.parseInt(raw));
    }

    private UserVo toUserVo(User user) {
        return new UserVo(
            user.getId(),
            user.getPhone(),
            user.getNickname(),
            user.getAvatar(),
            user.getCreditScore()
        );
    }
}

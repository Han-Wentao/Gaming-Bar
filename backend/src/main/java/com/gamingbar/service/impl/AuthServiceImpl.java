package com.gamingbar.service.impl;

import com.gamingbar.common.exception.BusinessException;
import com.gamingbar.common.util.JwtUtils;
import com.gamingbar.common.util.TimeUtils;
import com.gamingbar.common.util.ValidationUtils;
import com.gamingbar.dto.auth.LoginRequest;
import com.gamingbar.dto.auth.SendSmsRequest;
import com.gamingbar.entity.SmsCode;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.SmsCodeMapper;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.AuthService;
import com.gamingbar.vo.auth.LoginResponseVo;
import com.gamingbar.vo.auth.SendSmsResponseVo;
import com.gamingbar.vo.user.UserVo;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final SmsCodeMapper smsCodeMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final int smsExpireSeconds;
    private final int resendIntervalSeconds;

    public AuthServiceImpl(SmsCodeMapper smsCodeMapper,
                           UserMapper userMapper,
                           JwtUtils jwtUtils,
                           @Value("${app.sms.expire-seconds}") int smsExpireSeconds,
                           @Value("${app.sms.resend-interval-seconds}") int resendIntervalSeconds) {
        this.smsCodeMapper = smsCodeMapper;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.smsExpireSeconds = smsExpireSeconds;
        this.resendIntervalSeconds = resendIntervalSeconds;
    }

    @Override
    public SendSmsResponseVo sendSms(SendSmsRequest request) {
        ValidationUtils.phone(request.getPhone());
        LocalDateTime now = TimeUtils.now();
        SmsCode latest = smsCodeMapper.selectLatestByPhone(request.getPhone());
        if (latest != null && latest.getCreateTime() != null
            && latest.getCreateTime().plusSeconds(resendIntervalSeconds).isAfter(now)) {
            throw new BusinessException(409, "验证码发送过于频繁，请稍后再试");
        }

        smsCodeMapper.markUnusedAsUsed(request.getPhone(), now);

        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(request.getPhone());
        smsCode.setSmsCode(String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000)));
        smsCode.setExpiredAt(now.plusSeconds(smsExpireSeconds));
        smsCode.setUsedStatus(0);
        smsCodeMapper.insert(smsCode);
        return new SendSmsResponseVo(smsExpireSeconds);
    }

    @Override
    @Transactional
    public LoginResponseVo login(LoginRequest request) {
        ValidationUtils.phone(request.getPhone());
        ValidationUtils.smsCode(request.getCode());
        LocalDateTime now = TimeUtils.now();

        SmsCode smsCode = smsCodeMapper.selectLatestUnusedByPhone(request.getPhone());
        if (smsCode == null || !request.getCode().equals(smsCode.getSmsCode())) {
            throw new BusinessException(400, "验证码错误");
        }
        if (smsCode.getExpiredAt().isBefore(now)) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        smsCodeMapper.markUsed(smsCode.getId(), now);

        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            user = new User();
            user.setPhone(request.getPhone());
            user.setNickname("玩家0000");
            user.setAvatar("");
            user.setCreditScore(100);
            userMapper.insert(user);

            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setNickname(defaultNickname(user.getId()));
            userMapper.updateProfile(updateUser);
            user = userMapper.selectById(user.getId());
        }

        return new LoginResponseVo(
            jwtUtils.generateToken(user.getId()),
            jwtUtils.getExpireSeconds(),
            toUserVo(user)
        );
    }

    @Override
    public UserVo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "未登录或 token 无效");
        }
        return toUserVo(user);
    }

    private String defaultNickname(Long userId) {
        String raw = String.valueOf(userId % 10000);
        return "玩家" + String.format("%04d", Integer.parseInt(raw));
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

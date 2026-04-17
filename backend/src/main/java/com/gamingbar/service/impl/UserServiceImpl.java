package com.gamingbar.service.impl;

import com.gamingbar.common.util.ValidationUtils;
import com.gamingbar.dto.user.UpdateProfileRequest;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.UserService;
import com.gamingbar.vo.user.UserVo;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserVo updateProfile(Long userId, UpdateProfileRequest request) {
        boolean hasNickname = request.getNickname() != null;
        boolean hasAvatar = request.getAvatar() != null;
        ValidationUtils.badRequest(hasNickname || hasAvatar);

        if (hasNickname) {
            String nickname = request.getNickname();
            ValidationUtils.badRequest(!nickname.isBlank()
                && nickname.length() >= 2
                && nickname.length() <= 20
                && !nickname.contains("\n")
                && !nickname.contains("\t"));
        }
        if (hasAvatar) {
            String avatar = request.getAvatar();
            ValidationUtils.badRequest(avatar.isEmpty()
                || avatar.startsWith("http://")
                || avatar.startsWith("https://"));
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setNickname(request.getNickname());
        updateUser.setAvatar(request.getAvatar());
        userMapper.updateProfile(updateUser);

        User user = userMapper.selectById(userId);
        return new UserVo(user.getId(), user.getPhone(), user.getNickname(), user.getAvatar(), user.getCreditScore());
    }
}

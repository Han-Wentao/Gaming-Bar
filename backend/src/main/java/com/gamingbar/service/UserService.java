package com.gamingbar.service;

import com.gamingbar.dto.user.UpdateProfileRequest;
import com.gamingbar.vo.user.UserVo;

public interface UserService {

    UserVo updateProfile(Long userId, UpdateProfileRequest request);
}

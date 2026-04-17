package com.gamingbar.controller.user;

import com.gamingbar.common.context.UserContext;
import com.gamingbar.common.result.ApiResponse;
import com.gamingbar.dto.user.UpdateProfileRequest;
import com.gamingbar.service.UserService;
import com.gamingbar.vo.user.UserVo;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    public ApiResponse<UserVo> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(UserContext.getUserId(), request));
    }
}

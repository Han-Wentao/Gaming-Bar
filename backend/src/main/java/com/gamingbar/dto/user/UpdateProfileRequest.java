package com.gamingbar.dto.user;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String avatar;
}

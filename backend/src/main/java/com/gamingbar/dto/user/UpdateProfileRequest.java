package com.gamingbar.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 20)
    private String nickname;

    @Size(max = 255)
    private String avatar;
}

package com.gamingbar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendSmsRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$")
    private String phone;
}

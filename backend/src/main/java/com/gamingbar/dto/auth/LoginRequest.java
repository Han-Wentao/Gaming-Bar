package com.gamingbar.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {

    private String phone;
    private String code;
}

package com.gamingbar.controller;

import com.gamingbar.common.result.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.success(Map.of(
            "service", "GamingBar API",
            "status", "running",
            "base_path", "/api"
        ));
    }
}

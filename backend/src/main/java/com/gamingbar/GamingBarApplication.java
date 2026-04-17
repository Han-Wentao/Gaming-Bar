package com.gamingbar;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gamingbar.mapper")
public class GamingBarApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamingBarApplication.class, args);
    }
}

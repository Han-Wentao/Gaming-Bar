package com.gamingbar.service.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "real")
public class RealSmsService implements SmsService {

    @Override
    public void sendCode(String phone, String code) {
        throw new IllegalStateException("RealSmsService is reserved for external provider integration.");
    }
}

package com.gamingbar.service.sms;

public interface SmsService {

    void sendCode(String phone, String code);
}

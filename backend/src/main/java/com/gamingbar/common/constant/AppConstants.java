package com.gamingbar.common.constant;

public final class AppConstants {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_ZONE = "Asia/Shanghai";
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";
    public static final String CLIENT_IP_HEADER = "X-Forwarded-For";
    public static final String SESSION_VERSION_PREFIX = "auth:session:version:";
    public static final String WS_TICKET_PREFIX = "auth:ws-ticket:";

    private AppConstants() {
    }
}

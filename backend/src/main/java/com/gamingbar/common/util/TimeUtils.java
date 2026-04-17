package com.gamingbar.common.util;

import com.gamingbar.common.constant.AppConstants;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final ZoneId ZONE_ID = ZoneId.of(AppConstants.TIME_ZONE);
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern(AppConstants.DATE_TIME_PATTERN);

    private TimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }

    public static LocalDateTime parse(String value) {
        return value == null ? null : LocalDateTime.parse(value, FORMATTER);
    }
}

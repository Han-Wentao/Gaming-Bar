package com.gamingbar.common.util;

import com.gamingbar.common.exception.BusinessException;
import java.util.Collection;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

    private ValidationUtils() {
    }

    public static void require(boolean condition, int code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }

    public static void badRequest(boolean condition) {
        require(condition, 400, "参数不合法");
    }

    public static void phone(String phone) {
        badRequest(phone != null && PHONE_PATTERN.matcher(phone).matches());
    }

    public static void smsCode(String code) {
        badRequest(code != null && CODE_PATTERN.matcher(code).matches());
    }

    public static void positive(Long value) {
        badRequest(value != null && value > 0);
    }

    public static void positiveInt(Integer value) {
        badRequest(value != null && value > 0);
    }

    public static void notBlankMessage(String value, int maxLength) {
        badRequest(value != null && !value.isBlank() && value.length() <= maxLength);
    }

    public static void page(Integer page, Integer size, int maxSize) {
        badRequest((page == null || page > 0) && (size == null || (size > 0 && size <= maxSize)));
    }

    public static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}

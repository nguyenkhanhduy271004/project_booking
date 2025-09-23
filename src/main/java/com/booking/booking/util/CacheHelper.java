package com.booking.booking.util;

import org.springframework.stereotype.Component;

@Component
public class CacheHelper {
    public boolean shouldSkipCaching(String result) {
        return result == null || result.contains("lỗi");
    }
}

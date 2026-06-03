package com.booktrading.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNoGenerator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generate() {
        String time = LocalDateTime.now().format(FMT);
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return time + random;
    }
}

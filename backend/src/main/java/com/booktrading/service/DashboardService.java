package com.booktrading.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getDashboardData();

    Map<String, Object> getDashboardDataWithDays(int days);

    long getUserCount();

    long getBookCount();

    long getOrderCount();

    long getDisputeCount();

    java.math.BigDecimal getTotalRevenue();

    List<Map<String, Object>> getCategoryStats();

    List<Map<String, Object>> getDailyStats(int days);

    List<Map<String, Object>> getDailyRevenueStats(int days);
}

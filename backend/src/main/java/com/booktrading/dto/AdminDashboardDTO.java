package com.booktrading.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AdminDashboardDTO {
    private long totalUsers;
    private long totalBooks;
    private long totalOrders;
    private java.math.BigDecimal totalRevenue;
    private List<Map<String, Object>> categoryStats;
    private List<Map<String, Object>> dailyStats;
}

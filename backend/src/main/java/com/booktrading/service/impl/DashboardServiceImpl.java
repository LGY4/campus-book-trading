package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booktrading.entity.*;
import com.booktrading.mapper.*;
import com.booktrading.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Map<String, Object> getDashboardData() {
        return getDashboardDataWithDays(7);
    }

    @Override
    public Map<String, Object> getDashboardDataWithDays(int days) {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", getUserCount());
        data.put("bookCount", getBookCount());
        data.put("orderCount", getOrderCount());
        data.put("disputeCount", getDisputeCount());
        data.put("totalRevenue", getTotalRevenue());
        data.put("categoryStats", getCategoryStats());
        data.put("dailyStats", getDailyStats(days));
        data.put("dailyRevenue", getDailyRevenueStats(days));
        return data;
    }

    @Override
    public long getUserCount() {
        return userMapper.selectCount(null);
    }

    @Override
    public long getBookCount() {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, "ON_SALE");
        return bookMapper.selectCount(wrapper);
    }

    @Override
    public long getOrderCount() {
        return orderMapper.selectCount(null);
    }

    @Override
    public long getDisputeCount() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "DISPUTE");
        return orderMapper.selectCount(wrapper);
    }

    @Override
    public BigDecimal getTotalRevenue() {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "COMPLETED");
        wrapper.select("COALESCE(SUM(price), 0) as totalRevenue");
        List<Map<String, Object>> result = orderMapper.selectMaps(wrapper);
        if (result.isEmpty() || result.get(0).get("totalRevenue") == null) {
            return BigDecimal.ZERO;
        }
        Object val = result.get(0).get("totalRevenue");
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return new BigDecimal(val.toString());
    }

    @Override
    public List<Map<String, Object>> getCategoryStats() {
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ON_SALE");
        wrapper.select("category_id", "COUNT(*) as bookCount");
        wrapper.groupBy("category_id");
        List<Map<String, Object>> result = bookMapper.selectMaps(wrapper);

        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = new HashMap<>();
        for (Category c : categories) {
            categoryNameMap.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map<String, Object> row : result) {
            Map<String, Object> stat = new HashMap<>();
            Long categoryId = ((Number) row.get("category_id")).longValue();
            stat.put("categoryId", categoryId);
            stat.put("categoryName", categoryNameMap.getOrDefault(categoryId, "未知"));
            stat.put("bookCount", ((Number) row.get("bookCount")).longValue());
            stats.add(stat);
        }
        return stats;
    }

    @Override
    public List<Map<String, Object>> getDailyStats(int days) {
        List<Map<String, Object>> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>()
                            .between(Order::getCreateTime, dayStart, dayEnd));

            long userCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .between(User::getCreateTime, dayStart, dayEnd));

            long bookCount = bookMapper.selectCount(
                    new LambdaQueryWrapper<Book>()
                            .between(Book::getCreateTime, dayStart, dayEnd));

            Map<String, Object> stat = new HashMap<>();
            stat.put("date", date.format(formatter));
            stat.put("orderCount", orderCount);
            stat.put("newUsers", userCount);
            stat.put("newBooks", bookCount);
            stats.add(stat);
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getDailyRevenueStats(int days) {
        List<Map<String, Object>> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            QueryWrapper<Order> wrapper = new QueryWrapper<>();
            wrapper.eq("status", "COMPLETED")
                    .between("update_time", dayStart, dayEnd)
                    .select("COALESCE(SUM(price), 0) as revenue");
            List<Map<String, Object>> result = orderMapper.selectMaps(wrapper);

            BigDecimal revenue = BigDecimal.ZERO;
            if (!result.isEmpty() && result.get(0).get("revenue") != null) {
                Object val = result.get(0).get("revenue");
                revenue = val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString());
            }

            Map<String, Object> stat = new HashMap<>();
            stat.put("date", date.format(formatter));
            stat.put("revenue", revenue);
            stats.add(stat);
        }
        return stats;
    }
}

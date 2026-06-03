package com.booktrading.controller.admin;

import com.booktrading.dto.Result;
import com.booktrading.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/data")
    public Result<?> getDashboardData(@RequestParam(defaultValue = "7") int days) {
        days = Math.min(Math.max(days, 1), 90);
        return Result.ok(dashboardService.getDashboardDataWithDays(days));
    }
}

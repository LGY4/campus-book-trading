package com.booktrading.controller.admin;

import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.service.OperationLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/log")
public class AdminLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String module,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        IPage<?> result = operationLogService.listLogs(page, size, keyword, module, startDate, endDate);
        return Result.ok(PageResult.from(result));
    }
}

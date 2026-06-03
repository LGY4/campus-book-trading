package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Banner;
import com.booktrading.exception.BusinessException;
import com.booktrading.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        return Result.ok(PageResult.from(bannerService.listAdmin(page, size)));
    }

    @PostMapping("/create")
    @LogOperation(module = "轮播", action = "创建")
    public Result<?> create(@RequestBody Banner banner) {
        bannerService.createBanner(banner);
        return Result.ok("创建成功");
    }

    @PutMapping("/update")
    @LogOperation(module = "轮播", action = "更新")
    public Result<?> update(@RequestBody Banner banner) {
        bannerService.updateBanner(banner);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "轮播", action = "删除")
    public Result<?> delete(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return Result.ok("删除成功");
    }

    @PutMapping("/status/{id}")
    @LogOperation(module = "轮播", action = "状态变更")
    public Result<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        if (statusObj == null) {
            throw new BusinessException("缺少status参数");
        }
        Integer status = Integer.valueOf(statusObj.toString());
        if (status != 0 && status != 1) {
            throw new BusinessException("status值不合法");
        }
        bannerService.toggleStatus(id, status);
        return Result.ok("状态已更新");
    }
}

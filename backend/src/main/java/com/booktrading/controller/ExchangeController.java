package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.ExchangeCreateDTO;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.ExchangeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/create")
    @LogOperation(module = "交换", action = "发起")
    public Result<?> create(@RequestBody ExchangeCreateDTO dto) {
        exchangeService.createExchange(dto, currentUser.getId());
        return Result.ok("交换请求已发送");
    }

    @PutMapping("/accept/{id}")
    @LogOperation(module = "交换", action = "接受")
    public Result<?> accept(@PathVariable Long id) {
        exchangeService.acceptExchange(id, currentUser.getId());
        return Result.ok("已接受交换");
    }

    @PutMapping("/reject/{id}")
    @LogOperation(module = "交换", action = "拒绝")
    public Result<?> reject(@PathVariable Long id) {
        exchangeService.rejectExchange(id, currentUser.getId());
        return Result.ok("已拒绝交换");
    }

    @PutMapping("/complete/{id}")
    @LogOperation(module = "交换", action = "完成")
    public Result<?> complete(@PathVariable Long id) {
        exchangeService.completeExchange(id, currentUser.getId());
        return Result.ok("交换已完成");
    }

    @GetMapping("/my")
    public Result<?> myExchanges(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String type) {
        IPage<?> result = exchangeService.getMyExchanges(currentUser.getId(), page, size, type);
        return Result.ok(PageResult.from(result));
    }

    @PutMapping("/cancel/{id}")
    @LogOperation(module = "交换", action = "取消")
    public Result<?> cancel(@PathVariable Long id) {
        exchangeService.cancelExchange(id, currentUser.getId());
        return Result.ok("已撤回交换请求");
    }
}

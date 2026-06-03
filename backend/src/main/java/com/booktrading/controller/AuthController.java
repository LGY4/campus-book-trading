package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.LoginRequest;
import com.booktrading.dto.RegisterRequest;
import com.booktrading.dto.Result;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @LogOperation(module = "认证", action = "注册")
    public Result<?> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.ok("注册成功");
    }

    @PostMapping("/login")
    @LogOperation(module = "认证", action = "登录")
    public Result<?> login(@RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.ok(data);
    }

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/info")
    public Result<?> getCurrentUser() {
        Long userId = currentUser.getId();
        return Result.ok(userService.getUserById(userId));
    }
}

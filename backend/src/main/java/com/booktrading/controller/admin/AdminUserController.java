package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String role) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        IPage<?> result = userService.listUsers(page, size, keyword, status, role);
        return Result.ok(PageResult.from(result));
    }

    @PutMapping("/status/{id}")
    @LogOperation(module = "用户管理", action = "修改状态")
    public Result<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || !status.matches("ACTIVE|BANNED")) {
            throw new BusinessException("无效的状态值");
        }
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getStatus, status));
        return Result.ok("状态更新成功");
    }

    @PutMapping("/role/{id}")
    @LogOperation(module = "用户管理", action = "修改角色")
    public Result<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || !role.matches("STUDENT|ADMIN")) {
            throw new BusinessException("无效的角色值");
        }
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getRole, role));
        return Result.ok("角色更新成功");
    }

    @GetMapping("/export")
    @LogOperation(module = "用户管理", action = "导出")
    public void export(@RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        List<User> users = userService.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.println("ID,用户名,昵称,手机号,邮箱,角色,信誉分,状态,注册时间");
        for (User u : users) {
            writer.println(u.getId() + ","
                    + escape(u.getUsername()) + ","
                    + escape(u.getNickname()) + ","
                    + escape(u.getPhone()) + ","
                    + escape(u.getEmail()) + ","
                    + u.getRole() + ","
                    + u.getReputationScore() + ","
                    + u.getStatus() + ","
                    + u.getCreateTime());
        }
        writer.flush();
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getNickname, keyword)
                .or().like(User::getPhone, keyword));
        wrapper.last("LIMIT 20");
        List<Map<String, Object>> list = userService.list(wrapper).stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("label", u.getNickname() + " (" + u.getUsername() + ")");
            return m;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    private String escape(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}

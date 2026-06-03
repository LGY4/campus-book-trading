package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.Result;
import com.booktrading.entity.Category;
import com.booktrading.exception.BusinessException;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(categoryService.listAll());
    }

    @PostMapping("/create")
    @LogOperation(module = "类别", action = "创建")
    public Result<?> create(@RequestBody Category category) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
        categoryService.createCategory(category);
        return Result.ok("创建成功");
    }

    @PutMapping("/update")
    @LogOperation(module = "类别", action = "更新")
    public Result<?> update(@RequestBody Category category) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
        categoryService.updateCategory(category);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "类别", action = "删除")
    public Result<?> delete(@PathVariable Long id) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
        categoryService.deleteCategory(id);
        return Result.ok("删除成功");
    }
}

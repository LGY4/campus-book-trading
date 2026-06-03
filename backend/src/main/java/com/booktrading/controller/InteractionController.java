package com.booktrading.controller;

import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.InteractionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/favorite/{bookId}")
    public Result<?> toggleFavorite(@PathVariable Long bookId) {
        boolean favorited = interactionService.toggleFavorite(currentUser.getId(), bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        data.put("message", favorited ? "已收藏" : "已取消收藏");
        return Result.ok(data);
    }

    @GetMapping("/favorite/list")
    public Result<?> getFavorites(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = interactionService.getFavorites(currentUser.getId(), page, size);
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/footprint")
    public Result<?> getFootprint(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = interactionService.getFootprint(currentUser.getId(), page, size);
        return Result.ok(PageResult.from(result));
    }

    @DeleteMapping("/footprint/{id}")
    public Result<?> deleteFootprint(@PathVariable Long id) {
        interactionService.deleteInteraction(id, currentUser.getId());
        return Result.ok("已删除");
    }

    @DeleteMapping("/footprint/clear")
    public Result<?> clearFootprint() {
        interactionService.clearFootprint(currentUser.getId());
        return Result.ok("已清空");
    }

    @PostMapping("/want/{bookId}")
    public Result<?> toggleWant(@PathVariable Long bookId) {
        boolean wanted = interactionService.toggleWant(currentUser.getId(), bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("wanted", wanted);
        data.put("message", wanted ? "已标记想要" : "已取消想要");
        return Result.ok(data);
    }

    @GetMapping("/want/status/{bookId}")
    public Result<?> getWantStatus(@PathVariable Long bookId) {
        boolean wanted = interactionService.isWanted(currentUser.getId(), bookId);
        return Result.ok(wanted);
    }

    @GetMapping("/want/list")
    public Result<?> getWants(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = interactionService.getWants(currentUser.getId(), page, size);
        return Result.ok(PageResult.from(result));
    }
}

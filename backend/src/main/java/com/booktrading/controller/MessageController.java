package com.booktrading.controller;

import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.MessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/list")
    public Result<?> getMyMessages(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String type) {
        IPage<?> result = messageService.getMyMessages(currentUser.getId(), page, size, type);
        return Result.ok(PageResult.from(result));
    }

    @PutMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id, currentUser.getId());
        return Result.ok("已读");
    }

    @PutMapping("/read-all")
    public Result<?> markAllAsRead() {
        messageService.markAllAsRead(currentUser.getId());
        return Result.ok("全部已读");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id, currentUser.getId());
        return Result.ok("已删除");
    }

    @GetMapping("/unread-count")
    public Result<?> getUnreadCount() {
        return Result.ok(messageService.getUnreadCount(currentUser.getId()));
    }
}

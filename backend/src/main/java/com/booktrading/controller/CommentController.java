package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.CommentCreateDTO;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.CommentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/create")
    @LogOperation(module = "评论", action = "创建")
    public Result<?> create(@RequestBody CommentCreateDTO dto) {
        if (dto.getParentId() != null) {
            commentService.createFollowUp(dto, currentUser.getId());
            return Result.ok("追评成功");
        }
        commentService.createComment(dto, currentUser.getId());
        return Result.ok("评论成功");
    }

    @GetMapping("/book/{bookId}")
    public Result<?> getBookComments(@PathVariable Long bookId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = commentService.getBookComments(bookId, page, size);
        return Result.ok(PageResult.from(result));
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "评论", action = "删除")
    public Result<?> delete(@PathVariable Long id) {
        commentService.deleteComment(id, currentUser.getId());
        return Result.ok("删除成功");
    }
}

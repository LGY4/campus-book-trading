package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Book;
import com.booktrading.entity.Comment;
import com.booktrading.entity.User;
import com.booktrading.service.BookService;
import com.booktrading.service.CommentService;
import com.booktrading.service.UserService;
import com.booktrading.vo.CommentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/comment")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer minRating,
                          @RequestParam(required = false) Integer maxRating,
                          @RequestParam(required = false) Long bookId,
                          @RequestParam(required = false) Long userId) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Comment::getContent, keyword);
        }
        if (minRating != null) {
            wrapper.ge(Comment::getRating, minRating);
        }
        if (maxRating != null) {
            wrapper.le(Comment::getRating, maxRating);
        }
        if (bookId != null) {
            wrapper.eq(Comment::getBookId, bookId);
        }
        if (userId != null) {
            wrapper.eq(Comment::getUserId, userId);
        }
        wrapper.orderByDesc(Comment::getCreateTime);
        IPage<Comment> commentPage = commentService.page(new Page<>(page, size), wrapper);

        // Batch-fetch to avoid N+1
        Set<Long> bookIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        commentPage.getRecords().forEach(c -> {
            if (c.getBookId() != null) bookIds.add(c.getBookId());
            if (c.getUserId() != null) userIds.add(c.getUserId());
        });
        Map<Long, Book> bookMap = bookIds.isEmpty() ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        IPage<CommentVO> voPage = commentPage.convert(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setOrderId(c.getOrderId());
            vo.setBookId(c.getBookId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setRating(c.getRating());
            vo.setCreateTime(c.getCreateTime());
            Book book = bookMap.get(c.getBookId());
            if (book != null) vo.setBookTitle(book.getTitle());
            User user = userMap.get(c.getUserId());
            if (user != null) vo.setUserNickname(user.getNickname());
            return vo;
        });
        return Result.ok(PageResult.from(voPage));
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "评论管理", action = "删除评论")
    public Result<?> delete(@PathVariable Long id) {
        commentService.adminDeleteComment(id);
        return Result.ok("删除成功");
    }
}

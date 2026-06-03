package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.BookQueryDTO;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Book;

import java.util.List;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.BookService;
import com.booktrading.service.InteractionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/list")
    public Result<?> list(BookQueryDTO queryDTO) {
        queryDTO.setUserId(currentUser.getId());
        IPage<?> result = bookService.searchBooks(queryDTO);
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Long userId = currentUser.getId();
        if (userId != null) {
            interactionService.recordView(userId, id);
        }
        return Result.ok(bookService.getBookDetail(id, userId));
    }

    @PostMapping("/publish")
    @LogOperation(module = "书籍", action = "发布")
    public Result<?> publish(@RequestBody Book book) {
        book.setSellerId(currentUser.getId());
        Book saved = bookService.publishBook(book);
        return Result.ok(saved);
    }

    @PutMapping("/update")
    @LogOperation(module = "书籍", action = "更新")
    public Result<?> update(@RequestBody Book book) {
        Book updated = bookService.updateBook(book, currentUser.getId());
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "书籍", action = "删除")
    public Result<?> delete(@PathVariable Long id) {
        bookService.deleteBook(id, currentUser.getId());
        return Result.ok("删除成功");
    }

    @PutMapping("/delist/{id}")
    @LogOperation(module = "书籍", action = "下架")
    public Result<?> delist(@PathVariable Long id) {
        bookService.delistBook(id, currentUser.getId());
        return Result.ok("下架成功");
    }

    @PostMapping("/batch-delete")
    @LogOperation(module = "书籍", action = "批量删除")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        bookService.batchDeleteBooks(ids, currentUser.getId());
        return Result.ok("批量删除成功");
    }

    @GetMapping("/my")
    public Result<?> myBooks(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = bookService.getMyBooks(currentUser.getId(), page, size);
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/recommend")
    public Result<?> recommend(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(bookService.getRecommendations(currentUser.getId(), limit));
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        IPage<?> result = bookService.fullTextSearch(keyword, page, size, currentUser.getId());
        return Result.ok(PageResult.from(result));
    }
}

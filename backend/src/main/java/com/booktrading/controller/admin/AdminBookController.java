package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Book;
import com.booktrading.exception.BusinessException;
import com.booktrading.service.BookService;
import com.booktrading.vo.BookVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/book")
public class AdminBookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long categoryId) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthor, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Book::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        IPage<Book> bookPage = bookService.page(new Page<>(page, size), wrapper);
        IPage<BookVO> voPage = bookPage.convert(book -> bookService.getBookDetail(book.getId()));
        return Result.ok(PageResult.from(voPage));
    }

    @PutMapping("/status/{id}")
    @LogOperation(module = "书籍管理", action = "修改状态")
    public Result<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || !status.matches("ON_SALE|OFF_SHELF|SOLD|PENDING|REJECTED")) {
            throw new BusinessException("无效的状态值");
        }
        if ("ON_SALE".equals(status)) {
            Book book = bookService.getById(id);
            if (book == null) {
                throw new BusinessException("图书不存在");
            }
            if (book.getQuantity() == null || book.getQuantity() <= 0) {
                throw new BusinessException("库存为0，无法上架");
            }
        }
        bookService.update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, id)
                .set(Book::getStatus, status));
        return Result.ok("状态更新成功");
    }

    @PostMapping("/approve/{id}")
    @LogOperation(module = "书籍管理", action = "审核通过")
    public Result<?> approve(@PathVariable Long id) {
        bookService.approveBook(id);
        return Result.ok("审核通过");
    }

    @PostMapping("/reject/{id}")
    @LogOperation(module = "书籍管理", action = "审核拒绝")
    public Result<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "不符合发布规范";
        bookService.rejectBook(id, reason);
        return Result.ok("已驳回");
    }

    @GetMapping("/export")
    @LogOperation(module = "书籍管理", action = "导出")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Long categoryId,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthor, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Book::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        List<Book> books = bookService.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=books.csv");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.println("ID,书名,作者,ISBN,原价,售价,库存,品相,状态,发布时间");
        for (Book b : books) {
            writer.println(b.getId() + ","
                    + escape(b.getTitle()) + ","
                    + escape(b.getAuthor()) + ","
                    + escape(b.getIsbn()) + ","
                    + b.getOriginalPrice() + ","
                    + b.getSellingPrice() + ","
                    + b.getQuantity() + ","
                    + escape(b.getBookCondition()) + ","
                    + b.getStatus() + ","
                    + b.getCreateTime());
        }
        writer.flush();
    }

    private String escape(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}

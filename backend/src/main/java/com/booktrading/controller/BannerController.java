package com.booktrading.controller;

import com.booktrading.dto.Result;
import com.booktrading.entity.Banner;
import com.booktrading.entity.Book;
import com.booktrading.service.BannerService;
import com.booktrading.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private BookService bookService;

    @GetMapping("/list")
    public Result<?> list() {
        List<Banner> banners = bannerService.listEnabled();
        if (banners.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        Set<Long> bookIds = banners.stream()
                .map(Banner::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookIds.isEmpty()
                ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream()
                        .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Banner banner : banners) {
            Book book = bookMap.get(banner.getBookId());
            if (book == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", banner.getId());
            item.put("bookId", banner.getBookId());
            item.put("title", banner.getTitle() != null ? banner.getTitle() : book.getTitle());
            item.put("imageUrl", banner.getImageUrl() != null ? banner.getImageUrl() : book.getCoverImage());
            item.put("bookTitle", book.getTitle());
            item.put("sellingPrice", book.getSellingPrice());
            item.put("coverImage", book.getCoverImage());
            result.add(item);
        }
        return Result.ok(result);
    }
}

package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.dto.BookQueryDTO;
import com.booktrading.entity.Book;
import com.booktrading.vo.BookVO;

import java.util.List;

public interface BookService extends IService<Book> {

    Book publishBook(Book book);

    Book updateBook(Book book, Long userId);

    void deleteBook(Long id, Long sellerId);

    BookVO getBookDetail(Long id);

    BookVO getBookDetail(Long id, Long userId);

    IPage<BookVO> searchBooks(BookQueryDTO queryDTO);

    IPage<BookVO> fullTextSearch(String keyword, int page, int size, Long userId);

    IPage<Book> getMyBooks(Long sellerId, int page, int size);

    void incrementView(Long id);

    void incrementWant(Long id);

    void delistBook(Long id, Long sellerId);

    void batchDeleteBooks(List<Long> ids, Long sellerId);

    List<BookVO> getRecommendations(Long userId, int limit);

    void approveBook(Long id);

    void rejectBook(Long id, String reason);
}

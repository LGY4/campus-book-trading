package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Interaction;
import com.booktrading.vo.BookVO;

import java.util.Set;

public interface InteractionService extends IService<Interaction> {

    boolean toggleFavorite(Long userId, Long bookId);

    boolean isFavorited(Long userId, Long bookId);

    Set<Long> getFavoriteBookIds(Long userId);

    Set<Long> getWantedBookIds(Long userId);

    IPage<BookVO> getFavorites(Long userId, int page, int size);

    void recordView(Long userId, Long bookId);

    IPage<BookVO> getFootprint(Long userId, int page, int size);

    void deleteInteraction(Long interactionId, Long userId);

    void clearFootprint(Long userId);

    boolean toggleWant(Long userId, Long bookId);

    boolean isWanted(Long userId, Long bookId);

    IPage<BookVO> getWants(Long userId, int page, int size);
}

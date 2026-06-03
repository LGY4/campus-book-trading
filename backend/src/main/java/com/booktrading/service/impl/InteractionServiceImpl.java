package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Book;
import com.booktrading.entity.Interaction;
import com.booktrading.entity.User;
import com.booktrading.mapper.InteractionMapper;
import com.booktrading.service.BookService;
import com.booktrading.service.InteractionService;
import com.booktrading.service.UserService;
import com.booktrading.vo.BookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InteractionServiceImpl extends ServiceImpl<InteractionMapper, Interaction> implements InteractionService {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getBookId, bookId)
                .eq(Interaction::getType, "FAVORITE");
        Interaction existing = getOne(wrapper);

        if (existing != null) {
            removeById(existing.getId());
            return false;
        } else {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setBookId(bookId);
            interaction.setType("FAVORITE");
            interaction.setCreateTime(LocalDateTime.now());
            save(interaction);
            return true;
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long bookId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getBookId, bookId)
                .eq(Interaction::getType, "FAVORITE");
        return count(wrapper) > 0;
    }

    @Override
    public Set<Long> getFavoriteBookIds(Long userId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "FAVORITE")
                .select(Interaction::getBookId);
        return list(wrapper).stream()
                .map(Interaction::getBookId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getWantedBookIds(Long userId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "WANT")
                .select(Interaction::getBookId);
        return list(wrapper).stream()
                .map(Interaction::getBookId)
                .collect(Collectors.toSet());
    }

    @Override
    public IPage<BookVO> getFavorites(Long userId, int page, int size) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "FAVORITE");
        wrapper.orderByDesc(Interaction::getCreateTime);
        IPage<Interaction> interactionPage = page(new Page<>(page, size), wrapper);
        return batchConvertToBookVO(interactionPage);
    }

    @Override
    @Transactional
    public void recordView(Long userId, Long bookId) {
        if (userId == null) return;

        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getBookId, bookId)
                .eq(Interaction::getType, "VIEW")
                .last("LIMIT 1");
        Interaction existing = getOne(wrapper);

        if (existing == null) {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setBookId(bookId);
            interaction.setType("VIEW");
            interaction.setCreateTime(LocalDateTime.now());
            save(interaction);
            bookService.incrementView(bookId);
        } else {
            // 更新浏览时间，使足迹按最近浏览排序
            existing.setCreateTime(LocalDateTime.now());
            updateById(existing);
        }
    }

    @Override
    public IPage<BookVO> getFootprint(Long userId, int page, int size) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "VIEW");
        wrapper.orderByDesc(Interaction::getCreateTime);
        IPage<Interaction> interactionPage = page(new Page<>(page, size), wrapper);
        return batchConvertToBookVO(interactionPage);
    }

    @Override
    @Transactional
    public void deleteInteraction(Long interactionId, Long userId) {
        Interaction interaction = getById(interactionId);
        if (interaction == null) {
            return;
        }
        if (!interaction.getUserId().equals(userId)) {
            return;
        }
        removeById(interactionId);
    }

    @Override
    @Transactional
    public void clearFootprint(Long userId) {
        remove(new LambdaQueryWrapper<Interaction>()
                .eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "VIEW"));
    }

    @Override
    @Transactional
    public boolean toggleWant(Long userId, Long bookId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getBookId, bookId)
                .eq(Interaction::getType, "WANT");
        Interaction existing = getOne(wrapper);

        if (existing != null) {
            removeById(existing.getId());
            bookService.update(new LambdaUpdateWrapper<Book>()
                    .eq(Book::getId, bookId)
                    .setSql("want_count = GREATEST(want_count - 1, 0)"));
            return false;
        } else {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setBookId(bookId);
            interaction.setType("WANT");
            interaction.setCreateTime(LocalDateTime.now());
            save(interaction);
            bookService.incrementWant(bookId);
            return true;
        }
    }

    @Override
    public boolean isWanted(Long userId, Long bookId) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getBookId, bookId)
                .eq(Interaction::getType, "WANT");
        return count(wrapper) > 0;
    }

    @Override
    public IPage<BookVO> getWants(Long userId, int page, int size) {
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "WANT");
        wrapper.orderByDesc(Interaction::getCreateTime);
        IPage<Interaction> interactionPage = page(new Page<>(page, size), wrapper);
        return batchConvertToBookVO(interactionPage);
    }

    private IPage<BookVO> batchConvertToBookVO(IPage<Interaction> interactionPage) {
        Set<Long> bookIds = new HashSet<>();
        Set<Long> sellerIds = new HashSet<>();
        interactionPage.getRecords().forEach(i -> {
            if (i.getBookId() != null) bookIds.add(i.getBookId());
        });
        Map<Long, Book> bookMap = bookIds.isEmpty() ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        bookMap.values().forEach(b -> { if (b.getSellerId() != null) sellerIds.add(b.getSellerId()); });
        Map<Long, User> userMap = sellerIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(sellerIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        IPage<BookVO> result = interactionPage.convert(interaction -> {
            Book book = bookMap.get(interaction.getBookId());
            if (book == null) return null;
            BookVO vo = new BookVO();
            vo.setId(book.getId());
            vo.setTitle(book.getTitle());
            vo.setAuthor(book.getAuthor());
            vo.setCoverImage(book.getCoverImage());
            vo.setSellingPrice(book.getSellingPrice());
            vo.setOriginalPrice(book.getOriginalPrice());
            vo.setBookCondition(book.getBookCondition());
            vo.setStatus(book.getStatus());
            vo.setSellerId(book.getSellerId());
            vo.setQuantity(book.getQuantity());
            vo.setInteractionId(interaction.getId());
            vo.setViewTime(interaction.getCreateTime());
            vo.parseImages();

            User seller = book.getSellerId() != null ? userMap.get(book.getSellerId()) : null;
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(seller.getAvatar());
            }
            return vo;
        });
        // Filter out null entries from deleted books
        List<BookVO> filtered = result.getRecords().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int nullCount = result.getRecords().size() - filtered.size();
        result.setRecords(filtered);
        result.setTotal(Math.max(0, result.getTotal() - nullCount));
        return result;
    }

}

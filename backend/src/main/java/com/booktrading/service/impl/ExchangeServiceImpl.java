package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.dto.ExchangeCreateDTO;
import com.booktrading.entity.Book;
import com.booktrading.entity.Exchange;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.ExchangeMapper;
import com.booktrading.service.BookService;
import com.booktrading.service.ExchangeService;
import com.booktrading.service.MessageService;
import com.booktrading.service.UserService;
import com.booktrading.vo.ExchangeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExchangeServiceImpl extends ServiceImpl<ExchangeMapper, Exchange> implements ExchangeService {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public void createExchange(ExchangeCreateDTO dto, Long userId) {
        Book initiatorBook = bookService.getById(dto.getEffectiveInitiatorBookId());
        if (initiatorBook == null) {
            throw new BusinessException("您的图书不存在");
        }
        if (!initiatorBook.getSellerId().equals(userId)) {
            throw new BusinessException("只能用自己的图书发起交换");
        }
        cleanStuckBook(initiatorBook);
        if (!"ON_SALE".equals(initiatorBook.getStatus())) {
            throw new BusinessException("您的图书状态不可交换");
        }

        Book targetBook = bookService.getById(dto.getTargetBookId());
        if (targetBook == null) {
            throw new BusinessException("目标图书不存在");
        }
        cleanStuckBook(targetBook);
        if (!"ON_SALE".equals(targetBook.getStatus())) {
            throw new BusinessException("目标图书状态不可交换");
        }
        if (targetBook.getSellerId().equals(userId)) {
            throw new BusinessException("不能与自己的图书交换");
        }

        // 防止重复发起同一对书籍的交换请求
        long dupCount = count(new LambdaQueryWrapper<Exchange>()
                .eq(Exchange::getInitiatorBookId, dto.getEffectiveInitiatorBookId())
                .eq(Exchange::getTargetBookId, dto.getTargetBookId())
                .eq(Exchange::getStatus, "PENDING"));
        if (dupCount > 0) {
            throw new BusinessException("已存在相同的交换请求，请勿重复发起");
        }

        Exchange exchange = new Exchange();
        exchange.setInitiatorBookId(dto.getEffectiveInitiatorBookId());
        exchange.setTargetBookId(dto.getTargetBookId());
        exchange.setInitiatorId(userId);
        exchange.setTargetUserId(targetBook.getSellerId());
        exchange.setMessage(dto.getMessage());
        exchange.setStatus("PENDING");
        save(exchange);
        messageService.sendTradeMessage(targetBook.getSellerId(), userId,
                "您收到一个换书请求，对方想用书籍ID " + dto.getEffectiveInitiatorBookId() + " 换您的《" + targetBook.getTitle() + "》");
    }

    @Override
    @Transactional
    public void acceptExchange(Long id, Long userId) {
        Exchange exchange = getById(id);
        if (exchange == null) {
            throw new BusinessException("交换请求不存在");
        }
        if (!exchange.getTargetUserId().equals(userId)) {
            throw new BusinessException("无权操作此交换请求");
        }
        if (!"PENDING".equals(exchange.getStatus())) {
            throw new BusinessException("交换请求状态不正确");
        }
        // 验证书籍仍然可用
        Book initiatorBook = bookService.getById(exchange.getInitiatorBookId());
        if (initiatorBook == null) {
            throw new BusinessException("发起方图书不存在");
        }
        // 如果书籍卡在PENDING但无活跃交换，先恢复
        cleanStuckBook(initiatorBook);
        if (!"ON_SALE".equals(initiatorBook.getStatus()) && !"PENDING".equals(initiatorBook.getStatus())) {
            throw new BusinessException("发起方图书已不可用");
        }
        Book targetBook = bookService.getById(exchange.getTargetBookId());
        if (targetBook == null) {
            throw new BusinessException("目标图书不存在");
        }
        cleanStuckBook(targetBook);
        if (!"ON_SALE".equals(targetBook.getStatus()) && !"PENDING".equals(targetBook.getStatus())) {
            throw new BusinessException("目标图书已不可用");
        }

        exchange.setStatus("ACCEPTED");
        updateById(exchange);
        messageService.sendTradeMessage(exchange.getInitiatorId(), userId, "您的换书请求已被接受，请尽快完成交换");

        // 拒绝其他涉及相同书籍的待处理请求
        List<Exchange> others = list(new LambdaQueryWrapper<Exchange>()
                .eq(Exchange::getStatus, "PENDING")
                .ne(Exchange::getId, id)
                .and(w -> w.eq(Exchange::getInitiatorBookId, exchange.getInitiatorBookId())
                        .or().eq(Exchange::getTargetBookId, exchange.getInitiatorBookId())
                        .or().eq(Exchange::getInitiatorBookId, exchange.getTargetBookId())
                        .or().eq(Exchange::getTargetBookId, exchange.getTargetBookId())));
        for (Exchange other : others) {
            other.setStatus("REJECTED");
            updateById(other);
            messageService.sendTradeMessage(other.getInitiatorId(), userId, "您的换书请求因对方已接受其他交换而被自动拒绝");
        }

        initiatorBook.setStatus("PENDING");
        bookService.updateById(initiatorBook);
        targetBook.setStatus("PENDING");
        bookService.updateById(targetBook);
    }

    @Override
    @Transactional
    public void rejectExchange(Long id, Long userId) {
        Exchange exchange = getById(id);
        if (exchange == null) {
            throw new BusinessException("交换请求不存在");
        }
        if (!exchange.getTargetUserId().equals(userId)) {
            throw new BusinessException("无权操作此交换请求");
        }
        if (!"PENDING".equals(exchange.getStatus())) {
            throw new BusinessException("交换请求状态不正确");
        }
        exchange.setStatus("REJECTED");
        updateById(exchange);
        messageService.sendTradeMessage(exchange.getInitiatorId(), userId, "您的换书请求已被拒绝");
    }

    @Override
    @Transactional
    public void completeExchange(Long id, Long userId) {
        Exchange exchange = getById(id);
        if (exchange == null) {
            throw new BusinessException("交换请求不存在");
        }
        if (!exchange.getInitiatorId().equals(userId) && !exchange.getTargetUserId().equals(userId)) {
            throw new BusinessException("无权操作此交换请求");
        }
        if (!"ACCEPTED".equals(exchange.getStatus())) {
            throw new BusinessException("交换请求状态不正确");
        }
        exchange.setStatus("COMPLETED");
        updateById(exchange);
        Long otherId = userId.equals(exchange.getInitiatorId()) ? exchange.getTargetUserId() : exchange.getInitiatorId();
        messageService.sendTradeMessage(otherId, userId, "换书已完成");

        Book initiatorBook = bookService.getById(exchange.getInitiatorBookId());
        if (initiatorBook != null) {
            bookService.update(new LambdaUpdateWrapper<Book>()
                    .eq(Book::getId, initiatorBook.getId())
                    .setSql("quantity = quantity - 1"));
            Book updated = bookService.getById(initiatorBook.getId());
            if (updated != null && (updated.getQuantity() == null || updated.getQuantity() <= 0)) {
                updated.setStatus("SOLD");
            } else if (updated != null) {
                updated.setStatus("ON_SALE");
            }
            if (updated != null) bookService.updateById(updated);
        }
        Book targetBook = bookService.getById(exchange.getTargetBookId());
        if (targetBook != null) {
            bookService.update(new LambdaUpdateWrapper<Book>()
                    .eq(Book::getId, targetBook.getId())
                    .setSql("quantity = quantity - 1"));
            Book updated = bookService.getById(targetBook.getId());
            if (updated != null && (updated.getQuantity() == null || updated.getQuantity() <= 0)) {
                updated.setStatus("SOLD");
            } else if (updated != null) {
                updated.setStatus("ON_SALE");
            }
            if (updated != null) bookService.updateById(updated);
        }
    }

    @Override
    public IPage<ExchangeVO> getMyExchanges(Long userId, int page, int size, String type) {
        LambdaQueryWrapper<Exchange> wrapper = new LambdaQueryWrapper<>();
        if ("sent".equals(type)) {
            wrapper.eq(Exchange::getInitiatorId, userId);
        } else if ("received".equals(type)) {
            wrapper.eq(Exchange::getTargetUserId, userId);
        } else {
            wrapper.and(w -> w.eq(Exchange::getInitiatorId, userId)
                    .or().eq(Exchange::getTargetUserId, userId));
        }
        wrapper.orderByDesc(Exchange::getCreateTime);
        IPage<Exchange> exchangePage = page(new Page<>(page, size), wrapper);

        // Batch fetch to avoid N+1
        Set<Long> bookIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        exchangePage.getRecords().forEach(e -> {
            if (e.getInitiatorBookId() != null) bookIds.add(e.getInitiatorBookId());
            if (e.getTargetBookId() != null) bookIds.add(e.getTargetBookId());
            if (e.getInitiatorId() != null) userIds.add(e.getInitiatorId());
            if (e.getTargetUserId() != null) userIds.add(e.getTargetUserId());
        });
        Map<Long, Book> bookMap = bookIds.isEmpty() ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return exchangePage.convert(e -> toExchangeVO(e, bookMap, userMap));
    }

    private ExchangeVO toExchangeVO(Exchange exchange, Map<Long, Book> bookMap, Map<Long, User> userMap) {
        ExchangeVO vo = new ExchangeVO();
        vo.setId(exchange.getId());
        vo.setInitiatorBookId(exchange.getInitiatorBookId());
        vo.setTargetBookId(exchange.getTargetBookId());
        vo.setInitiatorId(exchange.getInitiatorId());
        vo.setTargetUserId(exchange.getTargetUserId());
        vo.setStatus(exchange.getStatus());
        vo.setMessage(exchange.getMessage());
        vo.setCreateTime(exchange.getCreateTime());
        vo.setUpdateTime(exchange.getUpdateTime());

        Book initiatorBook = exchange.getInitiatorBookId() != null ? bookMap.get(exchange.getInitiatorBookId()) : null;
        if (initiatorBook != null) {
            vo.setInitiatorBookTitle(initiatorBook.getTitle());
            vo.setInitiatorBookCover(initiatorBook.getCoverImage());
        }
        Book targetBook = exchange.getTargetBookId() != null ? bookMap.get(exchange.getTargetBookId()) : null;
        if (targetBook != null) {
            vo.setTargetBookTitle(targetBook.getTitle());
            vo.setTargetBookCover(targetBook.getCoverImage());
        }

        User initiator = exchange.getInitiatorId() != null ? userMap.get(exchange.getInitiatorId()) : null;
        if (initiator != null) {
            vo.setInitiatorNickname(initiator.getNickname());
            vo.setInitiatorAvatar(initiator.getAvatar());
        }
        User targetUser = exchange.getTargetUserId() != null ? userMap.get(exchange.getTargetUserId()) : null;
        if (targetUser != null) {
            vo.setTargetUserNickname(targetUser.getNickname());
            vo.setTargetUserAvatar(targetUser.getAvatar());
        }

        return vo;
    }

    @Override
    @Transactional
    public void cancelExchange(Long id, Long userId) {
        Exchange exchange = getById(id);
        if (exchange == null) {
            throw new BusinessException("交换请求不存在");
        }
        if (!exchange.getInitiatorId().equals(userId)) {
            throw new BusinessException("无权操作此交换请求");
        }
        if (!"PENDING".equals(exchange.getStatus())) {
            throw new BusinessException("交换请求状态不正确");
        }
        exchange.setStatus("CANCELLED");
        updateById(exchange);
        messageService.sendTradeMessage(exchange.getTargetUserId(), userId, "对方已撤回换书请求");

        // 恢复书籍状态
        restoreBookStatus(exchange.getInitiatorBookId());
        restoreBookStatus(exchange.getTargetBookId());
    }

    private void restoreBookStatus(Long bookId) {
        if (bookId == null) return;
        Book book = bookService.getById(bookId);
        if (book != null && "PENDING".equals(book.getStatus())) {
            long activeCount = count(new LambdaQueryWrapper<Exchange>()
                    .eq(Exchange::getStatus, "ACCEPTED")
                    .and(w -> w.eq(Exchange::getInitiatorBookId, bookId)
                            .or().eq(Exchange::getTargetBookId, bookId)));
            if (activeCount == 0) {
                book.setStatus("ON_SALE");
                bookService.updateById(book);
            }
        }
    }

    private void cleanStuckBook(Book book) {
        if (book == null || !"PENDING".equals(book.getStatus())) return;
        long activeCount = count(new LambdaQueryWrapper<Exchange>()
                .eq(Exchange::getStatus, "ACCEPTED")
                .and(w -> w.eq(Exchange::getInitiatorBookId, book.getId())
                        .or().eq(Exchange::getTargetBookId, book.getId())));
        if (activeCount == 0) {
            book.setStatus("ON_SALE");
            bookService.updateById(book);
        }
    }
}

package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.dto.BookQueryDTO;
import com.booktrading.entity.Book;
import com.booktrading.entity.Category;
import com.booktrading.entity.Interaction;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.BookMapper;
import com.booktrading.entity.Order;
import com.booktrading.service.BookService;
import com.booktrading.service.CategoryService;
import com.booktrading.service.InteractionService;
import com.booktrading.service.MessageService;
import com.booktrading.service.OrderService;
import com.booktrading.service.UserService;
import com.booktrading.vo.BookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    @Lazy
    private InteractionService interactionService;

    @Autowired
    @Lazy
    private OrderService orderService;

    @Autowired
    private MessageService messageService;

    @Override
    public Book publishBook(Book book) {
        User seller = userService.getById(book.getSellerId());
        boolean autoApprove = isFullReputation(seller);
        System.out.println("[BookService] publishBook sellerId=" + book.getSellerId()
                + " reputation=" + (seller != null ? seller.getReputationScore() : "null")
                + " autoApprove=" + autoApprove);
        book.setStatus(autoApprove ? "ON_SALE" : "PENDING");
        book.setViewCount(0);
        book.setWantCount(0);
        if (book.getQuantity() == null || book.getQuantity() < 1) {
            book.setQuantity(1);
        }
        save(book);
        System.out.println("[BookService] saved book id=" + book.getId() + " status=" + book.getStatus());
        return book;
    }

    @Override
    @Transactional
    public Book updateBook(Book book, Long userId) {
        Book existing = getById(book.getId());
        if (existing == null) {
            throw new BusinessException("图书不存在");
        }
        if (!existing.getSellerId().equals(userId)) {
            throw new BusinessException("无权修改此图书");
        }
        book.setSellerId(userId);
        if (!"SOLD".equals(existing.getStatus())) {
            User seller = userService.getById(userId);
            boolean autoApprove = isFullReputation(seller);
            System.out.println("[BookService] updateBook sellerId=" + userId
                    + " reputation=" + (seller != null ? seller.getReputationScore() : "null")
                    + " autoApprove=" + autoApprove);
            book.setStatus(autoApprove ? "ON_SALE" : "PENDING");
        }
        updateById(book);
        Book result = getById(book.getId());
        System.out.println("[BookService] updated book id=" + book.getId() + " status=" + result.getStatus());
        return result;
    }

    @Override
    @Transactional
    public void deleteBook(Long id, Long sellerId) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (!book.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权删除此图书");
        }
        if (hasActiveOrders(id)) {
            throw new BusinessException("图书有未完成的订单，无法删除");
        }
        removeById(id);
    }

    @Override
    public BookVO getBookDetail(Long id) {
        return getBookDetail(id, null);
    }

    @Override
    public BookVO getBookDetail(Long id, Long userId) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        BookVO vo = toBookVO(book);
        if (userId != null) {
            vo.setIsFavorited(interactionService.isFavorited(userId, id));
            vo.setIsWanted(interactionService.isWanted(userId, id));
        }
        return vo;
    }

    @Override
    public IPage<BookVO> searchBooks(BookQueryDTO queryDTO) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, "ON_SALE");

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Book::getTitle, queryDTO.getKeyword())
                    .or().like(Book::getAuthor, queryDTO.getKeyword())
                    .or().like(Book::getIsbn, queryDTO.getKeyword()));
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, queryDTO.getCategoryId());
        }
        if (queryDTO.getMinPrice() != null) {
            wrapper.ge(Book::getSellingPrice, queryDTO.getMinPrice());
        }
        if (queryDTO.getMaxPrice() != null) {
            wrapper.le(Book::getSellingPrice, queryDTO.getMaxPrice());
        }

        wrapper.orderByDesc(Book::getCreateTime);

        IPage<Book> page = page(new Page<>(queryDTO.getPage(), queryDTO.getPageSize()), wrapper);
        Long userId = queryDTO.getUserId();
        if (userId != null) {
            Set<Long> favoritedBookIds = interactionService.getFavoriteBookIds(userId);
            Set<Long> wantedBookIds = interactionService.getWantedBookIds(userId);
            return page.convert(book -> {
                BookVO vo = toBookVO(book);
                vo.setIsFavorited(favoritedBookIds.contains(book.getId()));
                vo.setIsWanted(wantedBookIds.contains(book.getId()));
                return vo;
            });
        }
        return page.convert(this::toBookVO);
    }

    @Override
    public IPage<BookVO> fullTextSearch(String keyword, int page, int size, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            return new Page<>(page, size);
        }
        QueryWrapper<Book> qw = new QueryWrapper<>();
        qw.eq("status", "ON_SALE");
        qw.and(w -> w.like("title", keyword).or().like("author", keyword));
        qw.orderByDesc("view_count");
        IPage<Book> bookPage = page(new Page<>(page, size), qw);
        if (userId != null) {
            Set<Long> favoritedBookIds = interactionService.getFavoriteBookIds(userId);
            Set<Long> wantedBookIds = interactionService.getWantedBookIds(userId);
            return bookPage.convert(book -> {
                BookVO vo = toBookVO(book);
                vo.setIsFavorited(favoritedBookIds.contains(book.getId()));
                vo.setIsWanted(wantedBookIds.contains(book.getId()));
                return vo;
            });
        }
        return bookPage.convert(this::toBookVO);
    }

    @Override
    public IPage<Book> getMyBooks(Long sellerId, int page, int size) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getSellerId, sellerId);
        wrapper.orderByDesc(Book::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void incrementView(Long id) {
        update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    @Override
    public void incrementWant(Long id) {
        update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, id)
                .setSql("want_count = want_count + 1"));
    }

    @Override
    @Transactional
    public void delistBook(Long id, Long sellerId) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (!book.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作此图书");
        }
        if (!"ON_SALE".equals(book.getStatus())) {
            throw new BusinessException("只有在售图书才能下架");
        }
        book.setStatus("OFF_SHELF");
        updateById(book);
    }

    @Override
    @Transactional
    public void batchDeleteBooks(List<Long> ids, Long sellerId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Book> books = listByIds(ids);
        for (Book book : books) {
            if (!book.getSellerId().equals(sellerId)) {
                throw new BusinessException("无权删除图书: " + book.getTitle());
            }
            if (hasActiveOrders(book.getId())) {
                throw new BusinessException("图书有未完成的订单，无法删除: " + book.getTitle());
            }
        }
        removeByIds(ids);
    }

    @Override
    @Transactional
    public void approveBook(Long id) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.setStatus("ON_SALE");
        updateById(book);
        messageService.sendSystemMessage(book.getSellerId(),
                "您的书籍《" + book.getTitle() + "》已通过审核，已上架");
    }

    @Override
    @Transactional
    public void rejectBook(Long id, String reason) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.setStatus("REJECTED");
        book.setRejectReason(reason);
        updateById(book);
        messageService.sendSystemMessage(book.getSellerId(),
                "您的书籍《" + book.getTitle() + "》未通过审核，原因：" + reason);
    }

    @Override
    public List<BookVO> getRecommendations(Long userId, int limit) {
        if (userId == null) {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getStatus, "ON_SALE");
            wrapper.orderByDesc(Book::getCreateTime);
            wrapper.last("LIMIT " + limit);
            return list(wrapper).stream().map(this::toBookVO).collect(Collectors.toList());
        }

        // 1. 协同过滤："买了这本书的人还买了"
        List<BookVO> collabRecs = getCollaborativeRecommendations(userId, limit);
        if (collabRecs.size() >= limit) {
            return collabRecs;
        }

        // 2. 基于分类的内容推荐（补充不足部分）
        Set<Long> excludeIds = collabRecs.stream().map(BookVO::getId).collect(Collectors.toSet());
        Set<Long> interactedBookIds = interactionService.getFavoriteBookIds(userId);
        excludeIds.addAll(interactedBookIds);

        LambdaQueryWrapper<Interaction> viewWrapper = new LambdaQueryWrapper<>();
        viewWrapper.eq(Interaction::getUserId, userId)
                .eq(Interaction::getType, "VIEW")
                .orderByDesc(Interaction::getCreateTime)
                .last("LIMIT 20");
        List<Interaction> views = interactionService.list(viewWrapper);
        views.forEach(v -> excludeIds.add(v.getBookId()));

        Set<Long> categoryIds = new java.util.HashSet<>();
        if (!excludeIds.isEmpty()) {
            List<Book> interactedBooks = listByIds(new ArrayList<>(excludeIds));
            for (Book b : interactedBooks) {
                if (b.getCategoryId() != null) {
                    categoryIds.add(b.getCategoryId());
                }
            }
        }

        int remaining = limit - collabRecs.size();
        if (categoryIds.isEmpty()) {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getStatus, "ON_SALE");
            if (!excludeIds.isEmpty()) wrapper.notIn(Book::getId, excludeIds);
            wrapper.orderByDesc(Book::getViewCount);
            wrapper.last("LIMIT " + remaining);
            List<BookVO> extra = list(wrapper).stream().map(this::toBookVO).collect(Collectors.toList());
            collabRecs.addAll(extra);
        } else {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getStatus, "ON_SALE");
            wrapper.in(Book::getCategoryId, categoryIds);
            if (!excludeIds.isEmpty()) wrapper.notIn(Book::getId, excludeIds);
            wrapper.orderByDesc(Book::getViewCount);
            wrapper.last("LIMIT " + remaining);
            List<BookVO> extra = list(wrapper).stream().map(this::toBookVO).collect(Collectors.toList());
            collabRecs.addAll(extra);
        }
        return collabRecs;
    }

    /**
     * 协同过滤：找到与当前用户购买过相同书籍的用户，推荐他们买过的其他书籍
     */
    private List<BookVO> getCollaborativeRecommendations(Long userId, int limit) {
        // 找到当前用户已购买完成的书籍
        LambdaQueryWrapper<Order> myOrders = new LambdaQueryWrapper<>();
        myOrders.eq(Order::getBuyerId, userId)
                .eq(Order::getStatus, "COMPLETED")
                .select(Order::getBookId);
        List<Long> myBookIds = orderService.list(myOrders).stream()
                .map(Order::getBookId).collect(Collectors.toList());
        if (myBookIds.isEmpty()) return new ArrayList<>();

        // 找到也买了这些书的其他用户
        LambdaQueryWrapper<Order> peerOrders = new LambdaQueryWrapper<>();
        peerOrders.in(Order::getBookId, myBookIds)
                .eq(Order::getStatus, "COMPLETED")
                .ne(Order::getBuyerId, userId)
                .select(Order::getBuyerId, Order::getBookId);
        List<Order> peerOrderList = orderService.list(peerOrders);
        if (peerOrderList.isEmpty()) return new ArrayList<>();

        // 统计这些用户买的其他书籍（排除已买过的），按频次排序
        Set<Long> myBookIdSet = new java.util.HashSet<>(myBookIds);
        Map<Long, Long> bookFreq = new java.util.HashMap<>();
        for (Order o : peerOrderList) {
            if (!myBookIdSet.contains(o.getBookId())) {
                bookFreq.merge(o.getBookId(), 1L, Long::sum);
            }
        }
        if (bookFreq.isEmpty()) return new ArrayList<>();

        // 取频次最高的书籍
        List<Long> topBookIds = bookFreq.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Book> books = listByIds(topBookIds);
        // 保持推荐顺序
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        return topBookIds.stream()
                .map(bookMap::get)
                .filter(b -> b != null && "ON_SALE".equals(b.getStatus()))
                .map(this::toBookVO)
                .collect(Collectors.toList());
    }

    private BookVO toBookVO(Book book) {
        BookVO vo = new BookVO();
        vo.setId(book.getId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setPublisher(book.getPublisher());
        vo.setIsbn(book.getIsbn());
        vo.setOriginalPrice(book.getOriginalPrice());
        vo.setSellingPrice(book.getSellingPrice());
        vo.setBookCondition(book.getBookCondition());
        vo.setDescription(book.getDescription());
        vo.setCoverImage(book.getCoverImage());
        vo.setImagesJson(book.getImagesJson());
        vo.parseImages();
        vo.setCategoryId(book.getCategoryId());
        vo.setSellerId(book.getSellerId());
        vo.setQuantity(book.getQuantity());
        vo.setStatus(book.getStatus());
        vo.setRejectReason(book.getRejectReason());
        vo.setViewCount(book.getViewCount());
        vo.setWantCount(book.getWantCount());
        vo.setCreateTime(book.getCreateTime());

        User seller = userService.getById(book.getSellerId());
        if (seller != null) {
            vo.setSellerName(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
            vo.setSellerReputation(seller.getReputationScore());
        }

        Category category = categoryService.getById(book.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        return vo;
    }

    private boolean hasActiveOrders(Long bookId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBookId, bookId)
                .in(Order::getStatus, "PENDING", "PAID", "SHIPPED", "REFUNDING", "DISPUTE");
        return orderService.count(wrapper) > 0;
    }

    private boolean isFullReputation(User seller) {
        if (seller == null || seller.getReputationScore() == null) {
            return false;
        }
        return seller.getReputationScore().compareTo(new java.math.BigDecimal("4.5")) >= 0;
    }
}

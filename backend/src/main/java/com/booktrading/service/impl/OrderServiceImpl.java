package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.dto.OrderCreateDTO;
import com.booktrading.entity.Book;
import com.booktrading.entity.Comment;
import com.booktrading.entity.Order;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.OrderMapper;
import com.booktrading.service.BookService;
import com.booktrading.service.CommentService;
import com.booktrading.service.MessageService;
import com.booktrading.service.OrderService;
import com.booktrading.service.PaymentService;
import com.booktrading.service.UserService;
import com.booktrading.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Lazy
    private PaymentService paymentService;

    @Autowired
    @Lazy
    private CommentService commentService;

    @Override
    @Transactional
    public Order createOrder(OrderCreateDTO dto, Long buyerId) {
        Book book = bookService.getById(dto.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (!"ON_SALE".equals(book.getStatus())) {
            throw new BusinessException("图书已下架或已被预订");
        }
        if (book.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的图书");
        }

        // 使用数据库级别乐观锁防止并发下单
        if (book.getQuantity() != null && book.getQuantity() <= 0) {
            throw new BusinessException("图书库存不足");
        }
        boolean updated = bookService.update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, book.getId())
                .eq(Book::getStatus, "ON_SALE")
                .gt(Book::getQuantity, 0)
                .setSql("quantity = quantity - 1"));
        if (!updated) {
            throw new BusinessException("图书库存不足或已被下架");
        }
        // 库存归零时自动下架
        Book updatedBook = bookService.getById(book.getId());
        if (updatedBook != null && updatedBook.getQuantity() != null && updatedBook.getQuantity() <= 0) {
            updatedBook.setStatus("OFF_SHELF");
            bookService.updateById(updatedBook);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBookId(dto.getBookId());
        order.setBuyerId(buyerId);
        order.setSellerId(book.getSellerId());
        order.setPrice(book.getSellingPrice());
        order.setStatus("PENDING");
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        order.setReceiverName(dto.getReceiverName());
        save(order);

        messageService.sendTradeMessage(book.getSellerId(), buyerId,
                "您的书籍《" + book.getTitle() + "》有新订单，订单号：" + order.getOrderNo());

        return order;
    }

    @Override
    @Transactional
    public void payOrder(Long orderId, Long userId) {
        Order order = getOrderEntity(orderId);
        if (userId != null && !order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus("PAID");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getBuyerId(), null,
                "订单 " + order.getOrderNo() + " 支付成功，等待卖家发货");
        messageService.sendTradeMessage(order.getSellerId(), order.getBuyerId(),
                "订单 " + order.getOrderNo() + " 买家已付款，请尽快发货");
    }

    @Override
    @Transactional
    public void shipOrder(Long orderId, Long sellerId, String shippingInfo, String logisticsCompany, String trackingNumber) {
        Order order = getOrderEntity(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus("SHIPPED");
        order.setShippingInfo(shippingInfo);
        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        String logisticsDetail = (logisticsCompany != null ? logisticsCompany : "") + (trackingNumber != null ? " " + trackingNumber : "");
        messageService.sendTradeMessage(order.getBuyerId(), order.getSellerId(),
                "订单 " + order.getOrderNo() + " 卖家已发货" + (logisticsDetail.trim().isEmpty() ? "" : "，物流信息：" + logisticsDetail.trim()));
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId, Long userId) {
        Order order = getOrderEntity(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus("COMPLETED");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getSellerId(), order.getBuyerId(),
                "订单 " + order.getOrderNo() + " 买家已确认收货，交易完成");

        Book book = bookService.getById(order.getBookId());
        if (book != null) {
            if (book.getQuantity() == null || book.getQuantity() <= 0) {
                book.setStatus("SOLD");
                bookService.updateById(book);
            }
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = getOrderEntity(orderId);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法取消");
        }
        // 已支付的订单取消需要退款
        if ("PAID".equals(order.getStatus())) {
            paymentService.refund(orderId);
        }
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        Long notifyId = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        messageService.sendTradeMessage(notifyId, userId, "订单 " + order.getOrderNo() + " 已被取消");

        // 原子恢复库存，避免并发竞态
        bookService.update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, order.getBookId())
                .setSql("quantity = quantity + 1"));
        Book book = bookService.getById(order.getBookId());
        if (book != null && !"OFF_SHELF".equals(book.getStatus()) && !"SOLD".equals(book.getStatus())) {
            book.setStatus("ON_SALE");
            bookService.updateById(book);
        }
    }

    @Override
    @Transactional
    public void requestRefund(Long orderId, Long userId, String reason) {
        Order order = getOrderEntity(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        // 已完成的订单，7天内可申请退款
        if ("COMPLETED".equals(order.getStatus()) && order.getUpdateTime() != null) {
            if (order.getUpdateTime().plusDays(7).isBefore(LocalDateTime.now())) {
                throw new BusinessException("订单已完成超过7天，无法申请退款");
            }
        }
        order.setStatus("REFUNDING");
        order.setRefundReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getSellerId(), userId,
                "订单 " + order.getOrderNo() + " 买家申请退款，原因：" + reason);
    }

    @Override
    @Transactional
    public void confirmRefund(Long orderId, Long sellerId) {
        Order order = getOrderEntity(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"REFUNDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        // 已发货的订单，卖家同意退款后需要买家退回书籍
        if (order.getLogisticsCompany() != null || order.getTrackingNumber() != null) {
            order.setStatus("RETURNING");
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);
            messageService.sendTradeMessage(order.getBuyerId(), sellerId,
                    "订单 " + order.getOrderNo() + " 卖家同意退款，请退回书籍并填写物流信息");
            messageService.sendTradeMessage(order.getSellerId(), sellerId,
                    "订单 " + order.getOrderNo() + " 您已同意退款，等待买家退回书籍");
            return;
        }
        // 未发货的订单直接退款
        paymentService.refund(orderId);
        order.setStatus("REFUNDED");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getBuyerId(), sellerId,
                "订单 " + order.getOrderNo() + " 卖家已同意退款，退款已原路返回");
        messageService.sendTradeMessage(order.getSellerId(), sellerId,
                "订单 " + order.getOrderNo() + " 退款已完成");

        bookService.update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, order.getBookId())
                .setSql("quantity = quantity + 1"));
        Book book = bookService.getById(order.getBookId());
        if (book != null && !"OFF_SHELF".equals(book.getStatus()) && !"SOLD".equals(book.getStatus())) {
            book.setStatus("ON_SALE");
            bookService.updateById(book);
        }
    }

    @Override
    @Transactional
    public void rejectRefund(Long orderId, Long sellerId) {
        Order order = getOrderEntity(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"REFUNDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus("PAID");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getBuyerId(), sellerId,
                "订单 " + order.getOrderNo() + " 卖家拒绝了退款申请，如有异议可发起纠纷");
    }

    @Override
    @Transactional
    public void submitReturnShipping(Long orderId, Long userId, String logisticsCompany, String trackingNumber) {
        Order order = getOrderEntity(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"RETURNING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setReturnLogisticsCompany(logisticsCompany);
        order.setReturnTrackingNumber(trackingNumber);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getSellerId(), userId,
                "订单 " + order.getOrderNo() + " 买家已退回书籍，物流信息：" + logisticsCompany + " " + trackingNumber);
    }

    @Override
    @Transactional
    public void confirmReturnReceive(Long orderId, Long sellerId) {
        Order order = getOrderEntity(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"RETURNING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        paymentService.refund(orderId);
        order.setStatus("REFUNDED");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        messageService.sendTradeMessage(order.getBuyerId(), sellerId,
                "订单 " + order.getOrderNo() + " 卖家已确认收到退回书籍，退款已原路返回");
        messageService.sendTradeMessage(order.getSellerId(), sellerId,
                "订单 " + order.getOrderNo() + " 退款已完成");

        bookService.update(new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, order.getBookId())
                .setSql("quantity = quantity + 1"));
        Book book = bookService.getById(order.getBookId());
        if (book != null && !"OFF_SHELF".equals(book.getStatus()) && !"SOLD".equals(book.getStatus())) {
            book.setStatus("ON_SALE");
            bookService.updateById(book);
        }
    }

    @Override
    @Transactional
    public void disputeOrder(Long orderId, Long userId, String reason, String disputeImages) {
        Order order = getOrderEntity(orderId);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())
                && !"REFUNDING".equals(order.getStatus()) && !"RETURNING".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态无法发起争议");
        }
        order.setStatus("DISPUTE");
        order.setDisputeReason(reason);
        order.setDisputeImages(disputeImages);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        Long otherId = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        messageService.sendTradeMessage(otherId, userId, "订单 " + order.getOrderNo() + " 有争议，请等待管理员处理");
        // 通知所有管理员
        List<User> admins = userService.list(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        for (User admin : admins) {
            messageService.sendSystemMessage(admin.getId(), "订单 " + order.getOrderNo() + " 有新争议待处理");
        }
    }

    @Override
    @Transactional
    public void resolveDispute(Long orderId, String adminNote, String action, java.math.BigDecimal partialRefundAmount) {
        Order order = getOrderEntity(orderId);
        if (!"DISPUTE".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        order.setAdminNote(adminNote);
        order.setUpdateTime(LocalDateTime.now());

        if ("refund".equals(action)) {
            paymentService.refund(orderId);
            order.setStatus("REFUNDED");
            updateById(order);
            bookService.update(new LambdaUpdateWrapper<Book>()
                    .eq(Book::getId, order.getBookId())
                    .setSql("quantity = quantity + 1"));
            Book book = bookService.getById(order.getBookId());
            if (book != null && !"OFF_SHELF".equals(book.getStatus())) {
                book.setStatus("ON_SALE");
                bookService.updateById(book);
            }
            messageService.sendSystemMessage(order.getBuyerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定退款，退款已原路返回");
            messageService.sendSystemMessage(order.getSellerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定退款给买家");
        } else if ("partial_refund".equals(action) && partialRefundAmount != null && partialRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentService.partialRefund(orderId, partialRefundAmount);
            order.setStatus("COMPLETED");
            updateById(order);
            messageService.sendSystemMessage(order.getBuyerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定部分退款 ¥" + partialRefundAmount);
            messageService.sendSystemMessage(order.getSellerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定部分退款 ¥" + partialRefundAmount + " 给买家");
        } else {
            order.setStatus("COMPLETED");
            updateById(order);
            Book book = bookService.getById(order.getBookId());
            if (book != null && (book.getQuantity() == null || book.getQuantity() <= 0)) {
                book.setStatus("SOLD");
                bookService.updateById(book);
            }
            messageService.sendSystemMessage(order.getBuyerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定放款给卖家");
            messageService.sendSystemMessage(order.getSellerId(),
                    "订单 " + order.getOrderNo() + " 纠纷已处理，管理员裁定放款给您");
        }
    }

    @Override
    public IPage<OrderVO> getMyBuyOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = page(new Page<>(page, size), wrapper);
        return batchConvertToVO(orderPage);
    }

    @Override
    public IPage<OrderVO> getMySellOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = page(new Page<>(page, size), wrapper);
        return batchConvertToVO(orderPage);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Order order = getOrderEntity(orderId);
        return toOrderVO(order);
    }

    @Override
    public List<Order> getCompletedOrdersByBook(Long bookId, Long buyerId) {
        // 查出该用户对该书已评价的订单ID
        List<Long> commentedOrderIds = commentService.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBookId, bookId)
                .eq(Comment::getUserId, buyerId)
                .isNull(Comment::getParentId)
                .select(Comment::getOrderId))
                .stream().map(Comment::getOrderId).collect(Collectors.toList());

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBookId, bookId)
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getStatus, "COMPLETED");
        if (!commentedOrderIds.isEmpty()) {
            wrapper.notIn(Order::getId, commentedOrderIds);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return list(wrapper);
    }

    private IPage<OrderVO> batchConvertToVO(IPage<Order> orderPage) {
        Set<Long> bookIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        orderPage.getRecords().forEach(o -> {
            if (o.getBookId() != null) bookIds.add(o.getBookId());
            if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
            if (o.getSellerId() != null) userIds.add(o.getSellerId());
        });
        Map<Long, Book> bookMap = bookIds.isEmpty() ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return orderPage.convert(order -> toOrderVO(order, bookMap, userMap));
    }

    private Order getOrderEntity(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBookId(order.getBookId());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setAddress(order.getAddress());
        vo.setPhone(order.getPhone());
        vo.setReceiverName(order.getReceiverName());
        vo.setShippingInfo(order.getShippingInfo());
        vo.setLogisticsCompany(order.getLogisticsCompany());
        vo.setTrackingNumber(order.getTrackingNumber());
        vo.setReturnLogisticsCompany(order.getReturnLogisticsCompany());
        vo.setReturnTrackingNumber(order.getReturnTrackingNumber());
        vo.setRefundReason(order.getRefundReason());
        vo.setDisputeReason(order.getDisputeReason());
        vo.setDisputeImages(order.getDisputeImages());
        vo.setAdminNote(order.getAdminNote());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        Book book = bookService.getById(order.getBookId());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setCoverImage(book.getCoverImage());
        }

        User buyer = userService.getById(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerName(buyer.getNickname());
        }

        User seller = userService.getById(order.getSellerId());
        if (seller != null) {
            vo.setSellerName(seller.getNickname());
        }

        return vo;
    }

    private OrderVO toOrderVO(Order order, Map<Long, Book> bookMap, Map<Long, User> userMap) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBookId(order.getBookId());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setAddress(order.getAddress());
        vo.setPhone(order.getPhone());
        vo.setReceiverName(order.getReceiverName());
        vo.setShippingInfo(order.getShippingInfo());
        vo.setLogisticsCompany(order.getLogisticsCompany());
        vo.setTrackingNumber(order.getTrackingNumber());
        vo.setReturnLogisticsCompany(order.getReturnLogisticsCompany());
        vo.setReturnTrackingNumber(order.getReturnTrackingNumber());
        vo.setRefundReason(order.getRefundReason());
        vo.setDisputeReason(order.getDisputeReason());
        vo.setDisputeImages(order.getDisputeImages());
        vo.setAdminNote(order.getAdminNote());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        Book book = order.getBookId() != null ? bookMap.get(order.getBookId()) : null;
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setCoverImage(book.getCoverImage());
        }

        User buyer = order.getBuyerId() != null ? userMap.get(order.getBuyerId()) : null;
        if (buyer != null) {
            vo.setBuyerName(buyer.getNickname());
        }

        User seller = order.getSellerId() != null ? userMap.get(order.getSellerId()) : null;
        if (seller != null) {
            vo.setSellerName(seller.getNickname());
        }

        return vo;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return timestamp + uuid;
    }
}

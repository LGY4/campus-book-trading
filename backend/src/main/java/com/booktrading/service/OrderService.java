package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.dto.OrderCreateDTO;
import com.booktrading.entity.Order;
import com.booktrading.vo.OrderVO;

public interface OrderService extends IService<Order> {

    Order createOrder(OrderCreateDTO dto, Long buyerId);

    void payOrder(Long orderId, Long userId);

    void shipOrder(Long orderId, Long sellerId, String shippingInfo, String logisticsCompany, String trackingNumber);

    void confirmReceive(Long orderId, Long userId);

    void cancelOrder(Long orderId, Long userId);

    void requestRefund(Long orderId, Long userId, String reason);

    void confirmRefund(Long orderId, Long sellerId);

    void rejectRefund(Long orderId, Long sellerId);

    void submitReturnShipping(Long orderId, Long userId, String logisticsCompany, String trackingNumber);

    void confirmReturnReceive(Long orderId, Long sellerId);

    void disputeOrder(Long orderId, Long userId, String reason, String disputeImages);

    void resolveDispute(Long orderId, String adminNote, String action, java.math.BigDecimal partialRefundAmount);

    IPage<OrderVO> getMyBuyOrders(Long userId, int page, int size, String status);

    IPage<OrderVO> getMySellOrders(Long userId, int page, int size, String status);

    OrderVO getOrderDetail(Long orderId);

    java.util.List<Order> getCompletedOrdersByBook(Long bookId, Long buyerId);
}
